/*
 * Copyright 2011 GRNET S.A. All rights reserved.
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


import com.ckkloverdos.maybe.{Failed, NoVal, Just, Maybe}
import gr.grnet.aquarium.store.{PolicyStore, UserStateStore, ResourceEventStore}
import gr.grnet.aquarium.util.{ContextualLogger, Loggable, justForSure, failedForSure}
import gr.grnet.aquarium.logic.accounting.Accounting
import gr.grnet.aquarium.logic.accounting.algorithm.SimpleCostPolicyAlgorithmCompiler
import gr.grnet.aquarium.logic.events.{NewWalletEntry, ResourceEvent}
import gr.grnet.aquarium.util.date.{TimeHelpers, MutableDateCalc}
import gr.grnet.aquarium.logic.accounting.dsl.{DSLAgreement, DSLCostPolicy, DSLResourcesMap, DSLPolicy}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class UserStateComputations extends Loggable {
  def createFirstUserState(userId: String,
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
      OwnedResourcesSnapshot(Nil, now)
    )
  }

  def findUserStateAtEndOfBillingMonth(userId: String,
                                       billingMonthInfo: BillingMonthInfo,
                                       userStateStore: UserStateStore,
                                       resourceEventStore: ResourceEventStore,
                                       policyStore: PolicyStore,
                                       userCreationMillis: Long,
                                       currentUserState: UserState,
                                       zeroUserState: UserState, 
                                       defaultResourcesMap: DSLResourcesMap,
                                       accounting: Accounting,
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
        userStateStore,
        resourceEventStore,
        policyStore,
        userCreationMillis,
        currentUserState,
        zeroUserState,
        defaultResourcesMap,
        accounting,
        Just(clog))
    }

    val userCreationDateCalc = new MutableDateCalc(userCreationMillis)
    val billingMonthStartMillis = billingMonthInfo.startMillis
    val billingMonthStopMillis  = billingMonthInfo.stopMillis

    if(billingMonthStopMillis < userCreationMillis) {
      // If the user did not exist for this billing month, piece of cake
      clog.debug("User did not exist before %s", userCreationDateCalc)
      clog.debug("Returning ZERO state %s".format(zeroUserState))
      clog.end()
      Just(zeroUserState)
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
                 latestUserStateM

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

  def doFullMonthlyBilling(userId: String,
                           billingMonthInfo: BillingMonthInfo,
                           userStateStore: UserStateStore,
                           resourceEventStore: ResourceEventStore,
                           policyStore: PolicyStore,
                           userCreationMillis: Long,
                           currentUserState: UserState,
                           zeroUserState: UserState,
                           defaultResourcesMap: DSLResourcesMap,
                           accounting: Accounting,
                           contextualLogger: Maybe[ContextualLogger] = NoVal): Maybe[UserState] = Maybe {

    //+ Utility methods
    def rcDebugInfo(rcEvent: ResourceEvent) = {
      rcEvent.toDebugString(defaultResourcesMap, false)
    }
    //- Utility methods

    val clog = ContextualLogger.fromOther(
      contextualLogger,
      logger,
      "doFullMonthlyBilling(%s)", billingMonthInfo)
    clog.begin()

    val previousBillingMonthUserStateM = findUserStateAtEndOfBillingMonth(
      userId,
      billingMonthInfo.previousMonth,
      userStateStore,
      resourceEventStore,
      policyStore,
      userCreationMillis,
      currentUserState,
      zeroUserState,
      defaultResourcesMap,
      accounting,
      Just(clog)
    )
    
    if(previousBillingMonthUserStateM.isNoVal) {
      throw new Exception("Could not calculate initial user state for billing %s".format(billingMonthInfo))
    }
    if(previousBillingMonthUserStateM.isFailed) {
      throw failedForSure(previousBillingMonthUserStateM).exception
    }

    val billingMonthStartMillis = billingMonthInfo.startMillis
    val billingMonthEndMillis = billingMonthInfo.stopMillis

    val startingUserState = justForSure(previousBillingMonthUserStateM).get
    // Keep the working (current) user state. This will get updated as we proceed with billing for the month
    // specified in the parameters.
    var _workingUserState = startingUserState

    // This is a collection of all the latest resource events.
    // We want these in order to correlate incoming resource events with their previous (in `occurredMillis` time)
    // ones.
    // Will be updated on processing the next resource event.
    val previousResourceEvents = startingUserState.latestResourceEventsSnapshot.toMutableWorker
    // Prepare the implicitly terminated resource events from previous billing period
    val implicitlyTerminatedResourceEvents = _workingUserState.implicitlyTerminatedSnapshot.toMutableWorker
    // Keep the resource events from this period that were first (and unused) of their kind
    val ignoredFirstResourceEvents = IgnoredFirstResourceEventsWorker.Empty

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
      // implicitly terminated events are checked first
      implicitlyTerminatedResourceEvents.findAndRemoveResourceEvent(resource, instanceId) match {
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

    def debugTheMaps(): Unit = {
      if(previousResourceEvents.size > 0) {
        val map = previousResourceEvents.resourceEventsMap.map { case (k, v) => (k, rcDebugInfo(v)) }
        clog.debugMap("previousResourceEvents", map, 0)
      }
      if(implicitlyTerminatedResourceEvents.size > 0) {
        val map = implicitlyTerminatedResourceEvents.implicitlyIssuedEventsMap.map { case (k, v) => (k, rcDebugInfo(v)) }
        clog.debug("implicitlyTerminatedResourceEvents", map, 0)
      }
      if(ignoredFirstResourceEvents.size > 0) {
        val map = ignoredFirstResourceEvents.ignoredFirstEventsMap.map { case (k, v) => (k, rcDebugInfo(v)) }
        clog.debug("%s ignoredFirstResourceEvents", map, 0)
      }
    }

    debugTheMaps()

    // Find the actual resource events from DB
    val allResourceEventsForMonth = resourceEventStore.findAllRelevantResourceEventsForBillingPeriod(
      userId,
      billingMonthStartMillis,
      billingMonthEndMillis)
    var _eventCounter = 0

    if(allResourceEventsForMonth.size > 0) {
      clog.debug("Found %s resource events, starting processing...", allResourceEventsForMonth.size)
    } else {
      clog.debug("Not found any resource events")
    }

    for {
      currentResourceEvent <- allResourceEventsForMonth
    } {
      _eventCounter = _eventCounter + 1
      
      if(_eventCounter == 1) {
        clog.debugMap("defaultResourcesMap", defaultResourcesMap.map, 1)
      } else {
        clog.debug("")
      }
      
      val theResource = currentResourceEvent.safeResource
      val theInstanceId = currentResourceEvent.safeInstanceId
      val theValue = currentResourceEvent.value

      clog.debug("Processing %s", currentResourceEvent)
      val currentResourceEventDebugInfo = rcDebugInfo(currentResourceEvent)
      clog.begin(currentResourceEventDebugInfo)

      debugTheMaps()

      // Ignore the event if it is not billable (but still record it in the "previous" stuff).
      // But to make this decision, first we need the resource definition (and its cost policy).
      val dslResourceOpt = defaultResourcesMap.findResource(theResource)

      dslResourceOpt match {
        // We have a resource (and thus a cost policy)
        case Some(dslResource) ⇒
          val costPolicy = dslResource.costPolicy
          clog.debug("Cost policy %s for %s", costPolicy, dslResource)
          val isBillable = costPolicy.isBillableEventBasedOnValue(theValue)
          isBillable match {
            // The resource event is not billable
            case false ⇒
              clog.debug("Ignoring not billable event %s", currentResourceEventDebugInfo)

            // The resource event is billable
            case true ⇒
              // Find the previous event.
              // This is (potentially) needed to calculate new credit amount and new resource instance amount
              val previousResourceEventM = findAndRemovePreviousResourceEvent(theResource, theInstanceId)
              clog.debug("PreviousM %s", previousResourceEventM.map(rcDebugInfo(_)))

              val havePreviousResourceEvent = previousResourceEventM.isJust
              val needPreviousResourceEvent = costPolicy.needsPreviousEventForCreditAndAmountCalculation
              if(needPreviousResourceEvent && !havePreviousResourceEvent) {
                // This must be the first resource event of its kind, ever.
                // TODO: We should normally check the DB to verify the claim (?)
                clog.info("Ignoring first event of its kind %s", currentResourceEventDebugInfo)
                ignoredFirstResourceEvents.updateResourceEvent(currentResourceEvent)
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

                clog.debug("Computing full chargeslots")
                val fullChargeslotsM = accounting.computeFullChargeslots(
                  previousResourceEventM,
                  currentResourceEvent,
                  oldCredits,
                  oldAmount,
                  newAmount,
                  dslResource,
                  defaultResourcesMap,
                  alltimeAgreements,
                  SimpleCostPolicyAlgorithmCompiler,
                  policyStore,
                  Just(clog)
                )

                // We have the chargeslots, let's associate them with the current event
                fullChargeslotsM match {
                  case Just(fullChargeslots) ⇒
                    if(fullChargeslots.length == 0) {
                      // At least one chargeslot is required.
                      throw new Exception("No chargeslots computed for resource event %s".format(currentResourceEvent.id))
                    }
                    clog.debugSeq("fullChargeslots", fullChargeslots, 1)

                    // C. Compute new credit amount (based on the charge slots)
                    val newCreditsDiff = fullChargeslots.map(_.computedCredits.get).sum
                    val newCredits = oldCredits - newCreditsDiff
                    clog.debug("newCreditsDiff = %s, newCredits = %s", newCreditsDiff, newCredits)

                    val newWalletEntry = NewWalletEntry(
                      userId,
                      newCreditsDiff,
                      oldCredits,
                      newCredits,
                      TimeHelpers.nowMillis,
                      billingMonthInfo.year,
                      billingMonthInfo.month,
                      currentResourceEvent,
                      previousResourceEventM.toOption,
                      fullChargeslots,
                      dslResource
                    )

                    clog.debug("New %s", newWalletEntry)

                    _workingUserState = _workingUserState.copy(
                      creditsSnapshot = CreditSnapshot(newCredits, TimeHelpers.nowMillis),
                      stateChangeCounter = _workingUserState.stateChangeCounter + 1,
                      totalEventsProcessedCounter = _workingUserState.totalEventsProcessedCounter + 1
                    )

                  case NoVal ⇒
                    // At least one chargeslot is required.
                    throw new Exception("No chargeslots computed")

                  case failed @ Failed(e, m) ⇒
                    throw new Exception(m, e)
                }
              }

          } // isBillable

          // After processing, all event, billable or not update the previous state
          previousResourceEvents.updateResourceEvent(currentResourceEvent)

        // We do not have a resource (and no cost policy)
        case None ⇒
          // Now, this is a matter of politics: what do we do if no policy was found?
          clog.warn("No cost policy for %s", currentResourceEventDebugInfo)
      } // dslResourceOpt match

      clog.end(currentResourceEventDebugInfo)
    } // for { currentResourceEvent <- allResourceEventsForMonth }

    val lastUpdateTime = TimeHelpers.nowMillis

    _workingUserState = _workingUserState.copy(
      implicitlyTerminatedSnapshot = implicitlyTerminatedResourceEvents.toImmutableSnapshot(lastUpdateTime),
      latestResourceEventsSnapshot = previousResourceEvents.toImmutableSnapshot(lastUpdateTime),
      stateChangeCounter = _workingUserState.stateChangeCounter + 1
    )
    
    clog.debug("RETURN %s", _workingUserState)
    clog.end()
    _workingUserState
  }
}
