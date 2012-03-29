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

package gr.grnet.aquarium.user


import scala.collection.mutable
import com.ckkloverdos.maybe.{Failed, NoVal, Just, Maybe}
import gr.grnet.aquarium.util.{ContextualLogger, Loggable, justForSure, failedForSure}
import gr.grnet.aquarium.logic.events.{NewWalletEntry, ResourceEvent}
import gr.grnet.aquarium.util.date.{TimeHelpers, MutableDateCalc}
import gr.grnet.aquarium.logic.accounting.dsl.{DSLAgreement, DSLResourcesMap}
import gr.grnet.aquarium.store.{StoreProvider, PolicyStore}
import gr.grnet.aquarium.logic.accounting.Accounting
import gr.grnet.aquarium.logic.accounting.algorithm.CostPolicyAlgorithmCompiler

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class UserStateComputations extends Loggable {
  def createInitialUserState(userId: String,
                             userCreationMillis: Long,
                             isActive: Boolean,
                             credits: Double,
                             roleNames: List[String] = List(),
                             agreementName: String = DSLAgreement.DefaultAgreementName) = {
    val now = userCreationMillis

    UserState(
      userId,
      userCreationMillis,
      0L,
      false,
      null,
      ImplicitlyIssuedResourceEventsSnapshot(List(), now),
      Nil,
      Nil,
      LatestResourceEventsSnapshot(List(), now),
      0L,
      0L,
      ActiveStateSnapshot(isActive, now),
      CreditSnapshot(credits, now),
      AgreementSnapshot(List(Agreement(agreementName, userCreationMillis)), now),
      RolesSnapshot(roleNames, now),
      OwnedResourcesSnapshot(Nil, now),
      Nil,
      UserStateChangeReasonCodes.InitialCalculationCode,
      InitialUserStateCalculation
    )
  }

  def createInitialUserStateFrom(us: UserState): UserState = {
    createInitialUserState(
      us.userId,
      us.userCreationMillis,
      us.activeStateSnapshot.isActive,
      us.creditsSnapshot.creditAmount,
      us.rolesSnapshot.roles,
      us.agreementsSnapshot.agreementsByTimeslot.valuesIterator.toList.last
    )
  }

  def findUserStateAtEndOfBillingMonth(userId: String,
                                       billingMonthInfo: BillingMonthInfo,
                                       storeProvider: StoreProvider,
                                       currentUserState: UserState,
                                       defaultResourcesMap: DSLResourcesMap,
                                       accounting: Accounting,
                                       algorithmCompiler: CostPolicyAlgorithmCompiler,
                                       calculationReason: UserStateChangeReason,
                                       contextualLogger: Maybe[ContextualLogger] = NoVal): Maybe[UserState] = {

    val clog = ContextualLogger.fromOther(
      contextualLogger,
      logger,
      "findUserStateAtEndOfBillingMonth(%s)", billingMonthInfo)
    clog.begin()

    def doCompute: Maybe[UserState] = {
      doFullMonthlyBilling(
        userId,
        billingMonthInfo,
        storeProvider,
        currentUserState,
        defaultResourcesMap,
        accounting,
        algorithmCompiler,
        calculationReason,
        Just(clog))
    }

    val userStateStore = storeProvider.userStateStore
    val resourceEventStore = storeProvider.resourceEventStore

    val userCreationMillis = currentUserState.userCreationMillis
    val userCreationDateCalc = new MutableDateCalc(userCreationMillis)
    val billingMonthStartMillis = billingMonthInfo.startMillis
    val billingMonthStopMillis  = billingMonthInfo.stopMillis

    if(billingMonthStopMillis < userCreationMillis) {
      // If the user did not exist for this billing month, piece of cake
      clog.debug("User did not exist before %s", userCreationDateCalc)

      // NOTE: Reason here will be: InitialUserStateCalculation
      val initialUserState0 = createInitialUserStateFrom(currentUserState)
      val initialUserStateM = userStateStore.storeUserState2(initialUserState0)

      clog.debug("Returning ZERO state [_idM=%s] %s".format(initialUserStateM.map(_._id), initialUserStateM))
      clog.end()

      initialUserStateM
    } else {
      // Ask DB cache for the latest known user state for this billing period
      val latestUserStateM = userStateStore.findLatestUserStateForEndOfBillingMonth(
        userId,
        billingMonthInfo.year,
        billingMonthInfo.month)

      latestUserStateM match {
        case NoVal ⇒
          // Not found, must compute
          clog.debug("No user state found from cache, will have to (re)compute")
          val result = doCompute
          clog.end()
          result

        case failed @ Failed(_, _) ⇒
          clog.warn("Failure while quering cache for user state: %s", failed)
          clog.end()
          failed

        case Just(latestUserState) ⇒
          // Found a "latest" user state but need to see if it is indeed the true and one latest.
          // For this reason, we must count the events again.
         val latestStateOOSEventsCounter = latestUserState.billingPeriodOutOfSyncResourceEventsCounter
         val actualOOSEventsCounterM = resourceEventStore.countOutOfSyncEventsForBillingPeriod(
           userId,
           billingMonthStartMillis,
           billingMonthStopMillis)

         actualOOSEventsCounterM match {
           case NoVal ⇒
             val errMsg = "No counter computed for out of sync events. Should at least be zero."
             clog.warn(errMsg)
             val result = Failed(new Exception(errMsg))
             clog.end()
             result

           case failed @ Failed(_, _) ⇒
             clog.warn("Failure while querying for out of sync events: %s", failed)
             clog.end()
             failed

           case Just(actualOOSEventsCounter) ⇒
             val counterDiff = actualOOSEventsCounter - latestStateOOSEventsCounter
             counterDiff match {
               // ZERO, we are OK!
               case 0 ⇒
                 // NOTE: Keep the caller's calculation reason
                 Just(latestUserState.copyForChangeReason(calculationReason))

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
                 val result = Failed(new Exception(errMsg))
                 clog.end()
                 result
             }
         }
      }
    }
  }

  //+ Utility methods
  def rcDebugInfo(rcEvent: ResourceEvent) = {
    rcEvent.toDebugString(false)
  }
  //- Utility methods

  def processResourceEvent(startingUserState: UserState,
                           userStateWorker: UserStateWorker,
                           currentResourceEvent: ResourceEvent,
                           policyStore: PolicyStore,
                           stateChangeReason: UserStateChangeReason,
                           billingMonthInfo: BillingMonthInfo,
                           walletEntriesBuffer: mutable.Buffer[NewWalletEntry],
                           algorithmCompiler: CostPolicyAlgorithmCompiler,
                           clogM: Maybe[ContextualLogger] = NoVal): UserState = {

    val clog = ContextualLogger.fromOther(clogM, logger, "walletEntriesForResourceEvent(%s)", currentResourceEvent.id)

    var _workingUserState = startingUserState

    val theResource = currentResourceEvent.safeResource
    val theInstanceId = currentResourceEvent.safeInstanceId
    val theValue = currentResourceEvent.value

    val accounting = userStateWorker.accounting
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
          val previousResourceEventM = userStateWorker.findAndRemovePreviousResourceEvent(theResource, theInstanceId)
          clog.debug("PreviousM %s", previousResourceEventM.map(rcDebugInfo(_)))

          val havePreviousResourceEvent = previousResourceEventM.isJust
          val needPreviousResourceEvent = costPolicy.needsPreviousEventForCreditAndAmountCalculation
          if(needPreviousResourceEvent && !havePreviousResourceEvent) {
            // This must be the first resource event of its kind, ever.
            // TODO: We should normally check the DB to verify the claim (?)
            clog.info("Ignoring first event of its kind %s", currentResourceEventDebugInfo)
            userStateWorker.updateIgnored(currentResourceEvent)
          } else {
            val defaultInitialAmount = costPolicy.getResourceInstanceInitialAmount
            val oldAmount = _workingUserState.getResourceInstanceAmount(theResource, theInstanceId, defaultInitialAmount)
            val oldCredits = _workingUserState.creditsSnapshot.creditAmount

            // A. Compute new resource instance accumulating amount
            val newAmount = costPolicy.computeNewAccumulatingAmount(oldAmount, theValue)

            clog.debug("theValue = %s, oldAmount = %s, newAmount = %s, oldCredits = %s", theValue, oldAmount, newAmount, oldCredits)

            // B. Compute new wallet entries
            clog.debug("agreementsSnapshot = %s", _workingUserState.agreementsSnapshot)
            val alltimeAgreements = _workingUserState.agreementsSnapshot.agreementsByTimeslot

            //              clog.debug("Computing full chargeslots")
            val fullChargeslotsM = accounting.computeFullChargeslots(
              previousResourceEventM,
              currentResourceEvent,
              oldCredits,
              oldAmount,
              newAmount,
              dslResource,
              resourcesMap,
              alltimeAgreements,
              algorithmCompiler,
              policyStore,
              Just(clog)
            )

            // We have the chargeslots, let's associate them with the current event
            fullChargeslotsM match {
              case Just((referenceTimeslot, fullChargeslots)) ⇒
                if(fullChargeslots.length == 0) {
                  // At least one chargeslot is required.
                  throw new Exception("No chargeslots computed for resource event %s".format(currentResourceEvent.id))
                }
                clog.debugSeq("fullChargeslots", fullChargeslots, 0)

                // C. Compute new credit amount (based on the charge slots)
                val newCreditsDiff = fullChargeslots.map(_.computedCredits.get).sum
                val newCredits = oldCredits - newCreditsDiff

                if(stateChangeReason.shouldStoreCalculatedWalletEntries) {
                  val newWalletEntry = NewWalletEntry(
                    userStateWorker.userId,
                    newCreditsDiff,
                    oldCredits,
                    newCredits,
                    TimeHelpers.nowMillis,
                    referenceTimeslot,
                    billingMonthInfo.year,
                    billingMonthInfo.month,
                    if(havePreviousResourceEvent)
                      List(currentResourceEvent, justForSure(previousResourceEventM).get)
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
                  creditsSnapshot = CreditSnapshot(newCredits, TimeHelpers.nowMillis),
                  stateChangeCounter = _workingUserState.stateChangeCounter + 1,
                  totalEventsProcessedCounter = _workingUserState.totalEventsProcessedCounter + 1
                )

              case NoVal ⇒
                // At least one chargeslot is required.
                throw new Exception("No chargeslots computed")

              case failed@Failed(e, m) ⇒
                throw new Exception(m, e)
            }
          }
        }

        // After processing, all events billable or not update the previous state
        userStateWorker.updatePrevious(currentResourceEvent)

        _workingUserState = _workingUserState.copy(
          latestResourceEventsSnapshot = userStateWorker.previousResourceEvents.toImmutableSnapshot(TimeHelpers.nowMillis)
        )

      // We do not have a resource (and thus, no cost policy)
      case None ⇒
        // Now, this is a matter of politics: what do we do if no policy was found?
        clog.warn("Unknown resource for %s", currentResourceEventDebugInfo)
    } // dslResourceOpt match

    clog.end(currentResourceEventDebugInfo)

    _workingUserState
  }

  def processResourceEvents(resourceEvents: Traversable[ResourceEvent],
                            startingUserState: UserState,
                            userStateWorker: UserStateWorker,
                            policyStore: PolicyStore,
                            stateChangeReason: UserStateChangeReason,
                            billingMonthInfo: BillingMonthInfo,
                            walletEntriesBuffer: mutable.Buffer[NewWalletEntry],
                            algorithmCompiler: CostPolicyAlgorithmCompiler,
                            clogM: Maybe[ContextualLogger] = NoVal): UserState = {

    var _workingUserState = startingUserState

    for(currentResourceEvent <- resourceEvents) {

      _workingUserState = processResourceEvent(
        _workingUserState,
        userStateWorker,
        currentResourceEvent,
        policyStore,
        stateChangeReason,
        billingMonthInfo,
        walletEntriesBuffer,
        algorithmCompiler,
        clogM
      )
    }

    _workingUserState
  }


  def doFullMonthlyBilling(userId: String,
                           billingMonthInfo: BillingMonthInfo,
                           storeProvider: StoreProvider,
                           currentUserState: UserState,
                           defaultResourcesMap: DSLResourcesMap,
                           accounting: Accounting,
                           algorithmCompiler: CostPolicyAlgorithmCompiler,
                           calculationReason: UserStateChangeReason = NoSpecificChangeReason,
                           contextualLogger: Maybe[ContextualLogger] = NoVal): Maybe[UserState] = Maybe {


    val clog = ContextualLogger.fromOther(
      contextualLogger,
      logger,
      "doFullMonthlyBilling(%s)", billingMonthInfo)
    clog.begin()

    val clogJ = Just(clog)

    val previousBillingMonthUserStateM = findUserStateAtEndOfBillingMonth(
      userId,
      billingMonthInfo.previousMonth,
      storeProvider,
      currentUserState,
      defaultResourcesMap,
      accounting,
      algorithmCompiler,
      calculationReason.forPreviousBillingMonth,
      clogJ
    )

    if(previousBillingMonthUserStateM.isNoVal) {
      throw new Exception("Could not calculate initial user state for billing %s".format(billingMonthInfo))
    }
    if(previousBillingMonthUserStateM.isFailed) {
      throw failedForSure(previousBillingMonthUserStateM).exception
    }

    val startingUserState = justForSure(previousBillingMonthUserStateM).get

    val userStateStore = storeProvider.userStateStore
    val resourceEventStore = storeProvider.resourceEventStore
    val policyStore = storeProvider.policyStore

    val billingMonthStartMillis = billingMonthInfo.startMillis
    val billingMonthEndMillis = billingMonthInfo.stopMillis

    // Keep the working (current) user state. This will get updated as we proceed with billing for the month
    // specified in the parameters.
    // NOTE: The calculation reason is not the one we get from the previous user state but the one our caller specifies
    var _workingUserState = startingUserState.copyForChangeReason(calculationReason)

    val userStateWorker = UserStateWorker.fromUserState(_workingUserState, accounting, defaultResourcesMap)

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
      policyStore,
      calculationReason,
      billingMonthInfo,
      newWalletEntries,
      algorithmCompiler,
      clogJ
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
      userStateWorker.userId,
      LatestResourceEventsWorker.fromList(specialEvents),
      ImplicitlyIssuedResourceEventsWorker.Empty,
      IgnoredFirstResourceEventsWorker.Empty,
      userStateWorker.accounting,
      userStateWorker.resourcesMap
    )

    _workingUserState = processResourceEvents(
      theirImplicitEnds,
      _workingUserState,
      specialUserStateWorker,
      policyStore,
      calculationReason,
      billingMonthInfo,
      newWalletEntries,
      algorithmCompiler,
      clogJ
    )

    val lastUpdateTime = TimeHelpers.nowMillis

    _workingUserState = _workingUserState.copy(
      implicitlyIssuedSnapshot = userStateWorker.implicitlyIssuedStartEvents.toImmutableSnapshot(lastUpdateTime),
      latestResourceEventsSnapshot = userStateWorker.previousResourceEvents.toImmutableSnapshot(lastUpdateTime),
      stateChangeCounter = _workingUserState.stateChangeCounter + 1,
      parentUserStateId = startingUserState.idOpt,
      newWalletEntries = newWalletEntries.toList
    )

    clog.debug("calculationReason = %s", calculationReason)

    if(calculationReason.shouldStoreUserState) {
      val storedUserStateM = userStateStore.storeUserState2(_workingUserState)
      storedUserStateM match {
        case Just(storedUserState) ⇒
          clog.info("Saved [_id=%s] %s", storedUserState._id, storedUserState)
          _workingUserState = storedUserState
        case NoVal ⇒
          clog.warn("Could not store %s", _workingUserState)
        case failed @ Failed(e, m) ⇒
          clog.error(e, "Could not store %s", _workingUserState)
      }
    }

    clog.debug("RETURN %s", _workingUserState)
    clog.end()
    _workingUserState
  }
}

