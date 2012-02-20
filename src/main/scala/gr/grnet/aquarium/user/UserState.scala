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

import gr.grnet.aquarium.util.json.{JsonHelpers, JsonSupport}
import net.liftweb.json.{JsonAST, Xml}
import gr.grnet.aquarium.logic.accounting.dsl.DSLAgreement
import com.ckkloverdos.maybe.{Failed, Maybe}
import gr.grnet.aquarium.logic.events.WalletEntry


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
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class UserState(
    userId: String,

    /**
     * When the user was created in the system (not Aquarium). We use this as a basis for billing periods. Set to
     * zero if unknown.
     * 
     */
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
    theFullBillingMonth: BillingMonth,

    /**
     * If this is a state for a full billing month, then keep here the implicit OFF
     * resource events.
     *
     * The use case is this: A VM may have been started (ON state) before the end of the billing period
     * and ended (OFF state) after the beginning of the next billing period. In order to bill this, we must assume
     * an implicit OFF even right at the end of the billing period and an implicit ON event with the beginning of the
     * next billing period.
     */
    implicitOFFsSnapshot: ImplicitOFFResourceEventsSnapshot,

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
     * The latest resource events per resource instance
     */
    latestResourceEventsSnapshot: LatestResourceEventsSnapshot,

    /**
     * Counts the number of resource events used to produce this user state for
     * the billing period recorded by `billingPeriodSnapshot`
     */
    resourceEventsCounter: Long,

    activeStateSnapshot: ActiveStateSnapshot,
    creditsSnapshot: CreditSnapshot,
    agreementsSnapshot: AgreementSnapshot,
    rolesSnapshot: RolesSnapshot,
    ownedResourcesSnapshot: OwnedResourcesSnapshot
) extends JsonSupport {

  private[this] def _allSnapshots: List[Long] = {
    List(
      activeStateSnapshot.snapshotTime,
      creditsSnapshot.snapshotTime, agreementsSnapshot.snapshotTime, rolesSnapshot.snapshotTime,
      ownedResourcesSnapshot.snapshotTime)
  }

  def oldestSnapshotTime: Long = _allSnapshots min

  def newestSnapshotTime: Long  = _allSnapshots max

//  def userCreationDate = new Date(userCreationMillis)
//
//  def userCreationFormatedDate = new DateCalculator(userCreationMillis).toString

  def maybeDSLAgreement(at: Long): Maybe[DSLAgreement] = {
    agreementsSnapshot match {
      case snapshot @ AgreementSnapshot(data, _) ⇒
        snapshot.getAgreement(at)
      case _ ⇒
       Failed(new Exception("No agreement snapshot found for user %s".format(userId)))
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

  def resourcesMap = ownedResourcesSnapshot.toResourcesMap
  
  def safeCredits = creditsSnapshot match {
    case c @ CreditSnapshot(_, _) ⇒ c
    case _ ⇒ CreditSnapshot(0.0, 0)
  }
}


object UserState {
  def fromJson(json: String): UserState = {
    JsonHelpers.jsonToObject[UserState](json)
  }

  def fromJValue(jsonAST: JsonAST.JValue): UserState = {
    JsonHelpers.jValueToObject[UserState](jsonAST)
  }

  def fromBytes(bytes: Array[Byte]): UserState = {
    JsonHelpers.jsonBytesToObject[UserState](bytes)
  }

  def fromXml(xml: String): UserState = {
    fromJValue(Xml.toJson(scala.xml.XML.loadString(xml)))
  }

  object JsonNames {
    final val _id = "_id"
    final val userId = "userId"
  }
}

case class BillingMonth(yearOfBillingMonth: Int, billingMonth: Int)