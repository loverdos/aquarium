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

import gr.grnet.aquarium.message.avro.gen.{EffectiveUnitPriceMsg, SelectorValueMsg, EffectivePriceTableMsg, FullPriceTableMsg, ResourceTypeMsg, PolicyMsg}
import gr.grnet.aquarium.policy.{CronSpec, EffectiveUnitPrice, EffectivePriceTable, FullPriceTable, ResourceType, PolicyModel}
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.mutable

/**
 * Provides helper methods that construct model objects, usually from their avro message counterparts.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object ModelFactory {
  def newOptionString(in: CharSequence): Option[String] = {
    in match {
      case null ⇒ None
      case some ⇒ Some(in.toString)
    }
  }

  def newResourceType(msg: ResourceTypeMsg) = {
    ResourceType(
      msg.getName().toString,
      msg.getUnit().toString,
      msg.getChargingBehaviorClass().toString
    )
  }

  def newEffectiveUnitPrice(msg: EffectiveUnitPriceMsg) = {
    EffectiveUnitPrice(
      unitPrice = msg.getUnitPrice(),
      when = msg.getWhen() match {
        case null ⇒
          None

        case cronSpecTupleMsg ⇒
          Some(
            CronSpec(cronSpecTupleMsg.getA.toString),
            CronSpec(cronSpecTupleMsg.getB.toString)
          )
      }
    )
  }

  def newEffectivePriceTable(msg: EffectivePriceTableMsg) = {
    EffectivePriceTable(
      priceOverrides = msg.getPriceOverrides.asScala.map(newEffectiveUnitPrice).toList
    )
  }

  def newSelectorValue(v: SelectorValueMsg): Any /* either a selector (String) or a map */ = {
    v match {
      case effectivePriceTableMsg: EffectivePriceTableMsg ⇒
        newEffectivePriceTable(effectivePriceTableMsg)

      case mapOfSelectorValueMsg: java.util.Map[_, _] ⇒
        mapOfSelectorValueMsg.asScala.map { case (k, v) ⇒
          (k.toString, newSelectorValue(v.asInstanceOf[SelectorValueMsg]))
        }.toMap // make immutable map
    }
  }

  def newFullPriceTable(msg: FullPriceTableMsg) = {
    FullPriceTable(
      perResource = Map(msg.getPerResource().asScala.map { case (k, v) ⇒
        val k2 = k.toString
        val v2 = v.asInstanceOf[java.util.Map[CharSequence, SelectorValueMsg]].asScala.map { case (k, v) ⇒
          (k.toString, newSelectorValue(v))
        }.toMap // make immutable map

        (k2, v2)
      }.toSeq: _*)
    )
  }

  def newRoleMapping(
      roleMapping: java.util.Map[CharSequence, FullPriceTableMsg]
  ): mutable.Map[String, FullPriceTable] = {

    roleMapping.asScala.map { case (k, v) ⇒
      val k2 = k.toString
      val v2 = newFullPriceTable(v)

      (k2, v2)
    }
  }

  def newPolicyModel(msg: PolicyMsg) = {
    PolicyModel(
      originalID = msg.getOriginalID().toString,
      inStoreID = newOptionString(msg.getInStoreID()),
      parentID = newOptionString(msg.getParentID()),
      validFromMillis = msg.getValidFromMillis(),
      validToMillis = msg.getValidToMillis(),
      resourceTypes = Set(msg.getResourceTypes().asScala.map(newResourceType).toSeq: _*),
      chargingBehaviors = Set(msg.getChargingBehaviors().asScala.map(_.toString).toSeq: _*),
      roleMapping = newRoleMapping(msg.getRoleMapping).toMap
    )
  }
}
