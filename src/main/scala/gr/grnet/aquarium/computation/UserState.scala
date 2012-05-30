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

package gr.grnet.aquarium.computation

import org.bson.types.ObjectId

import gr.grnet.aquarium.converter.{JsonTextFormat, StdConverters}
import gr.grnet.aquarium.event.model.NewWalletEntry
import gr.grnet.aquarium.util.json.JsonSupport
import gr.grnet.aquarium.logic.accounting.dsl.DSLAgreement
import gr.grnet.aquarium.computation.reason.{NoSpecificChangeReason, UserStateChangeReason, InitialUserStateSetup}
import gr.grnet.aquarium.computation.data.{RoleHistory, ResourceInstanceSnapshot, OwnedResourcesSnapshot, AgreementHistory, LatestResourceEventsSnapshot, ImplicitlyIssuedResourceEventsSnapshot}

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
 * @param latestResourceEventsSnapshot
 * @param billingPeriodOutOfSyncResourceEventsCounter
 * @param agreementHistory
 * @param ownedResourcesSnapshot
 * @param newWalletEntries
 *          The wallet entries computed. Not all user states need to holds wallet entries,
 *          only those that refer to billing periods (end of billing period).
  * @param lastChangeReason
 *          The [[gr.grnet.aquarium.computation.reason.UserStateChangeReason]] for which the usr state has changed.
 * @param parentUserStateId
 *          The `ID` of the parent state. The parent state is the one used as a reference point in order to calculate
 *          this user state.
 * @param _id
 *          The unique `ID` given by the store.
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

    newWalletEntries: List[NewWalletEntry],
    occurredMillis: Long, // The time fro which this state is relevant

    // The last known change reason for this userState
    lastChangeReason: UserStateChangeReason = NoSpecificChangeReason,
    // The user state we used to compute this one. Normally the (cached)
    // state at the beginning of the billing period.
    parentUserStateId: Option[String] = None,
    _id: String = new ObjectId().toString
) extends JsonSupport {

  def idOpt: Option[String] = _id match {
    case null ⇒ None
    case _id  ⇒ Some(_id.toString)
  }

//  def userCreationDate = new Date(userCreationMillis)
//
//  def userCreationFormatedDate = new MutableDateCalc(userCreationMillis).toString

  def findDSLAgreementForTime(at: Long): Option[DSLAgreement] = {
    agreementHistory.findForTime(at)
  }

  def findResourceInstanceSnapshot(resource: String, instanceId: String): Option[ResourceInstanceSnapshot] = {
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
    this.copy(lastChangeReason = changeReason)
  }

  def resourcesMap = ownedResourcesSnapshot.toResourcesMap

//  def modifyFromIMEvent(imEvent: IMEventModel, snapshotMillis: Long): UserState = {
//    this.copy(
//      isInitial = false,
//      imStateSnapshot = imStateSnapshot.addMostRecentEvent(imEvent),
//      lastChangeReason = IMEventArrival(imEvent),
//      occurredMillis = snapshotMillis
//    )
//  }

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
  }

  def createInitialUserState(userID: String,
                             userCreationMillis: Long,
                             totalCredits: Double,
                             initialRole: String,
                             initialAgreement: String) = {
    UserState(
      true,
      userID,
      userCreationMillis,
      0L,
      false,
      null,
      ImplicitlyIssuedResourceEventsSnapshot.Empty,
      LatestResourceEventsSnapshot.Empty,
      0L,
      totalCredits,
      RoleHistory.initial(initialRole, userCreationMillis),
      AgreementHistory.initial(initialAgreement, userCreationMillis),
      OwnedResourcesSnapshot.Empty,
      Nil,
      userCreationMillis,
      InitialUserStateSetup
    )
  }

  def createInitialUserState(usb: UserStateBootstrappingData): UserState = {
    createInitialUserState(
      usb.userID,
      usb.userCreationMillis,
      usb.initialCredits,
      usb.initialRole,
      usb.initialAgreement
    )
  }

  def createInitialUserStateFrom(us: UserState): UserState = {
    createInitialUserState(
      us.userID,
      us.userCreationMillis,
      us.totalCredits,
      us.roleHistory.firstRoleName.getOrElse("default"),          // FIXME What is the default?
      us.agreementHistory.firstAgreementName.getOrElse("default") // FIXME What is the default?
    )
  }
}
