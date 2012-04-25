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

import gr.grnet.aquarium.util.json.JsonSupport
import gr.grnet.aquarium.logic.accounting.dsl.DSLAgreement
import com.ckkloverdos.maybe.{Failed, Maybe}
import gr.grnet.aquarium.util.date.MutableDateCalc
import gr.grnet.aquarium.event.{NewWalletEntry, WalletEntry}
import gr.grnet.aquarium.converter.{JsonTextFormat, StdConverters}
import gr.grnet.aquarium.AquariumException
import gr.grnet.aquarium.event.im.IMEventModel


/**
 * A comprehensive representation of the User's state.
 *
 * Note that it is made of autonomous parts that are actually data snapshots.
 *
 * The different snapshots need not agree on the snapshot time, ie. some state
 * part may be stale, while other may be fresh.
 *
 * The user state is meant to be partially updated according to relevant events landing on Aquarium.
 *
 * @define communicatedByIM
 *          This is communicated to Aquarium from the `IM` system.
 *
 *
 * @param userID
 *          The user ID. $communicatedByIM
 * @param userCreationMillis
 *          When the user was created.
 *          $communicatedByIM
 *          Set to zero if unknown.
 * @param stateChangeCounter
 * @param isFullBillingMonthState
 * @param theFullBillingMonth
 * @param implicitlyIssuedSnapshot
 * @param billingMonthWalletEntries
 * @param outOfSyncWalletEntries
 * @param latestResourceEventsSnapshot
 * @param billingPeriodResourceEventsCounter
 * @param billingPeriodOutOfSyncResourceEventsCounter
 * @param activeStateSnapshot
 * @param creditsSnapshot
 * @param agreementsSnapshot
 * @param rolesSnapshot
 * @param ownedResourcesSnapshot
 * @param newWalletEntries
 *          The wallet entries computed. Not all user states need to holds wallet entries,
 *          only those that refer to billing periods (end of billing period).
 * @param lastChangeReasonCode
 *          The code for the `lastChangeReason`.
 * @param lastChangeReason
 *          The [[gr.grnet.aquarium.user.UserStateChangeReason]] for which the usr state has changed.
 * @param totalEventsProcessedCounter
 * @param parentUserStateId
 *          The `ID` of the parent state. The parent state is the one used as a reference point in order to calculate
 *          this user state.
 * @param _id
 *          The unique `ID` given by the store.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class UserState(
    userID: String,

    userCreationMillis: Long,

    /**
     * Each time the user state is updated, this must be increased.
     * The counter is used when accessing user state from the cache (user state store)
     * in order to get the latest value for a particular billing period.
     */
    stateChangeCounter: Long,

    /**
     * True iff this user state refers to a full billing period, that is a full billing month.
     */
    isFullBillingMonthState: Boolean,

    /**
     * The full billing period for which this user state refers to.
     * This is set when the user state refers to a full billing period (= month)
     * and is used to cache the user state for subsequent queries.
     */
    theFullBillingMonth: BillingMonthInfo,

    /**
     * If this is a state for a full billing month, then keep here the implicit OFF
     * resource events or any other whose cost policy demands an implicit event at the end of the billing period.
     *
     * The use case is this: A VM may have been started (ON state) before the end of the billing period
     * and ended (OFF state) after the beginning of the next billing period. In order to bill this, we must assume
     * an implicit OFF even right at the end of the billing period and an implicit ON event with the beginning of the
     * next billing period.
     */
    implicitlyIssuedSnapshot: ImplicitlyIssuedResourceEventsSnapshot,

    /**
     * So far computed wallet entries for the current billing month.
     */
    billingMonthWalletEntries: List[WalletEntry],

    /**
     * Wallet entries that were computed for out of sync events.
     * (for the current billing month ??)
     */
    outOfSyncWalletEntries: List[WalletEntry],

    /**
     * The latest (previous) resource events per resource instance.
     */
    latestResourceEventsSnapshot: LatestResourceEventsSnapshot,

    /**
     * Counts the total number of resource events used to produce this user state for
     * the billing period recorded by `billingPeriodSnapshot`
     */
    billingPeriodResourceEventsCounter: Long,

    /**
     * The out of sync events used to produce this user state for
     * the billing period recorded by `billingPeriodSnapshot`
     */
    billingPeriodOutOfSyncResourceEventsCounter: Long,

    activeStateSnapshot: ActiveStateSnapshot,
    creditsSnapshot: CreditSnapshot,
    agreementsSnapshot: AgreementSnapshot,
    rolesSnapshot: RolesSnapshot,
    ownedResourcesSnapshot: OwnedResourcesSnapshot,
    newWalletEntries: List[NewWalletEntry],
    lastChangeReasonCode: UserStateChangeReasonCodes.ChangeReasonCode,
    // The last known change reason for this userState
    lastChangeReason: UserStateChangeReason = NoSpecificChangeReason,
    totalEventsProcessedCounter: Long = 0L,
    // The user state we used to compute this one. Normally the (cached)
    // state at the beginning of the billing period.
    parentUserStateId: Option[String] = None,
    id: String = ""
) extends JsonSupport {

  private[this] def _allSnapshots: List[Long] = {
    List(
      activeStateSnapshot.snapshotTime,
      creditsSnapshot.snapshotTime, agreementsSnapshot.snapshotTime, rolesSnapshot.snapshotTime,
      ownedResourcesSnapshot.snapshotTime,
      implicitlyIssuedSnapshot.snapshotTime,
      latestResourceEventsSnapshot.snapshotTime
    )
  }

  def oldestSnapshotTime: Long = _allSnapshots min

  def newestSnapshotTime: Long  = _allSnapshots max

  def _id = id
  def idOpt: Option[String] = _id match {
    case null ⇒ None
    case ""   ⇒ None
    case _id  ⇒ Some(_id)
  }

//  def userCreationDate = new Date(userCreationMillis)
//
//  def userCreationFormatedDate = new MutableDateCalc(userCreationMillis).toString

  def maybeDSLAgreement(at: Long): Maybe[DSLAgreement] = {
    agreementsSnapshot match {
      case snapshot @ AgreementSnapshot(data, _) ⇒
        snapshot.getAgreement(at)
      case _ ⇒
       Failed(new AquariumException("No agreement snapshot found for user %s".format(userID)))
    }
  }

  def findResourceInstanceSnapshot(resource: String, instanceId: String): Maybe[ResourceInstanceSnapshot] = {
    ownedResourcesSnapshot.findResourceInstanceSnapshot(resource, instanceId)
  }

  def getResourceInstanceAmount(resource: String, instanceId: String, defaultValue: Double): Double = {
    ownedResourcesSnapshot.getResourceInstanceAmount(resource, instanceId, defaultValue)
  }

  def copyForResourcesSnapshotUpdate(resource: String,   // resource name
                                     instanceId: String, // resource instance id
                                     newAmount: Double,
                                     snapshotTime: Long): UserState = {

    val (newResources, _, _) = ownedResourcesSnapshot.computeResourcesSnapshotUpdate(resource, instanceId, newAmount, snapshotTime)

    this.copy(
      ownedResourcesSnapshot = newResources,
      stateChangeCounter = this.stateChangeCounter + 1)
  }
  
  def copyForChangeReason(changeReason: UserStateChangeReason) = {
    this.copy(lastChangeReasonCode = changeReason.code, lastChangeReason = changeReason)
  }

  def resourcesMap = ownedResourcesSnapshot.toResourcesMap

//  def toShortString = "UserState(%s, %s, %s, %s, %s)".format(
//    userId,
//    _id,
//    parentUserStateId,
//    totalEventsProcessedCounter,
//    calculationReason)
}


