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
import net.liftweb.json.{Extraction, parse => parseJson, JsonAST, Xml}
import gr.grnet.aquarium.logic.accounting.dsl.DSLResource


/**
 * A comprehensive representation of the User's state.
 *
 * Note that it is made of autonomous parts that are actually data snapshots.
 *
 * The different snapshots need not agree on the snapshot time, ie. some state
 * part may be stale, while other may be fresh.
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
  ownedResources: Map[DSLResource, Any /*ResourceState*/]) extends JsonSupport


object UserState {
  final object JsonNames {
    final val _id = "_id"
    final val userId = "userId"
  }

  def fromJson(json: String): UserState = {
    implicit val formats = JsonHelpers.DefaultJsonFormats
    val jsonAST = parseJson(json)
    Extraction.extract[UserState](jsonAST)
  }

  def fromJValue(jsonAST: JsonAST.JValue): UserState = {
    implicit val formats = JsonHelpers.DefaultJsonFormats
    Extraction.extract(jsonAST)
  }

  def fromBytes(bytes: Array[Byte]): UserState = {
    fromJson(new String(bytes, "UTF-8"))
  }

  def fromXml(xml: String): UserState = {
    fromJValue(Xml.toJson(scala.xml.XML.loadString(xml)))
  }
}