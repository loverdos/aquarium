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

import org.junit.Test
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.JavaConverters.mapAsJavaMapConverter
import gr.grnet.aquarium.charging.VMChargingBehavior
import java.io.ByteArrayOutputStream

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class PolicyTest {
  @Test
  def testOne() {
    val policyConf = _Policy.newBuilder().
      setChargingBehaviors(
        List[CharSequence](
          "gr.grnet.aquarium.charging.VMChargingBehavior",
          "gr.grnet.aquarium.charging.ContinuousChargingBehavior",
          "gr.grnet.aquarium.charging.OnceChargingBehavior"
        ).asJava).
      setID("default-policy").
      setParentID("").
      setValidFromMillis(0L).
      setValidToMillis(Long.MaxValue).
      setResourceTypes(
        List(
          _ResourceType.newBuilder().
            setName("diskspace").
            setUnit("MB/Hr").
            setChargingBehaviorClass("gr.grnet.aquarium.charging.ContinuousChargingBehavior").
          build(),
          _ResourceType.newBuilder().
            setName("vmtime").
            setUnit("Hr").
            setChargingBehaviorClass("gr.grnet.aquarium.charging.VMChargingBehavior").
          build(),
          _ResourceType.newBuilder().
            setName("addcredits").
            setUnit("Credits").
            setChargingBehaviorClass("gr.grnet.aquarium.charging.OnceChargingBehavior").
          build()
        ).asJava).
      setRoleMapping(
        Map[CharSequence, _FullPriceTable](
            "default" -> _FullPriceTable.newBuilder().
              setPerResource(
                Map(
                  ("diskspace": CharSequence) -> Map[CharSequence, _SelectorValue](
                    "default" -> _SelectorValue.newBuilder().
                      setSelectorValue(_EffectiveUnitPrice.newBuilder().
                        setUnitPrice(0.01).
                        setWhen(null)
                      build()).
                    build()
                  ).asJava,
                  ("vmtime": CharSequence) -> Map[CharSequence, _SelectorValue](
                    VMChargingBehavior.Selectors.Power.powerOn -> _SelectorValue.newBuilder().
                      setSelectorValue(_EffectiveUnitPrice.newBuilder().
                        setUnitPrice(0.01).
                        setWhen(null)
                      build()).
                    build(),
                    VMChargingBehavior.Selectors.Power.powerOff -> _SelectorValue.newBuilder().
                      setSelectorValue(_EffectiveUnitPrice.newBuilder().
                        setUnitPrice(0.01).
                        setWhen(null)
                      build()).
                    build()
                  ).asJava,
                  ("addcredits": CharSequence) -> Map[CharSequence, _SelectorValue](
                    "default" -> _SelectorValue.newBuilder().
                      setSelectorValue(_EffectiveUnitPrice.newBuilder().
                        setUnitPrice(-1.0).
                        setWhen(null)
                      build()).
                    build()
                  ).asJava
                ).asJava
            ).build()
        ).asJava).
    build()

    val generatedJSON = AvroHelpers.jsonStringOfSpecificRecord(policyConf)
    println(generatedJSON)
  }
}
