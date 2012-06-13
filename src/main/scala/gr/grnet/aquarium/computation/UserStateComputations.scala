/*
 * Copyright 2011-2012 GRNET S.A. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 *   1. Redistributions of source code must retain the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer.
 *
 *   2. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials
 *      provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY GRNET S.A. ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL GRNET S.A OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and
 * documentation are those of the authors and should not be
 * interpreted as representing official policies, either expressed
 * or implied, of GRNET S.A.
 */

package gr.grnet.aquarium.computation

import scala.collection.mutable
import gr.grnet.aquarium.util.{ContextualLogger, Loggable}
import gr.grnet.aquarium.util.date.{TimeHelpers, MutableDateCalc}
import gr.grnet.aquarium.logic.accounting.dsl.DSLResourcesMap
import gr.grnet.aquarium.computation.state.parts._
import gr.grnet.aquarium.event.model.NewWalletEntry
import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import gr.grnet.aquarium.{Aquarium, AquariumInternalError}
import gr.grnet.aquarium.computation.reason.{MonthlyBillingCalculation, InitialUserStateSetup, UserStateChangeReason}
import gr.grnet.aquarium.computation.state.{UserStateWorker, UserStateBootstrap, UserState}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
final class UserStateComputations(_aquarium: => Aquarium) extends Loggable {

  lazy val aquarium = _aquarium

  lazy val storeProvider         = aquarium.storeProvider
  lazy val timeslotComputations  = new TimeslotComputations {}
  lazy val algorithmCompiler     = aquarium.algorithmCompiler
  lazy val policyStore           = storeProvider.policyStore
  lazy val userStateStoreForRead = storeProvider.userStateStore
  lazy val resourceEventStore    = storeProvider.resourceEventStore

  def findUserStateAtEndOfBillingMonth(
      userStateBootstrap: UserStateBootstrap,
      billingMonthInfo: BillingMonthInfo,
      defaultResourcesMap: DSLResourcesMap,
      calculationReason: UserStateChangeReason,
      storeFunc: UserState ⇒ UserState,
      clogOpt: Option[ContextualLogger] = None
  ): UserState = {

    val clog = ContextualLogger.fromOther(
      clogOpt,
      logger,
      "findUserStateAtEndOfBillingMonth(%s)", billingMonthInfo.toShortDebugString)
    clog.begin()

    def computeFullMonthBillingAndSaveState(): UserState = {
      val userState0 = doFullMonthBilling(
        userStateBootstrap,
        billingMonthInfo,
        defaultResourcesMap,
        calculationReason,
        storeFunc,
        Some(clog)
      )

      // We always save the state when it is a full month billing
      val userState1 = storeFunc.apply(
        userState0.newWithChangeReason(
          MonthlyBillingCalculation(calculationReason, billingMonthInfo))
      )

      clog.debug("Stored full %s %s", billingMonthInfo.toDebugString, userState1.toJsonString)
      userState1
    }

    val userID = userStateBootstrap.userID
    val userCreationMillis = userStateBootstrap.userCreationMillis
    val userCreationDateCalc = new MutableDateCalc(userCreationMillis)
    val billingMonthStartMillis = billingMonthInfo.monthStartMillis
    val billingMonthStopMillis = billingMonthInfo.monthStopMillis

    if(billingMonthStopMillis < userCreationMillis) {
      // If the user did not exist for this billing month, piece of cake
      clog.debug("User did not exist before %s", userCreationDateCalc)

      // TODO: The initial user state might have already been created.
      //       First ask if it exists and compute only if not
      val initialUserState0 = UserState.createInitialUserStateFromBootstrap(
        userStateBootstrap,
        TimeHelpers.nowMillis(),
        InitialUserStateSetup(Some(calculationReason)) // we record the originating calculation reason
      )

      // We always save the initial state
      val initialUserState1 = storeFunc.apply(initialUserState0)

      clog.debug("Stored initial state = %s", initialUserState1.toJsonString)
      clog.end()

      return initialUserState1
    }

    // Ask DB cache for the latest known user state for this billing period
    val latestUserStateOpt = userStateStoreForRead.findLatestUserStateForFullMonthBilling(
      userID,
      billingMonthInfo)

    latestUserStateOpt match {
      case None ⇒
        // Not found, must compute
        clog.debug("No user state found from cache, will have to (re)compute")
        val result = computeFullMonthBillingAndSaveState
        clog.end()
        result

      case Some(latestUserState) ⇒
        // Found a "latest" user state but need to see if it is indeed the true and one latest.
        // For this reason, we must count the events again.
        val latestStateOOSEventsCounter = latestUserState.billingPeriodOutOfSyncResourceEventsCounter
        val actualOOSEventsCounter = resourceEventStore.countOutOfSyncResourceEventsForBillingPeriod(
          userID,
          billingMonthStartMillis,
          billingMonthStopMillis)

        val counterDiff = actualOOSEventsCounter - latestStateOOSEventsCounter
        counterDiff match {
          // ZERO, we are OK!
          case 0 ⇒
            // NOTE: Keep the caller's calculation reason
            val result = latestUserState.newWithChangeReason(calculationReason)
            clog.end()
            result

          // We had more, so must recompute
          case n if n > 0 ⇒
            clog.debug(
              "Found %s out of sync events (%s more), will have to (re)compute user state", actualOOSEventsCounter, n)
            val result = computeFullMonthBillingAndSaveState
            clog.end()
            result

          // We had less????
          case n if n < 0 ⇒
            val errMsg = "Found %s out of sync events (%s less). DB must be inconsistent".format(actualOOSEventsCounter, n)
            clog.warn(errMsg)
            throw new AquariumInternalError(errMsg)
        }
    }
  }

