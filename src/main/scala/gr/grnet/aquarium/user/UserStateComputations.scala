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

/**
 * The lowest (billing/charging) period is one week.
 * By convention, all (billing/charging) periods are multiples of a week.
 *
 * @param name is the name of the period and it must not contain "-"
 */
case class PeriodType(name: String, weeks: Int) {
  require(name ne null, "null name")
  require(name.indexOf('-') == -1, "name must not contain '-'")
  require(weeks > 0, "weeks must be positive")
  
  def isWeek  = weeks == 1
  def isMonth = weeks == 4
  def isMonthMultiple = (weeks % 4) == 0

  def representation = "%s-%s".format(name, weeks)

  def truncateToPeriod(millis: Long): Long = millis
  def truncateToPeriod(date: Date): Date   = new Date(truncateToPeriod(date.getTime))
}

object PeriodType {
  final val WeeklyBillingPeriod  = PeriodType("week",  1)
  final val WeeklyBillingPeriodRepr  = WeeklyBillingPeriod.representation
  final val MonthlyBillingPeriod = PeriodType("month", 4)
  final val MonthlyBillingPeriodRepr = MonthlyBillingPeriod.representation

  def fromRepresentation(bp: String): Maybe[PeriodType] = {
    bp match {
      case WeeklyBillingPeriodRepr ⇒
        Just(WeeklyBillingPeriod)
      case MonthlyBillingPeriodRepr ⇒
        Just(MonthlyBillingPeriod)
      case _ ⇒
        val dashIndex = bp.lastIndexOf('-')
        val name  = bp.substring(0, dashIndex)
        val weeks = bp.substring(dashIndex + 1).toInt

        Maybe(PeriodType(name, weeks))
    }
  }
}

//sealed trait AnyPeriod {
//  def startMillis: Long
//  def periodType: PeriodType
//}
//
//case class BillingPeriod(startMillis: Long, periodType: PeriodType) extends AnyPeriod
//case class ChargingPeriod(startMillis: Long, periodType: PeriodType) extends AnyPeriod

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

/**
 * - A billing run for a period is executed.
 * - The outcome of the billing run is a bill.
 * - A bill is made of charging statements for resources that have been used in several usage periods.
 *   For example, the bill of this month can contain
 *   - Charges for this month
 *   - Charges for previous months (which pricelist applies here, the past or the current one?)
 *
 * So, the are two kinds of periods:
 * - Billing periods
 * - Usage periods
 * These are counted in the same way, i.e. a weekly-
 */
case class BigDocs()

case class SingleEventChargingEntry(
    u: Double,
    du: Double,
    t: Long,
    dt: Long,
    resourceEventId: String,
    resourceEventOccurredMillis: Long, // caching some resourceEvent-oriented values here
    resourceEventReceivedMillis: Long,
    costPolicy: String,
    resource: String,
    instanceId: String)

case class MultipleEventsChargingEntry(
    u: Double,
    du: Double,
    t: Long,
    dt: Long,
    resourceEventIds: List[String],
    resourceEventOccurredMillis: List[Long], // caching some resourceEvent-oriented values here
    resourceEventReceivedMillis: List[Long],
    costPolicy: String,
    resource: String,
    instanceId: String)

/**
 * The full stage of a usage period
 */
case class UsagePeriodFullState(
    periodType: PeriodType,
    periodStart: Long, // (inclusive) truncated at start of period, this is not the time of the oldest event/charge
    periodStop: Long,  // (exclusive) truncated at end of period, this is not the time of the newest event/charge
    oldestMillis: Long,
    newestMillis: Long,
    singleEventEntries: List[SingleEventChargingEntry],
    multipleEventEntries: List[MultipleEventsChargingEntry]
    )

/**
 * The full state of a billing period may contain charges for several usage periods,
 * due to "out of bounds" events.
 */
case class BillingPeriodFullState(
    userId: String,
    periodType: PeriodType,
    periodStart: Long,
    periodEnd: Long,
    oldestMillis: Long,
    newestMillis: Long,
    usagePeriods: List[UsagePeriodFullState])

