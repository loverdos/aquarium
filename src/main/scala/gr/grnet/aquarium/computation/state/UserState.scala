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

package gr.grnet.aquarium.computation.state

import gr.grnet.aquarium.converter.{JsonTextFormat, StdConverters}
import gr.grnet.aquarium.charging.wallet.WalletEntry
import gr.grnet.aquarium.util.json.JsonSupport
import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.computation.parts.RoleHistory
import gr.grnet.aquarium.computation.state.parts.{OwnedResourcesMap, ResourceInstanceAmount, OwnedResourcesSnapshot, AgreementHistory, ImplicitlyIssuedResourceEventsSnapshot, LatestResourceEventsSnapshot}
import gr.grnet.aquarium.policy.UserAgreementModel
import gr.grnet.aquarium.charging.reason.{InitialUserStateSetup, NoSpecificChargingReason, ChargingReason}

/**
 * A comprehensive representation of the User's state.
 *
 * Note that it is made of autonomous parts that are actually parts snapshots.
 *
 * The different snapshots need not agree on the snapshot time, ie. some state
 * part may be stale, while other may be fresh.
 *
 * The user state is meant to be partially updated according to relevant events landing on Aquarium.
 *
 * @define communicatedByIM
 *         This is communicated to Aquarium from the `IM` system.
 *
 *
 * @param userID
 * The user ID. $communicatedByIM
 * @param userCreationMillis
 * When the user was created.
 * $communicatedByIM
 * Set to zero if unknown.
 * @param stateChangeCounter
 * @param isFullBillingMonthState
 * @param theFullBillingMonth
 * @param implicitlyIssuedSnapshot
 * @param latestResourceEventsSnapshot
 * @param billingPeriodOutOfSyncResourceEventsCounter
 * @param agreementHistory
 * @param ownedResourcesSnapshot
 * @param newWalletEntries
 * The wallet entries computed. Not all user states need to holds wallet entries,
 * only those that refer to billing periods (end of billing period).
 * @param lastChangeReason
 * The [[gr.grnet.aquarium.charging.reason.ChargingReason]] for which the usr state has changed.
 * @param parentUserStateIDInStore
 * The `ID` of the parent state. The parent state is the one used as a reference point in order to calculate
 * this user state.
 * @param _id
 * The unique `ID` given by the store.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class UserState(
    isInitial: Boolean,
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
    theFullBillingMonth: Option[BillingMonthInfo],

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
     * The latest (previous) resource events per resource instance.
     */
    latestResourceEventsSnapshot: LatestResourceEventsSnapshot,

    /**
     * The out of sync events used to produce this user state for
     * the billing period recorded by `billingPeriodSnapshot`
     */
    billingPeriodOutOfSyncResourceEventsCounter: Long,

    totalCredits: Double,

    roleHistory: RoleHistory,

    agreementHistory: AgreementHistory,

    ownedResourcesSnapshot: OwnedResourcesSnapshot,

    newWalletEntries: List[WalletEntry],

    occurredMillis: Long, // When this user state was computed

    // The last known change reason for this userState
    lastChangeReason: ChargingReason = NoSpecificChargingReason(),
    // The user state we used to compute this one. Normally the (cached)
    // state at the beginning of the billing period.
    parentUserStateIDInStore: Option[String] = None,
    _id: String = null
) extends JsonSupport {

  def idInStore: Option[String] = _id match {
    case null ⇒ None
    case _id ⇒ Some(_id.toString)
  }

  //  def userCreationDate = new Date(userCreationMillis)
  //
  //  def userCreationFormatedDate = new MutableDateCalc(userCreationMillis).toString

  def findDSLAgreementForTime(at: Long): Option[UserAgreementModel] = {
    agreementHistory.findForTime(at)
  }

  def findResourceInstanceSnapshot(resource: String, instanceId: String): Option[ResourceInstanceAmount] = {
    ownedResourcesSnapshot.findResourceInstanceSnapshot(resource, instanceId)
  }

  def getResourceInstanceAmount(resource: String, instanceId: String, defaultValue: Double): Double = {
    ownedResourcesSnapshot.getResourceInstanceAmount(resource, instanceId, defaultValue)
  }

  def newWithResourcesSnapshotUpdate(resource: String, // resource name
                                     instanceId: String, // resource instance id
                                     newAmount: Double,
                                     snapshotTime: Long): UserState = {

    val (newResources, _, _) =
      ownedResourcesSnapshot.computeResourcesSnapshotUpdate(resource, instanceId, newAmount, snapshotTime)

    this.copy(
      isInitial = false,
      ownedResourcesSnapshot = newResources,
      stateChangeCounter = this.stateChangeCounter + 1
    )
  }

  def newWithChangeReason(changeReason: ChargingReason) = {
    this.copy(
      isInitial = false,
      lastChangeReason = changeReason,
      stateChangeCounter = this.stateChangeCounter + 1
    )
  }

  def newWithRoleHistory(newRoleHistory: RoleHistory, changeReason: ChargingReason) = {
    // FIXME: Also update agreement
    this.copy(
      isInitial = false,
      stateChangeCounter = this.stateChangeCounter + 1,
      roleHistory = newRoleHistory,
      lastChangeReason = changeReason
    )
  }

  def resourcesMap: OwnedResourcesMap = {
    ownedResourcesSnapshot.toResourcesMap
  }

  def findLatestResourceEvent: Option[ResourceEventModel] = {
    latestResourceEventsSnapshot.findTheLatest
  }

  def findLatestResourceEventID: Option[String] = {
    latestResourceEventsSnapshot.findTheLatestID
  }

  def isLatestResourceEventIDEqualTo(toCheckID: String) = {
    findLatestResourceEventID.map(_ == toCheckID).getOrElse(false)
  }

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
    final val userID = "userID"
    final val isFullBillingMonthState = "isFullBillingMonthState"
    final val occurredMillis = "occurredMillis"
    final val theFullBillingMonth_year  = "theFullBillingMonth.year"  // FQN
    final val theFullBillingMonth_month = "theFullBillingMonth.month" // FQN

    object theFullBillingMonth {
      final val year = "year"
      final val month = "month"
    }

  }

  def createInitialUserState(
      userID: String,
      userCreationMillis: Long,
      occurredMillis: Long,
      totalCredits: Double,
      initialAgreement: UserAgreementModel,
      calculationReason: ChargingReason = InitialUserStateSetup(None)
  ) = {

    UserState(
      true,
      userID,
      userCreationMillis,
      0L,
      false,
      None,
      ImplicitlyIssuedResourceEventsSnapshot.Empty,
      LatestResourceEventsSnapshot.Empty,
      0L,
      totalCredits,
      RoleHistory.initial(initialAgreement.role, userCreationMillis),
      AgreementHistory.initial(initialAgreement),
      OwnedResourcesSnapshot.Empty,
      Nil,
      occurredMillis,
      calculationReason
    )
  }

  def createInitialUserStateFromBootstrap(
      usb: UserStateBootstrap,
      occurredMillis: Long,
      calculationReason: ChargingReason
  ): UserState = {

    createInitialUserState(
      usb.userID,
      usb.userCreationMillis,
      occurredMillis,
      usb.initialCredits,
      usb.initialAgreement,
      calculationReason
    )
  }
}