object UserState {
  def fromJson(json: String): UserState = {
    StdConverters.AllConverters.convertEx[UserState](JsonTextFormat(json))
  }

  object JsonNames {
    final val _id = "_id"
    final val userId = "userId"
  }
}

final class BillingMonthInfo private(val year: Int,
                                     val month: Int,
                                     val startMillis: Long,
                                     val stopMillis: Long) extends Ordered[BillingMonthInfo] {

  def previousMonth: BillingMonthInfo = {
    BillingMonthInfo.fromDateCalc(new MutableDateCalc(year, month).goPreviousMonth)
  }

  def nextMonth: BillingMonthInfo = {
    BillingMonthInfo.fromDateCalc(new MutableDateCalc(year, month).goNextMonth)
  }


  def compare(that: BillingMonthInfo) = {
    val ds = this.startMillis - that.startMillis
    if(ds < 0) -1 else if(ds == 0) 0 else 1
  }


  override def equals(any: Any) = any match {
    case that: BillingMonthInfo ⇒
      this.year == that.year && this.month == that.month // normally everything else MUST be the same by construction
    case _ ⇒
      false
  }

  override def hashCode() = {
    31 * year + month
  }

  override def toString = "%s-%02d".format(year, month)
}

object BillingMonthInfo {
  def fromMillis(millis: Long): BillingMonthInfo = {
    fromDateCalc(new MutableDateCalc(millis))
  }

