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

package gr.grnet.aquarium.message.avro

import gr.grnet.aquarium.charging.state.UserAgreementHistoryModel
import gr.grnet.aquarium.charging.state.UserStateModel
import gr.grnet.aquarium.message.avro.gen._
import gr.grnet.aquarium.policy._
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.mapAsScalaMapConverter
import gr.grnet.aquarium.policy.ResourceType
import gr.grnet.aquarium.policy.PolicyModel
import gr.grnet.aquarium.charging.state.UserAgreementHistoryModel
import gr.grnet.aquarium.policy.EffectivePriceTableModel
import gr.grnet.aquarium.policy.CronSpec
import gr.grnet.aquarium.policy.UserAgreementModel
import gr.grnet.aquarium.policy.AdHocFullPriceTableRef
import gr.grnet.aquarium.charging.state.UserStateModel
import gr.grnet.aquarium.policy.EffectiveUnitPriceModel

/**
 * Provides helper methods that construct model objects, usually from their avro message counterparts.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object ModelFactory {
  def newOptionString(in: String): Option[String] = {
    in match {
      case null ⇒ None
      case some ⇒ Some(some)
    }
  }

  //  def scalaModelOfAnyValueMsg(msg: AnyValueMsg): Any = {
  //    def convert(x: Any): Any = x match {
  //      case null ⇒ null
  //      case int: Int ⇒ int
  //      case long: Long ⇒ long
  //      case boolean: Boolean ⇒ boolean
  //      case char: Char ⇒ char
  //      case charSeq: CharSequence ⇒ charSeq.toString
  //      case juList: ju.List[_] ⇒ juList.asScala.map(convert).toList
  //      case juMap: ju.Map[_, _] ⇒ juMap.asScala.map { case (k, v) ⇒ (convert(k), convert(v))}.toMap
  //      case default ⇒ default
  //    }
  //
  //    convert(msg.getAnyValue)
  //  }

  def newResourceType(msg: ResourceTypeMsg) = {
    ResourceType(
      msg.getName(),
      msg.getUnit(),
      msg.getChargingBehaviorClass()
    )
  }

  def newEffectiveUnitPrice(msg: EffectiveUnitPriceMsg) = {
    EffectiveUnitPriceModel(
      unitPrice = msg.getUnitPrice(),
      when = msg.getWhen() match {
        case null ⇒
          None

        case cronSpecTupleMsg ⇒
          Some(
            CronSpec(cronSpecTupleMsg.getA),
            CronSpec(cronSpecTupleMsg.getB)
          )
      }
    )
  }

  def newEffectivePriceTable(msg: EffectivePriceTableMsg) = {
    EffectivePriceTableModel(
      priceOverrides = msg.getPriceOverrides.asScala.map(newEffectiveUnitPrice).toList
    )
  }

  def newSelectorValue(v: SelectorValueMsg): Any /* either a selector (String) or a map */ = {
    v match {
      case effectivePriceTableMsg: EffectivePriceTableMsg ⇒
        newEffectivePriceTable(effectivePriceTableMsg)

      case mapOfSelectorValueMsg: java.util.Map[_, _] ⇒
        mapOfSelectorValueMsg.asScala.map {
          case (k, v) ⇒
            (k.toString, newSelectorValue(v.asInstanceOf[SelectorValueMsg]))
        }.toMap // make immutable map
    }
  }

  def newFullPriceTable(msg: FullPriceTableMsg) = {
    FullPriceTableModel(msg)
  }

  def newRoleMapping(
      roleMapping: java.util.Map[String, FullPriceTableMsg]
  ): Map[String, FullPriceTableModel] = {

    roleMapping.asScala.map {
      case (k, v) ⇒
        val k2 = k.toString
        val v2 = newFullPriceTable(v)

        (k2, v2)
    }.toMap
  }

  def newPolicyModel(msg: PolicyMsg) = PolicyModel(msg)

  def newUserAgreementModelFromIMEvent(
      imEvent: IMEventMsg,
      id: String = MessageHelpers.UserAgreementMsgIDGenerator.nextUID()
  ) = {

    UserAgreementModel(
      MessageFactory.newUserAgreementFromIMEventMsg(imEvent, id),
      PolicyDefinedFullPriceTableRef
    )
  }

  def newUserAgreementModelFromIMEventMsg(
      imEvent: IMEventMsg,
      fullPriceTableRef: FullPriceTableRef,
      id: String = MessageHelpers.UserAgreementMsgIDGenerator.nextUID()
  ): UserAgreementModel = {
    UserAgreementModel(
      MessageFactory.newUserAgreementFromIMEventMsg(imEvent, id),
      fullPriceTableRef
    )
  }

  def newUserAgreementModel(msg: UserAgreementMsg): UserAgreementModel = {
    UserAgreementModel(
      msg,
      msg.getFullPriceTableRef match {
        case null ⇒
          PolicyDefinedFullPriceTableRef

        case fullPriceTableMsg ⇒
          AdHocFullPriceTableRef(newFullPriceTable(fullPriceTableMsg))
      }
    )
  }

  def newUserAgreementHistoryModel(msg: UserAgreementHistoryMsg): UserAgreementHistoryModel = {
    UserAgreementHistoryModel(msg)
  }

  def newUserStateModel(msg: UserStateMsg): UserStateModel = {
    UserStateModel(
      msg,
      newUserAgreementHistoryModel(msg.getAgreementHistory)
    )
  }
}