/**
 * A helper object holding intermediate state/results during resource event processing.
 *
 * @param previousResourceEvents
 *          This is a collection of all the latest resource events.
 *          We want these in order to correlate incoming resource events with their previous (in `occurredMillis` time)
 *          ones. Will be updated on processing the next resource event.
 *
 * @param implicitlyIssuedStartEvents
 *          The implicitly issued resource events at the beginning of the billing period.
 *
 * @param ignoredFirstResourceEvents
 *          The resource events that were first (and unused) of their kind.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class UserStateWorker(userId: String,
                           previousResourceEvents: LatestResourceEventsWorker,
                           implicitlyIssuedStartEvents: ImplicitlyIssuedResourceEventsWorker,
                           ignoredFirstResourceEvents: IgnoredFirstResourceEventsWorker,
                           accounting: Accounting,
                           resourcesMap: DSLResourcesMap) {

  /**
   * Finds the previous resource event by checking two possible sources: a) The implicitly terminated resource
   * events and b) the explicit previous resource events. If the event is found, it is removed from the
   * respective source.
   *
   * If the event is not found, then this must be for a new resource instance.
   * (and probably then some `zero` resource event must be implied as the previous one)
   *
   * @param resource
   * @param instanceId
   * @return
   */
  def findAndRemovePreviousResourceEvent(resource: String, instanceId: String): Maybe[ResourceEvent] = {
    // implicitly issued events are checked first
    implicitlyIssuedStartEvents.findAndRemoveResourceEvent(resource, instanceId) match {
      case just @ Just(_) ⇒
        just
      case NoVal ⇒
        // explicit previous resource events are checked second
        previousResourceEvents.findAndRemoveResourceEvent(resource, instanceId) match {
          case just @ Just(_) ⇒
            just
          case noValOrFailed ⇒
            noValOrFailed
        }
      case failed ⇒
        failed
    }
  }

  def updateIgnored(resourceEvent: ResourceEvent): Unit = {
    ignoredFirstResourceEvents.updateResourceEvent(resourceEvent)
  }

  def updatePrevious(resourceEvent: ResourceEvent): Unit = {
    previousResourceEvents.updateResourceEvent(resourceEvent)
  }

  def debugTheMaps(clog: ContextualLogger)(rcDebugInfo: ResourceEvent ⇒ String): Unit = {
    if(previousResourceEvents.size > 0) {
      val map = previousResourceEvents.latestEventsMap.map { case (k, v) => (k, rcDebugInfo(v)) }
      clog.debugMap("previousResourceEvents", map, 0)
    }
    if(implicitlyIssuedStartEvents.size > 0) {
      val map = implicitlyIssuedStartEvents.implicitlyIssuedEventsMap.map { case (k, v) => (k, rcDebugInfo(v)) }
      clog.debugMap("implicitlyTerminatedResourceEvents", map, 0)
    }
    if(ignoredFirstResourceEvents.size > 0) {
      val map = ignoredFirstResourceEvents.ignoredFirstEventsMap.map { case (k, v) => (k, rcDebugInfo(v)) }
      clog.debugMap("ignoredFirstResourceEvents", map, 0)
    }
  }

