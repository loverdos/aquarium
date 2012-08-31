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

package gr.grnet.aquarium.message.avro.gen

import gr.grnet.aquarium.charging.VMChargingBehavior
import gr.grnet.aquarium.message.avro.MessageFactory._
import gr.grnet.aquarium.util.nameOfClass
import org.junit.Test
import gr.grnet.aquarium.message.avro.AvroHelpers

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class PolicyMsgTest {
  @Test
  def testOne() {
    val policyConf = PolicyMsg.newBuilder().
      setOriginalID("default-policy").
      setInStoreID(null).
      setParentID(null).
      setValidFromMillis(0L).
      setValidToMillis(Long.MaxValue).
      setResourceTypes(newResourceTypeMsgs(
        newResourceTypeMsg("diskspace", "MB/Hr", nameOfClass[gr.grnet.aquarium.charging.ContinuousChargingBehavior]),
        newResourceTypeMsg("vmtime", "Hr", nameOfClass[gr.grnet.aquarium.charging.VMChargingBehavior]),
        newResourceTypeMsg("addcredits", "Credits", nameOfClass[gr.grnet.aquarium.charging.OnceChargingBehavior]))
      ).
      setChargingBehaviors(newChargingBehaviorMsgs(
        nameOfClass[gr.grnet.aquarium.charging.VMChargingBehavior],
        nameOfClass[gr.grnet.aquarium.charging.ContinuousChargingBehavior],
        nameOfClass[gr.grnet.aquarium.charging.OnceChargingBehavior])
      ).
      setRoleMapping(newRoleMappingMsg(
      "default" -> newFullPriceTableMsg(
        "diskspace" -> Map(
          "default" -> newSelectorValueMsg(newEffectivePriceTableMsg(newEffectiveUnitPriceMsg(0.01)))
        ),

        "vmtime" -> Map(
          VMChargingBehavior.Selectors.Power.powerOff ->
            newSelectorValueMsg(newEffectivePriceTableMsg(newEffectiveUnitPriceMsg(0.001))),
          VMChargingBehavior.Selectors.Power.powerOn ->
            newSelectorValueMsg(newEffectivePriceTableMsg(newEffectiveUnitPriceMsg(0.01)))
        ),
        "addcredits" -> Map(
          "default" -> newSelectorValueMsg(newEffectivePriceTableMsg(newEffectiveUnitPriceMsg(-1.0)))
        )
      )
    )).
      build()

    val generatedJSON = AvroHelpers.jsonStringOfSpecificRecord(policyConf)
    println(generatedJSON)
  }
}
