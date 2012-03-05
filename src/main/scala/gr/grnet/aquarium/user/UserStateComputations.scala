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
import gr.grnet.aquarium.util.{ContextualLogger, Loggable}
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
                           millis: Long = TimeHelpers.nowMillis,
                           agreementName: String = DSLAgreement.DefaultAgreementName) = {
    val now = 0L
    UserState(
      userId,
      now,
      0L,
      false,
      null,
      ImplicitlyIssuedResourceEventsSnapshot(List(), now),
      Nil, Nil,
      LatestResourceEventsSnapshot(List(), now),
      0L, 0L,
      ActiveStateSnapshot(false, now),
      CreditSnapshot(0, now),
      AgreementSnapshot(Agreement(agreementName, now) :: Nil, now),
      RolesSnapshot(List(), now),
      OwnedResourcesSnapshot(List(), now)
    )
  }

  def createFirstUserState(userId: String, agreementName: String, resourcesMap: DSLResourcesMap) = {
      val now = 0L
      UserState(
        userId,
        now,
        0L,
        false,
        null,
        ImplicitlyIssuedResourceEventsSnapshot(List(), now),
        Nil, Nil,
        LatestResourceEventsSnapshot(List(), now),
        0L, 0L,
        ActiveStateSnapshot(false, now),
        CreditSnapshot(0, now),
        AgreementSnapshot(Agreement(agreementName, now) :: Nil, now),
        RolesSnapshot(List(), now),
        OwnedResourcesSnapshot(List(), now)
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
                                       defaultPolicy: DSLPolicy,
                                       defaultResourcesMap: DSLResourcesMap,
                                       accounting: Accounting,
                                       contextualLogger: Maybe[ContextualLogger] = NoVal): Maybe[UserState] = {

    val clog = ContextualLogger.fromOther(
      contextualLogger,
      logger,
      "findUserStateAtEndOfBillingMonth(%s)", billingMonthInfo)
    clog.begin()

    def doCompute: Maybe[UserState] = {
      clog.debug("Computing full month billing")
      doFullMonthlyBilling(
        userId,
        billingMonthInfo,
        userStateStore,
        resourceEventStore,
        policyStore,
        userCreationMillis,
        currentUserState,
        zeroUserState,
        defaultPolicy,
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
      clog.debug("Returning %s".format(zeroUserState))
      clog.endWith(Just(zeroUserState))
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
          clog.endWith(doCompute)
          
        case failed @ Failed(_, _) ⇒
          clog.warn("Failure while quering cache for user state: %s", failed)
          clog.endWith(failed)

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
             clog.endWith(Failed(new Exception(errMsg)))

           case failed @ Failed(_, _) ⇒
             clog.warn("Failure while querying for out of sync events: %s", failed)
             clog.endWith(failed)

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
                 clog.endWith(doCompute)

               // We had less????
               case n if n < 0 ⇒
                 val errMsg = "Found %s out of sync events (%s less). DB must be inconsistent".format(actualOOSEventsCounter, n)
                 clog.warn(errMsg)
                 clog.endWith(Failed(new Exception(errMsg)))
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
                           defaultPolicy: DSLPolicy,
                           defaultResourcesMap: DSLResourcesMap,
                           accounting: Accounting,
                           contextualLogger: Maybe[ContextualLogger] = NoVal): Maybe[UserState] = Maybe {

    def rcDebugInfo(rcEvent: ResourceEvent) = {
      rcEvent.toDebugString(defaultResourcesMap, false)
    }

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
      defaultPolicy,
      defaultResourcesMap,
      accounting,
      Just(clog)
    )
    
    previousBillingMonthUserStateM match {
      case NoVal ⇒
        null // not really... (must throw an exception here probably...)
      case failed @ Failed(e, _) ⇒
        throw e
      case Just(startingUserState) ⇒
        // This is the real deal

        // This is a collection of all the latest resource events.
        // We want these in order to correlate incoming resource events with their previous (in `occurredMillis` time)
        // ones.
        // Will be updated on processing the next resource event.
        val previousResourceEvents = startingUserState.latestResourceEventsSnapshot.toMutableWorker
        clog.debug("previousResourceEvents = %s", previousResourceEvents)

        val billingMonthStartMillis = billingMonthInfo.startMillis
        val billingMonthEndMillis = billingMonthInfo.stopMillis

        // Keep the working (current) user state. This will get updated as we proceed with billing for the month
        // specified in the parameters.
        var _workingUserState = startingUserState

        // Prepare the implicitly terminated resource events from previous billing period
        val implicitlyTerminatedResourceEvents = _workingUserState.implicitlyTerminatedSnapshot.toMutableWorker
        if(implicitlyTerminatedResourceEvents.size > 0) {
          clog.debug("%s implicitlyTerminatedResourceEvents", implicitlyTerminatedResourceEvents.size)
          clog.withIndent {
            implicitlyTerminatedResourceEvents.foreach(ev ⇒ clog.debug("%s", rcDebugInfo(ev)))
          }
        }

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

        // Find the actual resource events from DB
        val allResourceEventsForMonth = resourceEventStore.findAllRelevantResourceEventsForBillingPeriod(
          userId,
          billingMonthStartMillis,
          billingMonthEndMillis)
        var _eventCounter = 0

        clog.debug("resourceEventStore = %s".format(resourceEventStore))
        if(allResourceEventsForMonth.size > 0) {
          clog.debug("Found %s resource events, starting processing...", allResourceEventsForMonth.size)
        } else {
          clog.debug("Not found any resource events")
        }

        for {
          currentResourceEvent <- allResourceEventsForMonth
        } {
          _eventCounter = _eventCounter + 1
          val theResource = currentResourceEvent.safeResource
          val theInstanceId = currentResourceEvent.safeInstanceId
          val theValue = currentResourceEvent.value

          clog.indent()
          clog.debug("")
          clog.debug("Processing %s", currentResourceEvent)
          clog.debug("+========= %s", rcDebugInfo(currentResourceEvent))

          clog.indent()

          if(previousResourceEvents.size > 0) {
            clog.debug("%s previousResourceEvents", previousResourceEvents.size)
            clog.withIndent {
              previousResourceEvents.foreach(ev ⇒ clog.debug("%s", rcDebugInfo(ev)))
            }
          }
          if(implicitlyTerminatedResourceEvents.size > 0) {
            clog.debug("%s implicitlyTerminatedResourceEvents", implicitlyTerminatedResourceEvents.size)
            clog.withIndent {
              implicitlyTerminatedResourceEvents.foreach(ev ⇒ clog.debug("%s", rcDebugInfo(ev)))
            }
          }
          if(ignoredFirstResourceEvents.size > 0) {
            clog.debug("%s ignoredFirstResourceEvents", ignoredFirstResourceEvents.size)
            clog.withIndent {
              ignoredFirstResourceEvents.foreach(ev ⇒ clog.debug("%s", rcDebugInfo(ev)))
            }
          }

          // Ignore the event if it is not billable (but still record it in the "previous" stuff).
          // But to make this decision, first we need the resource definition (and its cost policy).
          val resourceDefM = defaultResourcesMap.findResourceM(theResource)
          resourceDefM match {
            // We have a resource (and thus a cost policy)
            case Just(resourceDef) ⇒
              val costPolicy = resourceDef.costPolicy
              clog.debug("Cost policy: %s", costPolicy)
              val isBillable = costPolicy.isBillableEventBasedOnValue(theValue)
              isBillable match {
                // The resource event is not billable
                case false ⇒
                  clog.debug("Ignoring not billable event %s", rcDebugInfo(currentResourceEvent))

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
                    clog.info("Ignoring first event of its kind %s", rcDebugInfo(currentResourceEvent))
                    ignoredFirstResourceEvents.updateResourceEvent(currentResourceEvent)
                  } else {
                    val defaultInitialAmount = costPolicy.getResourceInstanceInitialAmount
                    val oldAmount = _workingUserState.getResourceInstanceAmount(theResource, theInstanceId, defaultInitialAmount)
                    val oldCredits = _workingUserState.creditsSnapshot.creditAmount

                    // A. Compute new resource instance accumulating amount
                    val newAmount = costPolicy.computeNewAccumulatingAmount(oldAmount, theValue)
                    
                    clog.debug("theValue = %s, oldAmount = %s, newAmount = %s, oldCredits = %s", theValue, oldAmount, newAmount, oldCredits)

                    // B. Compute new wallet entries
                    val alltimeAgreements = _workingUserState.agreementsSnapshot.agreementsByTimeslot

                    val fullChargeslotsM = accounting.computeFullChargeslots(
                      previousResourceEventM,
                      currentResourceEvent,
                      oldCredits,
                      oldAmount,
                      newAmount,
                      resourceDef,
                      defaultResourcesMap,
                      alltimeAgreements,
                      SimpleCostPolicyAlgorithmCompiler,
                      Just(clog)
                    )

                    // We have the chargeslots, let's associate them with the current event
                    fullChargeslotsM match {
                      case Just(fullChargeslots) ⇒
                        if(fullChargeslots.length == 0) {
                          // At least one chargeslot is required.
                          throw new Exception("No chargeslots computed")
                        }
                        clog.debug("chargeslots:")
                        clog.withIndent {
                          for(fullChargeslot <- fullChargeslots) {
                            clog.debug("%s", fullChargeslot)
                          }
                        }
                        
                        // C. Compute new credit amount (based on the charge slots)
                        val newCreditsDiff = fullChargeslots.map(_.computedCredits.get).sum
                        val newCredits = oldCredits + newCreditsDiff
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
                          resourceDef
                        )

                        clog.debug("New %s", newWalletEntry)

                      case NoVal ⇒
                        // At least one chargeslot is required.
                        throw new Exception("No chargeslots computed")

                      case failed @ Failed(e, m) ⇒
                        throw new Exception(m, e)
                    }
                  }

              }

              // After processing, all event, billable or not update the previous state
              previousResourceEvents.updateResourceEvent(currentResourceEvent)

            // We do not have a resource (and no cost policy)
            case NoVal ⇒
              // Now, this is a matter of politics: what do we do if no policy was found?
              clog.error("No cost policy for %s", rcDebugInfo(currentResourceEvent))

            // Could not retrieve resource (unlikely to happen)
            case failed @ Failed(e, m) ⇒
              clog.error("Error obtaining cost policy for %s", rcDebugInfo(currentResourceEvent))
              clog.error(e, m)
          }

          clog.unindent()
          clog.debug("-========= %s", rcDebugInfo(currentResourceEvent))
          clog.unindent()
        }
        

        clog.endWith(_workingUserState)
    }
  }
}