//  private[this]
//  def allPreviousAndAllImplicitlyStarted: List[ResourceEvent] = {
//    val buffer: FullMutableResourceTypeMap = scala.collection.mutable.Map[FullResourceType, ResourceEvent]()
//
//    buffer ++= implicitlyIssuedStartEvents.implicitlyIssuedEventsMap
//    buffer ++= previousResourceEvents.latestEventsMap
//
//    buffer.valuesIterator.toList
//  }

  /**
   * Find those events from `implicitlyIssuedStartEvents` and `previousResourceEvents` that will generate implicit
   * end events along with those implicitly issued events. Before returning, remove the events that generated the
   * implicit ends from the internal state of this instance.
   *
   * @see [[gr.grnet.aquarium.logic.accounting.dsl.DSLCostPolicy]]
   */
  def findAndRemoveGeneratorsOfImplicitEndEvents(newOccuredMillis: Long
                                                ): (List[ResourceEvent], List[ResourceEvent]) = {
    val buffer = mutable.ListBuffer[(ResourceEvent, ResourceEvent)]()
    val checkSet = mutable.Set[ResourceEvent]()

    def doItFor(map: ResourceEvent.FullMutableResourceTypeMap): Unit = {
      val resourceEvents = map.valuesIterator
      for {
        resourceEvent <- resourceEvents
        dslResource   <- resourcesMap.findResource(resourceEvent.safeResource)
        costPolicy    =  dslResource.costPolicy
      } {
        if(costPolicy.supportsImplicitEvents) {
          if(costPolicy.mustConstructImplicitEndEventFor(resourceEvent)) {
            val implicitEnd = costPolicy.constructImplicitEndEventFor(resourceEvent, newOccuredMillis)

            if(!checkSet.contains(resourceEvent)) {
              checkSet.add(resourceEvent)
              buffer append ((resourceEvent, implicitEnd))
            }

            // remove it anyway
            map.remove((resourceEvent.safeResource, resourceEvent.safeInstanceId))
          }
        }
      }
    }

    doItFor(previousResourceEvents.latestEventsMap)                // we give priority for previous
    doItFor(implicitlyIssuedStartEvents.implicitlyIssuedEventsMap) // ... over implicitly issued...

    (buffer.view.map(_._1).toList, buffer.view.map(_._2).toList)
  }
}

object UserStateWorker {
  def fromUserState(userState: UserState, accounting: Accounting, resourcesMap: DSLResourcesMap): UserStateWorker = {
    UserStateWorker(
      userState.userId,
      userState.latestResourceEventsSnapshot.toMutableWorker,
      userState.implicitlyIssuedSnapshot.toMutableWorker,
      IgnoredFirstResourceEventsWorker.Empty,
      accounting,
      resourcesMap
    )
  }
}
