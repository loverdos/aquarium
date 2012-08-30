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

import gr.grnet.aquarium.message.avro.gen._
import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.collection.JavaConverters.seqAsJavaListConverter

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object MessageFactory {
  def newEffectiveUnitPrice(unitPrice: Double, whenOpt: Option[_CronSpecTuple] = None) = {
    _EffectiveUnitPrice.newBuilder().
      setUnitPrice(unitPrice).
      setWhen(whenOpt.getOrElse(null)).
    build()
  }

  def newEffectivePriceTable(priceOverrides: _EffectiveUnitPrice*) = {
    _EffectivePriceTable.newBuilder().
      setPriceOverrides(priceOverrides.asJava).
    build()
  }

  def newSelectorValue(ept: _EffectivePriceTable): _SelectorValue = {
    _SelectorValue.newBuilder().
      setSelectorValue(ept).
    build()
  }

  def newSelectorValue(map: Map[CharSequence, _SelectorValue]): _SelectorValue = {
    _SelectorValue.newBuilder().
      setSelectorValue(map.asJava).
    build()
  }

  def newSelectorValue(pairs: (CharSequence, _SelectorValue)*): _SelectorValue = {
    _SelectorValue.newBuilder().
      setSelectorValue(Map(pairs:_*).asJava).
    build()
  }

//  def newFullPriceTable(perResource: Map[CharSequence, Map[CharSequence, _SelectorValue]]) = {
//    _FullPriceTable.newBuilder().
//      setPerResource(
//        (for((k, v) ← perResource) yield (k, v.asJava)).asJava
//      ).
//    build()
//  }

  def newFullPriceTable(perResource: (CharSequence, Map[CharSequence, _SelectorValue])*) = {
    _FullPriceTable.newBuilder().
      setPerResource(
        Map((for((k, v) ← perResource) yield (k, v.asJava)):_*).asJava
      ).
    build()
  }

  def newRoleMapping(map: Map[CharSequence, _FullPriceTable]): java.util.Map[CharSequence, _FullPriceTable] = {
    map.asJava
  }

  def newRoleMapping(pairs: (CharSequence, _FullPriceTable)*): java.util.Map[CharSequence, _FullPriceTable] = {
    Map(pairs:_*).asJava
  }

  def newResourceType(name: CharSequence, unit: CharSequence, chargingBehavior: CharSequence) = {
    _ResourceType.newBuilder().
      setName(name).
      setUnit(unit).
      setChargingBehaviorClass(chargingBehavior).
    build()
  }

  def newResourceTypes(rts: _ResourceType*) = {
    rts.asJava
  }

  def newChargingBehaviors(cbs: CharSequence*) = {
    cbs.asJava
  }
}
