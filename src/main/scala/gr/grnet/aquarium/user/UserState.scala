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
import net.liftweb.json.{parse => parseJson, JsonAST, Xml}
import gr.grnet.aquarium.logic.accounting.dsl.DSLAgreement
import com.ckkloverdos.maybe.{Failed, Just, Maybe}


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
    active: Boolean,
    credits: CreditSnapshot,
    agreement: AgreementSnapshot,
    roles: RolesSnapshot,
    paymentOrders: PaymentOrdersSnapshot,
    ownedGroups: OwnedGroupsSnapshot,
    groupMemberships: GroupMembershipsSnapshot,
    ownedResources: OwnedResourcesSnapshot
) extends JsonSupport {

  private[this] def _allSnapshots: List[Long] = {
    List(
      credits.snapshotTime, agreement.snapshotTime, roles.snapshotTime,
      paymentOrders.snapshotTime, ownedGroups.snapshotTime, groupMemberships.snapshotTime,
      ownedResources.snapshotTime)
  }

  def oldestSnapshotTime: Long = _allSnapshots min

  def newestSnapshotTime: Long  = _allSnapshots max

  def maybeDSLAgreement: Maybe[DSLAgreement] = {
    agreement match {
      case snapshot @ AgreementSnapshot(data, _) ⇒
        data match {
          case dslAgreement @ DSLAgreement(_, _, _, _, _) ⇒
            Just(dslAgreement)
          case _ ⇒
            Failed(new Exception("No agreement found in snapshot %s for user %s".format(snapshot, userId)))
        }
      case _ ⇒
        Failed(new Exception("No agreement snapshot found for user %s".format(userId)))
    }
  }
  
  def safeCredits = credits match {
    case c @ CreditSnapshot(date, millis) ⇒ c
    case _ ⇒ CreditSnapshot(0, 0)
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
}