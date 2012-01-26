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

import gr.grnet.aquarium.store.ResourceEventStore
import java.util.Date
import gr.grnet.aquarium.util.date.DateCalculator
import com.ckkloverdos.maybe.{Failed, NoVal, Just, Maybe}

sealed abstract class CalculationType(_name: String) {
  def name = _name
}

/**
 * Normal calculations that are part of the bill generation procedure
 */
case object PeriodicCalculation extends CalculationType("periodic")

/**
 * Adhoc calculations, e.g. when computing the state in realtime.
 */
case object AdhocCalculation extends CalculationType("adhoc")

trait UserPolicyFinder {
  def findUserPolicyAt(userId: String, whenMillis: Long)
}

trait FullStateFinder {
  def findFullState(userId: String, whenMillis: Long): Any
}

trait UserStateCache {
  def findUserStateAtEndOfPeriod(userId: String, year: Int, month: Int): Maybe[UserState]
  
  def findLatestUserStateForBillingPeriod(userId: String, yearOfBillingMonth: Int, billingMonth: Int): Maybe[UserState]
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object UserStateComputations {
  def createFirstUserState(userId: String, agreementName: String) = {
    val now = 0L
    UserState(
      userId,
      now,
      0L,
      false,
      null,
      0L,
      ActiveSuspendedSnapshot(false, now),
      CreditSnapshot(0, now),
      AgreementSnapshot(agreementName, now),
      RolesSnapshot(List(), now),
      PaymentOrdersSnapshot(Nil, now),
      OwnedGroupsSnapshot(Nil, now),
      GroupMembershipsSnapshot(Nil, now),
      OwnedResourcesSnapshot(List(), now)
    )
  }

  /**
   * Get the user state as computed up to (and not including) the start of the new billing period.
   *
   * Always compute, taking into account any "out of sync" resource events
   */
  def computeUserStateAtStartOfBillingPeriod(billingYear: Int,
                                             billingMonth: Int,
                                             knownUserState: UserState): UserState = {

    val billingDate = new DateCalculator(billingYear, billingMonth, 1)
    val billingDateMillis = billingDate.toMillis

    if(billingDateMillis < knownUserState.startDateMillis) {
      val userId = knownUserState.userId
      val agreementName = knownUserState.agreement match {
        case null      ⇒ "default"
        case agreement ⇒ agreement.data
      }
      createFirstUserState(userId, agreementName)
    } else {
      // We really need to compute the user state here

      // get all events that
      // FIXME: Implement
      knownUserState
    }
  }


  /**
   * Do a full month billing.
   *
   * Takes into account "out of sync events".
   * 
   */
  def computeFullMonthlyBilling(yearOfBillingMonth: Int,
                                billingMonth: Int,
                                userId: String,
                                policyFinder: UserPolicyFinder,
                                fullStateFinder: FullStateFinder,
                                userStateCache: UserStateCache,
                                timeUnitInMillis: Long,
                                rcEventStore: ResourceEventStore,
                                currentUserState: UserState,
                                otherStuff: Traversable[Any]): Maybe[UserState] = Maybe {

    val billingMonthStartDate = new DateCalculator(yearOfBillingMonth, billingMonth, 1)
    val prevBillingMonthStartDate = billingMonthStartDate.previousMonth
    val yearOfPrevBillingMonth = prevBillingMonthStartDate.year
    val prevBillingMonth = prevBillingMonthStartDate.monthOfYear

    // Check if this value is already cached and valid, otherwise compute the value
    // TODO : cache it in case of new computation
    val cachedStartUserStateM = userStateCache.findLatestUserStateForBillingPeriod(userId, yearOfPrevBillingMonth, prevBillingMonth)

    val (previousStartUserState, newStartUserState) = cachedStartUserStateM match {
      case Just(cachedStartUserState) ⇒
        // So, we do have a cached user state but must check if this is still valid

        // Check how many resource events were used to produce this user state
        val cachedHowmanyRCEvents = cachedStartUserState.resourceEventsCounter

        // Ask resource event store to see if we had any "out of sync" events for the particular (== previous)
        // billing period.
        val prevHowmanyOutOfSyncRCEvents = rcEventStore.countOutOfSyncEventsForBillingMonth(
          userId,
          yearOfPrevBillingMonth,
          prevBillingMonth)
        
        val recomputedStartUserState = if(prevHowmanyOutOfSyncRCEvents == 0) {
          // This is good, can return the cached value
          cachedStartUserState
        } else {
          // "Out of sync" resource events means re-computation
          computeUserStateAtStartOfBillingPeriod(yearOfPrevBillingMonth, prevBillingMonth, cachedStartUserState)
        }

        (cachedStartUserState, recomputedStartUserState)
      case NoVal ⇒
        // We do not even have a cached value, so perform re-computation
        val recomputedStartUserState = computeUserStateAtStartOfBillingPeriod(yearOfPrevBillingMonth, prevBillingMonth, currentUserState)
        (recomputedStartUserState, recomputedStartUserState)
      case Failed(e, m) ⇒
        throw new Exception(m, e)
    }

    // OK. Now that we have a user state to start with (= start of billing period reference point),
    // let us deal with the events themselves.
    val billingStartMillis = billingMonthStartDate.toMillis
    val billingStopMillis = billingMonthStartDate.endOfThisMonth.toMillis
    val allBillingPeriodRelevantRCEvents = rcEventStore.findAllRelevantResourceEventsForBillingPeriod(userId, billingStartMillis, billingStopMillis)

    type ResourceType = String
    type ResourceInstanceType = String
    val prevRCEventMap: scala.collection.mutable.Map[(ResourceType, ResourceInstanceType), Double] = scala.collection.mutable.Map()
    var workingUserState = newStartUserState

    for(currentRCEvent <- allBillingPeriodRelevantRCEvents) {
      // We need to do these kinds of calculations:
      // 1. Credit state calculations
      // 2. Resource state calculations

      // How credits are computed:
      // - "onoff" events (think "vmtime"):
      //   - need to be considered in on/off pairs
      //   - just use the time difference of this event to the previous one for the credit computation
      // - "discrete" events (think "bandwidth"):
      //   - just use their value, which is a difference already for the credit computation
      // - "continuous" events (think "bandwidth"):
      //   - need the previous absolute value
      //   - need the time difference of this event to the previous one
      //   - use both the above (previous absolute value, time difference) for the credit computation

    }


    null
  }


  /**
  * Runs the billing algorithm on the specified period.
  * By default, a billing period is monthly.
  * The start of the billing period is midnight of the first day of the month we compute the bill for.
  *
  */
   def doPartialMonthlyBilling(startBillingYear: Int,
                               startBillingMonth: Int,
                               stopBillingMillis: Long,
                               userId: String,
                               policyFinder: UserPolicyFinder,
                               fullStateFinder: FullStateFinder,
                               userStateFinder: UserStateCache,
                               timeUnitInMillis: Long,
                               rcEventStore: ResourceEventStore,
                               currentUserState: UserState,
                               otherStuff: Traversable[Any]): Maybe[UserState] = Maybe {
  

     null.asInstanceOf[UserState]
   }
}