  def fromDateCalc(mdc: MutableDateCalc): BillingMonthInfo = {
    val year = mdc.getYear
    val month = mdc.getMonthOfYear
    val startMillis = mdc.goStartOfThisMonth.getMillis
    val stopMillis  = mdc.goEndOfThisMonth.getMillis // no need to `copy` here, since we are discarding `mdc`

    new BillingMonthInfo(year, month, startMillis, stopMillis)
  }
}

sealed trait UserStateChangeReason {
  /**
   * Return `true` if the result of the calculation should be stored back to the
   * [[gr.grnet.aquarium.store.UserStateStore]].
   *
   */
  def shouldStoreUserState: Boolean

  def shouldStoreCalculatedWalletEntries: Boolean

  def forPreviousBillingMonth: UserStateChangeReason

  def calculateCreditsForImplicitlyTerminated: Boolean

  def code: UserStateChangeReasonCodes.ChangeReasonCode
}

object UserStateChangeReasonCodes {
  type ChangeReasonCode = Int

  final val InitialCalculationCode = 1
  final val NoSpecificChangeCode   = 2
  final val MonthlyBillingCode     = 3
  final val RealtimeBillingCode    = 4
  final val IMEventArrivalCode   = 5
}

case object InitialUserStateCalculation extends UserStateChangeReason {
  def shouldStoreUserState = true

  def shouldStoreCalculatedWalletEntries = false

  def forPreviousBillingMonth = this

  def calculateCreditsForImplicitlyTerminated = false

  def code = UserStateChangeReasonCodes.InitialCalculationCode
}
/**
 * A calculation made for no specific reason. Can be for testing, for example.
 *
 */
case object NoSpecificChangeReason extends UserStateChangeReason {
  def shouldStoreUserState = false

  def shouldStoreCalculatedWalletEntries = false

  def forBillingMonthInfo(bmi: BillingMonthInfo) = this

  def forPreviousBillingMonth = this

  def calculateCreditsForImplicitlyTerminated = false

  def code = UserStateChangeReasonCodes.NoSpecificChangeCode
}

/**
 * An authoritative calculation for the billing period.
 *
 * This marks a state for caching.
 *
 * @param billingMonthInfo
 */
case class MonthlyBillingCalculation(billingMonthInfo: BillingMonthInfo) extends UserStateChangeReason {
  def shouldStoreUserState = true

  def shouldStoreCalculatedWalletEntries = true

  def forPreviousBillingMonth = MonthlyBillingCalculation(billingMonthInfo.previousMonth)

  def calculateCreditsForImplicitlyTerminated = true

  def code = UserStateChangeReasonCodes.MonthlyBillingCode
}

/**
 * Used for the realtime billing calculation.
 *
 * @param forWhenMillis The time this calculation is for
 */
case class RealtimeBillingCalculation(forWhenMillis: Long) extends UserStateChangeReason {
  def shouldStoreUserState = false

  def shouldStoreCalculatedWalletEntries = false

  def forPreviousBillingMonth = this

  def calculateCreditsForImplicitlyTerminated = false

  def code = UserStateChangeReasonCodes.RealtimeBillingCode
}

case class IMEventArrival(imEvent: IMEventModel) extends UserStateChangeReason {
  def shouldStoreUserState = true

  def shouldStoreCalculatedWalletEntries = false

  def forPreviousBillingMonth = this

  def calculateCreditsForImplicitlyTerminated = false

  def code = UserStateChangeReasonCodes.IMEventArrivalCode
}
