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

import gr.grnet.aquarium.store.ResourceEventStore
import com.ckkloverdos.maybe.{Failed, NoVal, Just, Maybe}
import gr.grnet.aquarium.logic.accounting.Accounting
import gr.grnet.aquarium.util.date.{TimeHelpers, DateCalculator}
import gr.grnet.aquarium.logic.accounting.dsl.{DSLResourcesMap, DSLCostPolicy, DSLPolicy, DSLAgreement}
import gr.grnet.aquarium.util.Loggable
import gr.grnet.aquarium.logic.events.{WalletEntry, ResourceEvent}

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
  def findUserPolicyAt(userId: String, whenMillis: Long): DSLPolicy
}

trait FullStateFinder {
  def findFullState(userId: String, whenMillis: Long): Any
}

trait UserStateCache {
  def findUserStateAtEndOfPeriod(userId: String, year: Int, month: Int): Maybe[UserState]

  /**
   * Find the most up-to-date user state for the particular billing period.
   */
  def findLatestUserStateForBillingMonth(userId: String, yearOfBillingMonth: Int, billingMonth: Int): Maybe[UserState]
}

/**
 * Use this to keep track of implicit OFFs at the end of the billing period.
 *
 * The use case is this: A VM may have been started (ON state) before the end of the billing period
 * and ended (OFF state) after the beginning of the next billing period. In order to bill this, we must assume
 * an implicit OFF even right at the end of the billing period and an implicit ON event with the beginning of the
 * next billing period.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 *
 * @param onEvents The `ON` events that need to be implicitly terminated.
 */
case class ImplicitOffEvents(onEvents: List[ResourceEvent])

case class OutOfSyncWalletEntries(entries: List[WalletEntry])

/**
 * Full user state at the end of a billing month.
 *
 * @param userState
 * @param implicitOffs
 */