  //+ Utility methods
  protected def rcDebugInfo(rcEvent: ResourceEventModel) = {
    rcEvent.toDebugString
  }
  //- Utility methods

  def processResourceEvent(
      startingUserState: UserState,
      userStateWorker: UserStateWorker,
      currentResourceEvent: ResourceEventModel,
      stateChangeReason: UserStateChangeReason,
      billingMonthInfo: BillingMonthInfo,
      walletEntryRecorder: NewWalletEntry ⇒ Unit,
      clogOpt: Option[ContextualLogger] = None
  ): UserState = {

    val clog = ContextualLogger.fromOther(clogOpt, logger, "processResourceEvent(%s)", currentResourceEvent.id)

    var _workingUserState = startingUserState

    val theResource = currentResourceEvent.safeResource
    val theInstanceId = currentResourceEvent.safeInstanceId
    val theValue = currentResourceEvent.value
    val theDetails = currentResourceEvent.details

    val resourcesMap = userStateWorker.resourcesMap

    val currentResourceEventDebugInfo = rcDebugInfo(currentResourceEvent)
    clog.begin(currentResourceEventDebugInfo)

    userStateWorker.debugTheMaps(clog)(rcDebugInfo)

    // Ignore the event if it is not billable (but still record it in the "previous" stuff).
    // But to make this decision, first we need the resource definition (and its cost policy).
    val dslResourceOpt = resourcesMap.findResource(theResource)
    dslResourceOpt match {
      // We have a resource (and thus a cost policy)
      case Some(dslResource) ⇒
        val costPolicy = dslResource.costPolicy
        clog.debug("%s for %s", costPolicy, dslResource)
        val isBillable = costPolicy.isBillableEventBasedOnValue(theValue)
        if(!isBillable) {
          // The resource event is not billable
          clog.debug("Ignoring not billable %s", currentResourceEventDebugInfo)
        } else {
          // The resource event is billable
          // Find the previous event.
          // This is (potentially) needed to calculate new credit amount and new resource instance amount
          val previousResourceEventOpt0 = userStateWorker.findAndRemovePreviousResourceEvent(theResource, theInstanceId)
          clog.debug("PreviousM %s", previousResourceEventOpt0.map(rcDebugInfo(_)))

          val havePreviousResourceEvent = previousResourceEventOpt0.isDefined
          val needPreviousResourceEvent = costPolicy.needsPreviousEventForCreditAndAmountCalculation

          val (proceed, previousResourceEventOpt1) = if(needPreviousResourceEvent && !havePreviousResourceEvent) {
            // This must be the first resource event of its kind, ever.
            // TODO: We should normally check the DB to verify the claim (?)

            val actualFirstEvent = currentResourceEvent

            if(costPolicy.isBillableFirstEventBasedOnValue(actualFirstEvent.value) &&
              costPolicy.mustGenerateDummyFirstEvent) {

              clog.debug("First event of its kind %s", currentResourceEventDebugInfo)

              // OK. Let's see what the cost policy decides. If it must generate a dummy first event, we use that.
              // Otherwise, the current event goes to the ignored list.
              // The dummy first is considered to exist at the beginning of the billing period

              val dummyFirst = costPolicy.constructDummyFirstEventFor(currentResourceEvent, billingMonthInfo.monthStartMillis)

              clog.debug("Dummy first companion %s", rcDebugInfo(dummyFirst))

              // proceed with charging???
              (true, Some(dummyFirst))
            } else {
              clog.debug("Ignoring first event of its kind %s", currentResourceEventDebugInfo)
              userStateWorker.updateIgnored(currentResourceEvent)
              (false, None)
            }
          } else {
            (true, previousResourceEventOpt0)
          }

          if(proceed) {
            val defaultInitialAmount = costPolicy.getResourceInstanceInitialAmount
            val oldAmount = _workingUserState.getResourceInstanceAmount(theResource, theInstanceId, defaultInitialAmount)
            val oldCredits = _workingUserState.totalCredits

            // A. Compute new resource instance accumulating amount
            val newAccumulatingAmount = costPolicy.computeNewAccumulatingAmount(oldAmount, theValue, theDetails)

            clog.debug("theValue = %s, oldAmount = %s, newAmount = %s, oldCredits = %s", theValue, oldAmount, newAccumulatingAmount, oldCredits)

            // B. Compute new wallet entries
            clog.debug("agreementsSnapshot = %s", _workingUserState.agreementHistory)
            val alltimeAgreements = _workingUserState.agreementHistory.agreementNamesByTimeslot

            //              clog.debug("Computing full chargeslots")
            val (referenceTimeslot, fullChargeslots) = timeslotComputations.computeFullChargeslots(
              previousResourceEventOpt1,
              currentResourceEvent,
              oldCredits,
              oldAmount,
              newAccumulatingAmount,
              dslResource,
              resourcesMap,
              alltimeAgreements,
              algorithmCompiler,
              policyStore,
              Some(clog)
            )

            // We have the chargeslots, let's associate them with the current event
            if(fullChargeslots.length == 0) {
              // At least one chargeslot is required.
              throw new AquariumInternalError("No chargeslots computed for resource event %s".format(currentResourceEvent.id))
            }
            clog.debugSeq("fullChargeslots", fullChargeslots, 0)

            // C. Compute new credit amount (based on the charge slots)
            val newCreditsDiff = fullChargeslots.map(_.computedCredits.get).sum
            val newCredits = oldCredits - newCreditsDiff

            if(stateChangeReason.shouldStoreCalculatedWalletEntries) {
              val newWalletEntry = NewWalletEntry(
                userStateWorker.userID,
                newCreditsDiff,
                oldCredits,
                newCredits,
                TimeHelpers.nowMillis(),
                referenceTimeslot,
                billingMonthInfo.year,
                billingMonthInfo.month,
                if(havePreviousResourceEvent)
                  List(currentResourceEvent, previousResourceEventOpt1.get)
                else
                  List(currentResourceEvent),
                fullChargeslots,
                dslResource,
                currentResourceEvent.isSynthetic
              )
              clog.debug("New %s", newWalletEntry)

              walletEntryRecorder.apply(newWalletEntry)
            } else {
              clog.debug("newCreditsDiff = %s, newCredits = %s", newCreditsDiff, newCredits)
            }

            _workingUserState = _workingUserState.copy(
              totalCredits = newCredits,
              stateChangeCounter = _workingUserState.stateChangeCounter + 1
            )
          }
        }

        // After processing, all events billable or not update the previous state
        userStateWorker.updatePrevious(currentResourceEvent)

        _workingUserState = _workingUserState.copy(
          latestResourceEventsSnapshot = userStateWorker.previousResourceEvents.toImmutableSnapshot(TimeHelpers.nowMillis())
        )

      // We do not have a resource (and thus, no cost policy)
      case None ⇒
        // Now, this is a matter of politics: what do we do if no policy was found?
        clog.warn("Unknown resource for %s", currentResourceEventDebugInfo)
    } // dslResourceOpt match

    clog.end(currentResourceEventDebugInfo)

    _workingUserState
  }

