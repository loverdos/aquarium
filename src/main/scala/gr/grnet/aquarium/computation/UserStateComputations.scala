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
import gr.grnet.aquarium.logic.accounting.Accounting
import gr.grnet.aquarium.computation.data._
import gr.grnet.aquarium.computation.reason.{NoSpecificChangeReason, UserStateChangeReason}
import gr.grnet.aquarium.event.model.NewWalletEntry
import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import gr.grnet.aquarium.{Aquarium, AquariumInternalError, AquariumException}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class UserStateComputations(_aquarium: () ⇒ Aquarium) extends Loggable {

  protected lazy val aquarium           = _aquarium()
  protected lazy val storeProvider      = aquarium.storeProvider
  protected lazy val accounting         = new Accounting {}
  protected lazy val algorithmCompiler  = aquarium.algorithmCompiler
  protected lazy val policyStore        = storeProvider.policyStore
  protected lazy val userStateStore     = storeProvider.userStateStore
  protected lazy val resourceEventStore = storeProvider.resourceEventStore

  def findUserStateAtEndOfBillingMonth(userId: String,
                                       billingMonthInfo: BillingMonthInfo,
                                       currentUserState: UserState,
                                       defaultResourcesMap: DSLResourcesMap,
                                       calculationReason: UserStateChangeReason,
                                       clogOpt: Option[ContextualLogger] = None): UserState = {

    val clog = ContextualLogger.fromOther(
      clogOpt,
      logger,
      "findUserStateAtEndOfBillingMonth(%s)", billingMonthInfo)
    clog.begin()

    def doCompute: UserState = {
      doFullMonthlyBilling(
        userId,
        billingMonthInfo,
        currentUserState,
        defaultResourcesMap,
        calculationReason,
        Some(clog))
    }

    val userCreationMillis = currentUserState.userCreationMillis
    val userCreationDateCalc = new MutableDateCalc(userCreationMillis)
    val billingMonthStartMillis = billingMonthInfo.startMillis
    val billingMonthStopMillis = billingMonthInfo.stopMillis

    if(billingMonthStopMillis < userCreationMillis) {
      // If the user did not exist for this billing month, piece of cake
      clog.debug("User did not exist before %s", userCreationDateCalc)

      // NOTE: Reason here will be: InitialUserStateSetup$
      val initialUserState0 = UserState.createInitialUserStateFrom(currentUserState)
      val initialUserState1 = userStateStore.insertUserState(initialUserState0)

      clog.debug("Returning INITIAL state [_id=%s] %s".format(initialUserState1._id, initialUserState1))
      clog.end()

      initialUserState1
    } else {
      // Ask DB cache for the latest known user state for this billing period
      val latestUserStateOpt = userStateStore.findLatestUserStateForEndOfBillingMonth(
        userId,
        billingMonthInfo.year,
        billingMonthInfo.month)

      latestUserStateOpt match {
        case None ⇒
          // Not found, must compute
          clog.debug("No user state found from cache, will have to (re)compute")
          val result = doCompute
          clog.end()
          result

        case Some(latestUserState) ⇒
          // Found a "latest" user state but need to see if it is indeed the true and one latest.
          // For this reason, we must count the events again.
          val latestStateOOSEventsCounter = latestUserState.billingPeriodOutOfSyncResourceEventsCounter
          val actualOOSEventsCounter = resourceEventStore.countOutOfSyncEventsForBillingPeriod(
            userId,
            billingMonthStartMillis,
            billingMonthStopMillis)

          val counterDiff = actualOOSEventsCounter - latestStateOOSEventsCounter
          counterDiff match {
            // ZERO, we are OK!
            case 0 ⇒
              // NOTE: Keep the caller's calculation reason
              latestUserState.copyForChangeReason(calculationReason)

            // We had more, so must recompute
            case n if n > 0 ⇒
              clog.debug(
                "Found %s out of sync events (%s more), will have to (re)compute user state", actualOOSEventsCounter, n)
              val result = doCompute
              clog.end()
              result

            // We had less????
            case n if n < 0 ⇒
              val errMsg = "Found %s out of sync events (%s less). DB must be inconsistent".format(actualOOSEventsCounter, n)
              clog.warn(errMsg)
              throw new AquariumException(errMsg)
          }
      }
    }
  }

  //+ Utility methods
  def rcDebugInfo(rcEvent: ResourceEventModel) = {
    rcEvent.toDebugString(false)
  }

  //- Utility methods

  def processResourceEvent(startingUserState: UserState,
                           userStateWorker: UserStateWorker,
                           currentResourceEvent: ResourceEventModel,
                           stateChangeReason: UserStateChangeReason,
                           billingMonthInfo: BillingMonthInfo,
                           walletEntriesBuffer: mutable.Buffer[NewWalletEntry],
                           clogOpt: Option[ContextualLogger] = None): UserState = {

    val clog = ContextualLogger.fromOther(clogOpt, logger, "walletEntriesForResourceEvent(%s)", currentResourceEvent.id)

    var _workingUserState = startingUserState

    val theResource = currentResourceEvent.safeResource
    val theInstanceId = currentResourceEvent.safeInstanceId
    val theValue = currentResourceEvent.value

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
        clog.debug("Cost policy %s for %s", costPolicy, dslResource)
        val isBillable = costPolicy.isBillableEventBasedOnValue(theValue)
        if(!isBillable) {
          // The resource event is not billable
          clog.debug("Ignoring not billable event %s", currentResourceEventDebugInfo)
        } else {
          // The resource event is billable
          // Find the previous event.
          // This is (potentially) needed to calculate new credit amount and new resource instance amount
          val previousResourceEventOpt = userStateWorker.findAndRemovePreviousResourceEvent(theResource, theInstanceId)
          clog.debug("PreviousM %s", previousResourceEventOpt.map(rcDebugInfo(_)))

          val havePreviousResourceEvent = previousResourceEventOpt.isDefined
          val needPreviousResourceEvent = costPolicy.needsPreviousEventForCreditAndAmountCalculation
          if(needPreviousResourceEvent && !havePreviousResourceEvent) {
            // This must be the first resource event of its kind, ever.
            // TODO: We should normally check the DB to verify the claim (?)
            clog.debug("Ignoring first event of its kind %s", currentResourceEventDebugInfo)
            userStateWorker.updateIgnored(currentResourceEvent)
          } else {
            val defaultInitialAmount = costPolicy.getResourceInstanceInitialAmount
            val oldAmount = _workingUserState.getResourceInstanceAmount(theResource, theInstanceId, defaultInitialAmount)
            val oldCredits = _workingUserState.totalCredits

            // A. Compute new resource instance accumulating amount
            val newAmount = costPolicy.computeNewAccumulatingAmount(oldAmount, theValue)

            clog.debug("theValue = %s, oldAmount = %s, newAmount = %s, oldCredits = %s", theValue, oldAmount, newAmount, oldCredits)

            // B. Compute new wallet entries
            clog.debug("agreementsSnapshot = %s", _workingUserState.agreementHistory)
            val alltimeAgreements = _workingUserState.agreementHistory.agreementNamesByTimeslot

            //              clog.debug("Computing full chargeslots")
            val (referenceTimeslot, fullChargeslots) = accounting.computeFullChargeslots(
              previousResourceEventOpt,
              currentResourceEvent,
              oldCredits,
              oldAmount,
              newAmount,
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
                  List(currentResourceEvent, previousResourceEventOpt.get)
                else
                  List(currentResourceEvent),
                fullChargeslots,
                dslResource,
                currentResourceEvent.isSynthetic
              )
              clog.debug("New %s", newWalletEntry)

              walletEntriesBuffer += newWalletEntry
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

  def processResourceEvents(resourceEvents: Traversable[ResourceEventModel],
                            startingUserState: UserState,
                            userStateWorker: UserStateWorker,
                            stateChangeReason: UserStateChangeReason,
                            billingMonthInfo: BillingMonthInfo,
                            walletEntriesBuffer: mutable.Buffer[NewWalletEntry],
                            clogOpt: Option[ContextualLogger] = None): UserState = {

    var _workingUserState = startingUserState

    for(currentResourceEvent ← resourceEvents) {

      _workingUserState = processResourceEvent(
        _workingUserState,
        userStateWorker,
        currentResourceEvent,
        stateChangeReason,
        billingMonthInfo,
        walletEntriesBuffer,
        clogOpt
      )
    }

    _workingUserState
  }


  def doFullMonthlyBilling(userId: String,
                           billingMonthInfo: BillingMonthInfo,
                           currentUserState: UserState,
                           defaultResourcesMap: DSLResourcesMap,
                           calculationReason: UserStateChangeReason = NoSpecificChangeReason,
                           clogOpt: Option[ContextualLogger] = None): UserState = {


    val clog = ContextualLogger.fromOther(
      clogOpt,
      logger,
      "doFullMonthlyBilling(%s)", billingMonthInfo)
    clog.begin()

    val clogSome = Some(clog)

    val previousBillingMonthUserState = findUserStateAtEndOfBillingMonth(
      userId,
      billingMonthInfo.previousMonth,
      currentUserState,
      defaultResourcesMap,
      calculationReason.forPreviousBillingMonth,
      clogSome
    )

    val startingUserState = previousBillingMonthUserState


    val billingMonthStartMillis = billingMonthInfo.startMillis
    val billingMonthEndMillis = billingMonthInfo.stopMillis

    // Keep the working (current) user state. This will get updated as we proceed with billing for the month
    // specified in the parameters.
    // NOTE: The calculation reason is not the one we get from the previous user state but the one our caller specifies
    var _workingUserState = startingUserState.copyForChangeReason(calculationReason)

    val userStateWorker = UserStateWorker.fromUserState(_workingUserState, defaultResourcesMap)

    userStateWorker.debugTheMaps(clog)(rcDebugInfo)

    // First, find and process the actual resource events from DB
    val allResourceEventsForMonth = resourceEventStore.findAllRelevantResourceEventsForBillingPeriod(
      userId,
      billingMonthStartMillis,
      billingMonthEndMillis)

    val newWalletEntries = scala.collection.mutable.ListBuffer[NewWalletEntry]()

    _workingUserState = processResourceEvents(
      allResourceEventsForMonth,
      _workingUserState,
      userStateWorker,
      calculationReason,
      billingMonthInfo,
      newWalletEntries,
      clogSome
    )

    // Second, for the remaining events which must contribute an implicit OFF, we collect those OFFs
    // ... in order to generate an implicit ON later
    val (specialEvents, theirImplicitEnds) = userStateWorker.
      findAndRemoveGeneratorsOfImplicitEndEvents(billingMonthEndMillis)
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
      newWalletEntries,
      clogSome
    )

    val lastUpdateTime = TimeHelpers.nowMillis()

    _workingUserState = _workingUserState.copy(
      implicitlyIssuedSnapshot = userStateWorker.implicitlyIssuedStartEvents.toImmutableSnapshot(lastUpdateTime),
      latestResourceEventsSnapshot = userStateWorker.previousResourceEvents.toImmutableSnapshot(lastUpdateTime),
      stateChangeCounter = _workingUserState.stateChangeCounter + 1,
      parentUserStateId = startingUserState.idOpt,
      newWalletEntries = newWalletEntries.toList
    )

    clog.debug("calculationReason = %s", calculationReason)

    if(calculationReason.shouldStoreUserState) {
      val storedUserState = userStateStore.insertUserState(_workingUserState)
      clog.debug("Saved [_id=%s] %s", storedUserState._id, storedUserState)
      _workingUserState = storedUserState
    }

    clog.debug("RETURN %s", _workingUserState)
    clog.end()
    _workingUserState
  }
}
