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

import scala.collection.mutable

import com.ckkloverdos.maybe.{Failed, NoVal, Just, Maybe}
import gr.grnet.aquarium.logic.accounting.Accounting
import gr.grnet.aquarium.util.date.DateCalculator
import gr.grnet.aquarium.logic.accounting.dsl.{DSLResourcesMap, DSLCostPolicy, DSLPolicy}
import gr.grnet.aquarium.logic.events.ResourceEvent
import gr.grnet.aquarium.store.{PolicyStore, UserStateStore, ResourceEventStore}
import gr.grnet.aquarium.util.{ContextualLogger, Loggable}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class UserStateComputations extends Loggable {
  def createFirstUserState(userId: String, agreementName: String = "default") = {
    val now = 0L
    UserState(
      userId,
      now,
      0L,
      false,
      null,
      Nil, Nil, Nil,
      LatestResourceEventsSnapshot(Map(), now),
      0L,
      ActiveStateSnapshot(false, now),
      CreditSnapshot(0, now),
      AgreementSnapshot(Agreement(agreementName, now, -1) :: Nil, now),
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
        Nil, Nil,Nil,
        LatestResourceEventsSnapshot(Map(), now),
        0L,
        ActiveStateSnapshot(false, now),
        CreditSnapshot(0, now),
        AgreementSnapshot(Agreement(agreementName, now, - 1) :: Nil, now),
        RolesSnapshot(List(), now),
        OwnedResourcesSnapshot(List(), now)
      )
    }

  def findUserStateAtEndOfBillingMonth(userId: String,
                                       yearOfBillingMonth: Int,
                                       billingMonth: Int,
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
      "findUserStateAtEndOfBillingMonth(%s-%02d)", yearOfBillingMonth, billingMonth)
//    val clog = new ContextualLogger(logger, "findUserStateAtEndOfBillingMonth(%s-%02d)", yearOfBillingMonth, billingMonth)
    clog.begin()

    def doCompute: Maybe[UserState] = {
      clog.debug("Computing full month billing")
      doFullMonthlyBilling(
        userId,
        yearOfBillingMonth,
        billingMonth,
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

    val billingMonthStartDateCalc = new DateCalculator(yearOfBillingMonth, billingMonth)
    val userCreationDateCalc = new DateCalculator(userCreationMillis)
    val billingMonthStartMillis = billingMonthStartDateCalc.toMillis
    val billingMonthStopMillis  = billingMonthStartDateCalc.copy.goEndOfThisMonth.toMillis

    if(billingMonthStopMillis < userCreationMillis) {
      // If the user did not exist for this billing month, piece of cake
      clog.debug("User did not exist before %s. Returning %s", userCreationDateCalc, zeroUserState)
      clog.endWith(Just(zeroUserState))
    } else {
      resourceEventStore.countOutOfSyncEventsForBillingPeriod(userId, billingMonthStartMillis, billingMonthStopMillis) match {
        case Just(outOfSyncEventCount) ⇒
          // Have out of sync, so must recompute
          clog.debug("Found %s out of sync events, will have to (re)compute user state", outOfSyncEventCount)
          clog.endWith(doCompute)
        case NoVal ⇒
          // No out of sync events, ask DB cache
          userStateStore.findLatestUserStateForEndOfBillingMonth(userId, yearOfBillingMonth, billingMonth) match {
            case just @ Just(userState) ⇒
              // Found from cache
              clog.debug("Found from cache: %s", userState)
              clog.endWith(just)
            case NoVal ⇒
              // otherwise compute
              clog.debug("No user state found from cache, will have to (re)compute")
              clog.endWith(doCompute)
            case failed @ Failed(_, _) ⇒
              clog.warn("Failure while quering cache for user state: %s", failed)
              clog.endWith(failed)
          }
        case failed @ Failed(_, _) ⇒
          clog.warn("Failure while querying for out of sync events: %s", failed)
          clog.endWith(failed)
      }
    }
  }

  def doFullMonthlyBilling(userId: String,
                           yearOfBillingMonth: Int,
                           billingMonth: Int,
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


    val billingMonthStartDateCalc = new DateCalculator(yearOfBillingMonth, billingMonth)
    val billingMonthEndDateCalc   = billingMonthStartDateCalc.copy.goEndOfThisMonth
    val previousBillingMonthCalc = billingMonthStartDateCalc.copy.goPreviousMonth
    val previousBillingMonth = previousBillingMonthCalc.getMonthOfYear
    val yearOfPreviousBillingMonth = previousBillingMonthCalc.getYear

    val clog = ContextualLogger.fromOther(
      contextualLogger,
      logger,
      "doFullMonthlyBilling(%s, %s)", billingMonthStartDateCalc.toYYYYMMDD, billingMonthEndDateCalc.toYYYYMMDD)
    clog.begin()

    val previousBillingMonthUserStateM = findUserStateAtEndOfBillingMonth(
      userId,
      yearOfPreviousBillingMonth,
      previousBillingMonth,
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
        NoVal // not really... (must throw an exception here probably...)
      case failed @ Failed(_, _) ⇒
        failed
      case Just(startingUserState) ⇒
        // This is the real deal

        // This is a collection of all the latest resource events.
        // We want these in order to correlate incoming resource events with their previous (in `occurredMillis` time)
        // ones.
        // Will be updated on processing the next resource event.
        val previousResourceEvents = startingUserState.latestResourceEvents.toMutableWorker
        clog.debug("previousResourceEvents = %s", previousResourceEvents)

        val billingMonthStartMillis = billingMonthStartDateCalc.toMillis
        val billingMonthEndMillis  = billingMonthEndDateCalc.toMillis

        // Keep the working (current) user state. This will get updated as we proceed billing within the month
        var _workingUserState = startingUserState

        val allResourceEventsForMonth = resourceEventStore.findAllRelevantResourceEventsForBillingPeriod(
          userId,
          billingMonthStartMillis,
          billingMonthEndMillis)

        clog.debug("resourceEventStore = %s".format(resourceEventStore))
        clog.debug("Found %s resource events", allResourceEventsForMonth.size)
    }

    clog.end()
    null
  }


  /**
   * Find the previous resource event, if needed by the event's cost policy,
   * in order to use it for any credit calculations.
   */
  def findPreviousRCEventOf(rcEvent: ResourceEvent,
                            costPolicy: DSLCostPolicy,
                            previousRCEventsMap: mutable.Map[ResourceEvent.FullResourceType, ResourceEvent]): Maybe[ResourceEvent] = {

    if(costPolicy.needsPreviousEventForCreditCalculation) {
      // Get a previous resource only if this is needed by the policy
      previousRCEventsMap.get(rcEvent.fullResourceInfo) match {
        case Some(previousRCEvent) ⇒
          Just(previousRCEvent)
        case None ⇒
          queryForPreviousRCEvent(rcEvent)
      }
    } else {
      // No need for previous event. Will return NoVal
      NoVal
    }
  }

  /**
   * FIXME: implement
   */
  def queryForPreviousRCEvent(rcEvent: ResourceEvent): Maybe[ResourceEvent] = {
    NoVal
  }

  type FullResourceType = ResourceEvent.FullResourceType
  def updatePreviousRCEventWith(previousRCEventsMap: mutable.Map[FullResourceType, ResourceEvent],
                                newRCEvent: ResourceEvent): Unit = {
    previousRCEventsMap(newRCEvent.fullResourceInfo) = newRCEvent
  }
}