  def processResourceEvents(
      resourceEvents: Traversable[ResourceEventModel],
      startingUserState: UserState,
      userStateWorker: UserStateWorker,
      stateChangeReason: UserStateChangeReason,
      billingMonthInfo: BillingMonthInfo,
      walletEntryRecorder: NewWalletEntry ⇒ Unit,
      clogOpt: Option[ContextualLogger] = None
  ): UserState = {

    var _workingUserState = startingUserState

    for(currentResourceEvent ← resourceEvents) {

      _workingUserState = processResourceEvent(
        _workingUserState,
        userStateWorker,
        currentResourceEvent,
        stateChangeReason,
        billingMonthInfo,
        walletEntryRecorder,
        clogOpt
      )
    }

    _workingUserState
  }

  def doFullMonthBilling(
      userStateBootstrap: UserStateBootstrap,
      billingMonthInfo: BillingMonthInfo,
      defaultResourcesMap: DSLResourcesMap,
      calculationReason: UserStateChangeReason,
      storeFunc: UserState ⇒ UserState,
      clogOpt: Option[ContextualLogger] = None
  ): UserState = {

    doMonthBillingUpTo(
      billingMonthInfo,
      billingMonthInfo.monthStopMillis,
      userStateBootstrap,
      defaultResourcesMap,
      calculationReason,
      storeFunc,
      clogOpt
    )
  }

