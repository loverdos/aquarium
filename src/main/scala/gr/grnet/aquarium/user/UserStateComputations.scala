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
import gr.grnet.aquarium.util.Loggable
import gr.grnet.aquarium.logic.events.ResourceEvent
import gr.grnet.aquarium.store.{PolicyStore, UserStateStore, ResourceEventStore}

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
      Nil, Nil,Nil,
      0L,
      ActiveSuspendedSnapshot(false, now),
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
        0L,
        ActiveSuspendedSnapshot(false, now),
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
                                       accounting: Accounting): Maybe[UserState] = {

    def D(fmt: String, args: Any*) = {
      logger.debug("[%s, %s-%02d] %s".format(userId, yearOfBillingMonth, billingMonth, fmt.format(args:_*)))
    }

    def E(fmt: String, args: Any*) = {
      logger.error("[%s, %s-%02d] %s".format(userId, yearOfBillingMonth, billingMonth, fmt.format(args:_*)))
    }

    def W(fmt: String, args: Any*) = {
      logger.error("[%s, %s-%02d] %s".format(userId, yearOfBillingMonth, billingMonth, fmt.format(args:_*)))
    }

    def doCompute: Maybe[UserState] = {
      D("Computing full month billing")
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
        accounting)
    }

    val billingMonthStartDateCalc = new DateCalculator(yearOfBillingMonth, billingMonth)
    val billingMonthStartMillis = billingMonthStartDateCalc.toMillis
    val billingMonthStopMillis  = billingMonthStartDateCalc.goEndOfThisMonth.toMillis

    if(billingMonthStartMillis > userCreationMillis) {
      // If the user did not exist for this billing month, piece of cake
      D("User did not exists before %s. Returning %s", new DateCalculator(userCreationMillis), zeroUserState)
      Just(zeroUserState)
    } else {
      resourceEventStore.countOutOfSyncEventsForBillingPeriod(userId, billingMonthStartMillis, billingMonthStopMillis) match {
        case Just(outOfSyncEventCount) ⇒
          // Have out of sync, so must recompute
          D("Found %s out of sync events, will have to (re)compute user state", outOfSyncEventCount)
          doCompute
        case NoVal ⇒
          // No out of sync events, ask DB cache
          userStateStore.findLatestUserStateForEndOfBillingMonth(userId, yearOfBillingMonth, billingMonth) match {
            case just @ Just(userState) ⇒
              // Found from cache
              D("Found from cache: %s", userState)
              just
            case NoVal ⇒
              // otherwise compute
              D("No user state found from cache, will have to (re)compute")
              doCompute
            case failed @ Failed(_, _) ⇒
              W("Failure while quering cache for user state: %s", failed)
              failed
          }
        case failed @ Failed(_, _) ⇒
          W("Failure while querying for out of sync events: %s", failed)
          failed
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
                           accounting: Accounting): Maybe[UserState] = Maybe {
    val previousBillingMonthUserStateM = findUserStateAtEndOfBillingMonth(
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
      accounting
    )
    
    previousBillingMonthUserStateM match {
      case NoVal ⇒
        NoVal // not really...
      case failed @ Failed(_, _) ⇒
        failed
      case Just(startingUserState) ⇒
        // This is the real deal

        val billingMonthStartDateCalc = new DateCalculator(yearOfBillingMonth, billingMonth)
        val billingMonthEndDateCalc   = billingMonthStartDateCalc.copy.goEndOfThisMonth
        val billingMonthStartMillis = billingMonthStartDateCalc.toMillis
        val billingMonthEndMillis  = billingMonthEndDateCalc.toMillis

        // Keep the working (current) user state. This will get updated as we proceed billing within the month
        var _workingUserState = startingUserState

        val allResourceEventsForMonth = resourceEventStore.findAllRelevantResourceEventsForBillingPeriod(
          userId,
          billingMonthStartMillis,
          billingMonthEndMillis)
    }

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

object DefaultUserStateComputations extends UserStateComputations