case class AccountingStatus(
    userId: String,
    foo: String)

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

  def doBillingForFullMonth( billingYear: Int,
                             billingMonth: Int,
                             userId: String,
                             policyFinder: UserPolicyFinder,
                             fullStateFinder: FullStateFinder,
                             userStateFinder: UserStateCache,
                             timeUnitInMillis: Long,
                             rcEventStore: ResourceEventStore,
                             otherStuff: Traversable[Any]): UserState = {

    val startBillingDate = new DateCalculator(billingYear, billingMonth, 1)
    val endBillingDate = startBillingDate.endOfThisMonth

    doBillingAtStartOfMonth_(
      billingYear,
      billingMonth,
      endBillingDate.toMillis,
      userId,
      policyFinder,
      fullStateFinder,
      userStateFinder,
      timeUnitInMillis,
      rcEventStore,
      otherStuff
    )
  }

  /**
   * Runs the billing algorithm on the specified period.
   * By default, a billing period is monthly.
   * The start of the billing period is midnight of the first day of the month we compute the bill for.
   *
   */
  def doBillingAtStartOfMonth_(startBillingYear: Int,
                              startBillingMonth: Int,
                              stopBillingMillis: Long,
                              userId: String,
                              policyFinder: UserPolicyFinder,
                              fullStateFinder: FullStateFinder,
                              userStateFinder: UserStateCache,
                              timeUnitInMillis: Long,
                              rcEventStore: ResourceEventStore,
                              otherStuff: Traversable[Any]): UserState = {

    // Start of month for which billing is calculated
    val startBillingDate = new DateCalculator(startBillingYear, startBillingMonth, 1)
    val startBillingMillis = startBillingDate.toMillis

    // Make sure the end date is within the month
    val stopBillingDate = new DateCalculator(stopBillingMillis)
    if(!startBillingDate.isSameYearAndMonthAs(stopBillingDate)) {
      throw new Exception("Cannot calc billing for dates (%s, %s) that are not in the same month".format(startBillingDate, stopBillingDate))
    }

    // We bill based on events *received* within the billing period.
    // These are all the events that were *received* (and not occurred) within the billing period.
    // Some of them, though, may refer to previous billing period(s)
    val allRCEvents = rcEventStore.findResourceEventsForReceivedPeriod(userId, startBillingMillis, stopBillingMillis)

    // Get:
    // a) Those resource events that have arrived within our billing period but refer to previous billing
    // periods (via their occurredMillis). We call these "out of sync" events.
    // b) Those events that have arrived within our billing period and refer to it as well
    val (prevBillingPeriodsOutOfSyncRCEvents, thisBillingPeriodRCEvents) = allRCEvents.partition(_.occurredMillis < startBillingMillis)

    // In order to start the billing for this period, we need a reference point from the previous billing period,
    // so as to get the initial values for the resources.
    // If we have no "out of sync" resource events, then we are set.
    // Otherwise, we need to do some recalculation for the previous billing periods
    val startUserState: UserState = if(prevBillingPeriodsOutOfSyncRCEvents.size == 0) {
      // No "out-of-sync" resource events.
      // We just need to query for the calculated user state at the end of the previous billing period
      // (or calculate it now if it has not been cached)

      val previousBillingMonthStartDate = startBillingDate.previousMonth
      val yearOfPreviousBillingMonthStartDate = previousBillingMonthStartDate.year
      val monthOfPreviousBillingMonthStartDate = previousBillingMonthStartDate.monthOfYear
      val previousUserStateM = userStateFinder.findUserStateAtEndOfPeriod(userId, yearOfPreviousBillingMonthStartDate, monthOfPreviousBillingMonthStartDate)

      previousUserStateM match {
        case Just(previousUserState) ⇒
          previousUserState

        case NoVal ⇒
          // We need to compute the user state for previous billing period.
          // This could go on recursively until end of time
          // ==> FIXME What is the recursion end condition????
          //     FIXME : Probably the date the user entered the system
          doBillingForFullMonth(
            yearOfPreviousBillingMonthStartDate,
            monthOfPreviousBillingMonthStartDate,
            userId,
            policyFinder,
            fullStateFinder,
            userStateFinder,
            timeUnitInMillis,
            rcEventStore,
            Traversable())

        case Failed(e, m) ⇒
          throw new Exception(m, e)
      }
    } else {
      // OK. We have some "out of sync" resource events that will lead to a new state for the previous billing period
      // calculation.

      // previous billing month
      val previousBillingMonthStartDate = startBillingDate.previousMonth

      null
    }


    null.asInstanceOf[UserState]
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
  
     // Start of month for which billing is calculated
     val startBillingDate = new DateCalculator(startBillingYear, startBillingMonth, 1)
     val startBillingMillis = startBillingDate.toMillis

     // Make sure the end date is within the month
     val stopBillingDate = new DateCalculator(stopBillingMillis)
     if(!startBillingDate.isSameYearAndMonthAs(stopBillingDate)) {
       throw new Exception("Cannot calc billing for dates (%s, %s) that are not in the same month".format(startBillingDate, stopBillingDate))
     }

     // Get the user state at the end of the previous billing period
     val previousBillingMonthStartDate = startBillingDate.previousMonth
     val yearOfPreviousBillingMonthStartDate = previousBillingMonthStartDate.year
     val monthOfPreviousBillingMonthStartDate = previousBillingMonthStartDate.monthOfYear
     val previousUserState = computeUserStateAtStartOfBillingPeriod(
       yearOfPreviousBillingMonthStartDate,
       monthOfPreviousBillingMonthStartDate,
       currentUserState)

     // Get all relevant events.
     

     null.asInstanceOf[UserState]
   }
}