case class EndOfBillingState(userState: UserState, implicitOffs: ImplicitOffEvents, outOfSyncWalletEntries: OutOfSyncWalletEntries)

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
      0L,
      ActiveSuspendedSnapshot(false, now),
      CreditSnapshot(0, now),
      AgreementSnapshot(Agreement(agreementName, now, -1) :: Nil, now),
      RolesSnapshot(List(), now),
      PaymentOrdersSnapshot(Nil, now),
      OwnedGroupsSnapshot(Nil, now),
      GroupMembershipsSnapshot(Nil, now),
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
        0L,
        ActiveSuspendedSnapshot(false, now),
        CreditSnapshot(0, now),
        AgreementSnapshot(Agreement(agreementName, now, - 1) :: Nil, now),
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
                                             knownUserState: UserState,
                                             accounting: Accounting): Maybe[EndOfBillingState] = {

    val billingDate = new DateCalculator(billingYear, billingMonth, 1)
    val billingDateMillis = billingDate.toMillis

//    if(billingDateMillis < knownUserState.startDateMillis) {
//      val userId = knownUserState.userId
//      val agreementName = knownUserState.agreement match {
//        case null      ⇒ "default"
//        case agreement ⇒ agreement.data
//      }
//      createFirstUserState(userId, agreementName)
//    } else {
      // We really need to compute the user state here

      // get all events that
      // FIXME: Implement
    Just(EndOfBillingState(knownUserState, ImplicitOffEvents(Nil), OutOfSyncWalletEntries(Nil)))

//    }
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

  def updatePreviousRCEventWith(previousRCEventsMap: mutable.Map[ResourceEvent.FullResourceType, ResourceEvent],
                                newRCEvent: ResourceEvent): Unit = {
    previousRCEventsMap(newRCEvent.fullResourceInfo) = newRCEvent
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
                                rcEventStore: ResourceEventStore,
                                currentUserState: UserState,
                                otherStuff: Traversable[Any],
                                defaultPolicy: DSLPolicy, // Policy.policy
                                defaultResourcesMap: DSLResourcesMap,
                                accounting: Accounting): Maybe[EndOfBillingState] = Maybe {

    val billingMonthStartDate = new DateCalculator(yearOfBillingMonth, billingMonth, 1)
    val billingMonthStopDate = billingMonthStartDate.copy.goEndOfThisMonth

    logger.debug("billingMonthStartDate = %s".format(billingMonthStartDate))
    logger.debug("billingMonthStopDate  = %s".format(billingMonthStopDate))

    val prevBillingMonthStartDate = billingMonthStartDate.copy.goPreviousMonth
    val yearOfPrevBillingMonth = prevBillingMonthStartDate.getYear
    val prevBillingMonth = prevBillingMonthStartDate.getMonthOfYear

    // Check if this value is already cached and valid, otherwise compute the value
    // TODO : cache it in case of new computation
    val cachedStartUserStateM = userStateCache.findLatestUserStateForBillingMonth(
      userId,
      yearOfPrevBillingMonth,
      prevBillingMonth)

    val (previousStartUserState, newStartUserState) = cachedStartUserStateM match {
      case Just(cachedStartUserState) ⇒
        // So, we do have a cached user state but must check if this is still valid
        logger.debug("Found cachedStartUserState = %s".format(cachedStartUserState))

        // Check how many resource events were used to produce this user state
        val cachedHowmanyRCEvents = cachedStartUserState.resourceEventsCounter

        // Ask resource event store to see if we had any "out of sync" events for the particular (== previous)
        // billing period.
        val prevHowmanyOutOfSyncRCEvents = rcEventStore.countOutOfSyncEventsForBillingMonth(
          userId,
          yearOfPrevBillingMonth,
          prevBillingMonth)
        logger.debug("prevHowmanyOutOfSyncRCEvents = %s".format(prevHowmanyOutOfSyncRCEvents))
        
        val recomputedStartUserState = if(prevHowmanyOutOfSyncRCEvents == 0) {
        logger.debug("Not necessary to recompute start user state, using cachedStartUserState")
          // This is good, there were no "out of sync" resource events, so we can use the cached value
          cachedStartUserState
        } else {
          // Oops, there are "out of sync" resource event. Must compute (potentially recursively)
          logger.debug("Recompute start user state...")
          val computedUserStateAtStartOfBillingPeriod = computeUserStateAtStartOfBillingPeriod(
            yearOfPrevBillingMonth,
            prevBillingMonth,
            cachedStartUserState,
            accounting)
          logger.debug("computedUserStateAtStartOfiingPeriodllB = %s".format(computedUserStateAtStartOfBillingPeriod))
          val recomputedStartUserState = computedUserStateAtStartOfBillingPeriod.asInstanceOf[Just[EndOfBillingState]].get.userState // FIXME
          logger.debug("recomputedStartUserState = %s".format(recomputedStartUserState))
          recomputedStartUserState
        }

        (cachedStartUserState, recomputedStartUserState)
      case NoVal ⇒
        // We do not even have a cached value, so compute one!
        logger.debug("Do not have a cachedStartUserState, computing one...")
        val computedUserStateAtStartOfBillingPeriod = computeUserStateAtStartOfBillingPeriod(
          yearOfPrevBillingMonth,
          prevBillingMonth,
          currentUserState,
          accounting)
        logger.debug("computedUserStateAtStartOfBillingPeriod = %s".format(computedUserStateAtStartOfBillingPeriod))
        val recomputedStartUserState = computedUserStateAtStartOfBillingPeriod.asInstanceOf[Just[EndOfBillingState]].get.userState // FIXME
        logger.debug("recomputedStartUserState = %s".format(recomputedStartUserState))

        (recomputedStartUserState, recomputedStartUserState)
      case Failed(e, m) ⇒
        logger.error("[Could not find latest user state for billing month %s-%s] %s".format(yearOfPrevBillingMonth, prevBillingMonth, m), e)
        throw new Exception(m, e)
    }

    // OK. Now that we have a user state to start with (= start of billing period reference point),
    // let us deal with the events themselves.
    val billingStartMillis = billingMonthStartDate.toMillis
    val billingStopMillis  = billingMonthStopDate.toMillis
    val allBillingPeriodRelevantRCEvents = rcEventStore.findAllRelevantResourceEventsForBillingPeriod(userId, billingStartMillis, billingStopMillis)
    logger.debug("allBillingPeriodRelevantRCEvents [%s] = %s".format(allBillingPeriodRelevantRCEvents.size, allBillingPeriodRelevantRCEvents))

    type FullResourceType = ResourceEvent.FullResourceType
    val previousRCEventsMap = mutable.Map[FullResourceType, ResourceEvent]()
    val impliedRCEventsMap  = mutable.Map[FullResourceType, ResourceEvent]() // those which do not exists but are
    // implied in order to do billing calculations (e.g. the "off" vmtime resource event)

    // Our temporary state holder.
    var _workingUserState = newStartUserState
    val nowMillis = TimeHelpers.nowMillis

    for(currentResourceEvent <- allBillingPeriodRelevantRCEvents) {
      val resource = currentResourceEvent.resource
      val instanceId = currentResourceEvent.instanceId

      logger.debug("Processing %s".format(currentResourceEvent.toDebugString(defaultResourcesMap, true)))
      // ResourCe events Debug
      // =     =         =
      def RCD(fmt: String, args: Any*) = logger.debug("  => " + fmt.format(args:_*))

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
      //
      // BUT ALL THE ABOVE SHOULD NOT BE CONSIDERED HERE; RATHER THEY ARE POLYMORPHIC BEHAVIOURS

      // What we need to do is:
      // A. Update user state with new resource instance amount
      // B. Update user state with new credit
      // C. Update ??? state with wallet entries

      // The DSLCostPolicy for the resource does not change, so it is safe to use the default DSLPolicy to obtain it.
      val costPolicyOpt = currentResourceEvent.findCostPolicy(defaultResourcesMap)
      costPolicyOpt match {
        case Some(costPolicy) ⇒
          RCD("Found costPolicy = %s".format(costPolicy))
          ///////////////////////////////////////
          // A. Update user state with new resource instance amount
          // TODO: Check if we are at beginning of billing period, so as to use
          //       costPolicy.computeResourceInstanceAmountForNewBillingPeriod
          val DefaultResourceInstanceAmount = costPolicy.getResourceInstanceInitialAmount
          RCD("DefaultResourceInstanceAmount = %s".format(DefaultResourceInstanceAmount))

          val previousAmount = currentUserState.getResourceInstanceAmount(resource, instanceId, DefaultResourceInstanceAmount)
          RCD("previousAmount = %s".format(previousAmount))
          val newAmount = costPolicy.computeNewResourceInstanceAmount(previousAmount, currentResourceEvent.value)
          RCD("newAmount = %s".format(newAmount))

          _workingUserState = _workingUserState.copyForResourcesSnapshotUpdate(resource, instanceId, newAmount, nowMillis)
          // A. Update user state with new resource instance amount
          ///////////////////////////////////////


          ///////////////////////////////////////
          // B. Update user state with new credit
          val previousRCEventM = findPreviousRCEventOf(currentResourceEvent, costPolicy, previousRCEventsMap)
          _workingUserState.findResourceInstanceSnapshot(resource, instanceId)
          // B. Update user state with new credit
          ///////////////////////////////////////


          ///////////////////////////////////////
          // C. Update ??? state with wallet entries

          // C. Update ??? state with wallet entries
          ///////////////////////////////////////

        case None ⇒
          () // ERROR
      }


      updatePreviousRCEventWith(previousRCEventsMap, currentResourceEvent)
    } // for(newResourceEvent <- allBillingPeriodRelevantRCEvents)


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
                               rcEventStore: ResourceEventStore,
                               currentUserState: UserState,
                               otherStuff: Traversable[Any],
                               accounting: Accounting): Maybe[UserState] = Maybe {
  

     null.asInstanceOf[UserState]
   }
}

object DefaultUserStateComputations extends UserStateComputations