  def doMonthBillingUpTo(
      /**
       * Which month to bill.
       */
      billingMonthInfo: BillingMonthInfo,
      /**
       * Bill from start of month up to (and including) this time.
       */
      billingEndTimeMillis: Long,
      userStateBootstrap: UserStateBootstrap,
      defaultResourcesMap: DSLResourcesMap,
      calculationReason: UserStateChangeReason,
      storeFunc: UserState ⇒ UserState,
      clogOpt: Option[ContextualLogger] = None
  ): UserState = {

    val isFullMonthBilling = billingEndTimeMillis == billingMonthInfo.monthStopMillis
    val userID = userStateBootstrap.userID

    val clog = ContextualLogger.fromOther(
      clogOpt,
      logger,
      "doMonthBillingUpTo(%s)", new MutableDateCalc(billingEndTimeMillis).toYYYYMMDDHHMMSSSSS)
    clog.begin()

    clog.debug("%s", calculationReason)

    val clogSome = Some(clog)

    val previousBillingMonthInfo = billingMonthInfo.previousMonth
    val previousBillingMonthUserState = findUserStateAtEndOfBillingMonth(
      userStateBootstrap,
      previousBillingMonthInfo,
      defaultResourcesMap,
      calculationReason,
      storeFunc,
      clogSome
    )

    clog.debug("previousBillingMonthUserState(%s) = %s".format(
      previousBillingMonthInfo.toShortDebugString,
      previousBillingMonthUserState.toJsonString))

    val startingUserState = previousBillingMonthUserState

    // Keep the working (current) user state. This will get updated as we proceed with billing for the month
    // specified in the parameters.
    // NOTE: The calculation reason is not the one we get from the previous user state but the one our caller specifies
    var _workingUserState = startingUserState.newWithChangeReason(calculationReason)

    val userStateWorker = UserStateWorker.fromUserState(_workingUserState, defaultResourcesMap)

    userStateWorker.debugTheMaps(clog)(rcDebugInfo)

    // First, find and process the actual resource events from DB
    val newWalletEntries = scala.collection.mutable.ListBuffer[NewWalletEntry]()
    val walletEntryRecorder = (nwe: NewWalletEntry) ⇒ {
      newWalletEntries.append(nwe)
    }

    var _rcEventsCounter = 0
    resourceEventStore.foreachResourceEventOccurredInPeriod(
      userID,
      billingMonthInfo.monthStartMillis, // from start of month
      billingEndTimeMillis               // to requested time
    ) { currentResourceEvent ⇒

      clog.debug("Processing %s".format(currentResourceEvent))

      _workingUserState = processResourceEvent(
        _workingUserState,
        userStateWorker,
        currentResourceEvent,
        calculationReason,
        billingMonthInfo,
        walletEntryRecorder,
        clogSome
      )

      _rcEventsCounter += 1
    }

    clog.debug("Found %s resource events for month %s".format(_rcEventsCounter, billingMonthInfo.toShortDebugString))

    if(isFullMonthBilling) {
      // Second, for the remaining events which must contribute an implicit OFF, we collect those OFFs
      // ... in order to generate an implicit ON later (during the next billing cycle).
      val (specialEvents, theirImplicitEnds) = userStateWorker.
        findAndRemoveGeneratorsOfImplicitEndEvents(billingMonthInfo.monthStopMillis)

      if(specialEvents.lengthCompare(1) >= 0 || theirImplicitEnds.lengthCompare(1) >= 0) {
        clog.debug("")
        clog.debug("Process implicitly issued events")
        clog.debugSeq("specialEvents", specialEvents, 0)
        clog.debugSeq("theirImplicitEnds", theirImplicitEnds, 0)
      }

      // Now, the previous and implicitly started must be our base for the following computation, so we create an
      // appropriate worker
      val specialUserStateWorker = UserStateWorker(
        userStateWorker.userID,
        LatestResourceEventsWorker.fromList(specialEvents),
        ImplicitlyIssuedResourceEventsWorker.Empty,
        IgnoredFirstResourceEventsWorker.Empty,
        userStateWorker.resourcesMap
      )

      _workingUserState = processResourceEvents(
        theirImplicitEnds,
        _workingUserState,
        specialUserStateWorker,
        calculationReason,
        billingMonthInfo,
        walletEntryRecorder,
        clogSome
      )
    }

    val lastUpdateTime = TimeHelpers.nowMillis()

    _workingUserState = _workingUserState.copy(
      isFullBillingMonthState = isFullMonthBilling,

      theFullBillingMonth = if(isFullMonthBilling)
        Some(billingMonthInfo)
      else
        _workingUserState.theFullBillingMonth,

      implicitlyIssuedSnapshot = userStateWorker.implicitlyIssuedStartEvents.toImmutableSnapshot(lastUpdateTime),

      latestResourceEventsSnapshot = userStateWorker.previousResourceEvents.toImmutableSnapshot(lastUpdateTime),

      stateChangeCounter = _workingUserState.stateChangeCounter + 1,

      parentUserStateIDInStore = startingUserState.idInStore,

      newWalletEntries = newWalletEntries.toList
    )

    clog.end()
    _workingUserState
  }
}
