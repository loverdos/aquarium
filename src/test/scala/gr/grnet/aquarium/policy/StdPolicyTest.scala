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

package gr.grnet.aquarium.policy

import org.junit.Test
import gr.grnet.aquarium.Timespan
import gr.grnet.aquarium.charging.{OnceChargingBehavior, ContinuousChargingBehavior, VMChargingBehavior}
import gr.grnet.aquarium.converter.{StdConverters, PrettyJsonTextFormat}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class StdPolicyTest {
  final lazy val policy = StdPolicy(
    id = "policy-1",
    parentID = None,

    validityTimespan = Timespan(0),

    resourceTypes = Set(
      ResourceType("diskspace", "MB/Hr", classOf[ContinuousChargingBehavior].getName),
      ResourceType("vmtime",    "Hr",    classOf[VMChargingBehavior].getName)
    ),

    chargingBehaviors = Set(
      classOf[VMChargingBehavior].getName,
      classOf[ContinuousChargingBehavior].getName,
      classOf[OnceChargingBehavior].getName
    ),

    roleMapping = Map(
      "default" -> FullPriceTable(Map(
        "bandwidth" -> IsEffectivePriceTable(EffectivePriceTable(EffectiveUnitPrice(0.01) :: Nil)),
        "diskspace" -> IsEffectivePriceTable(EffectivePriceTable(EffectiveUnitPrice(0.01) :: Nil)),
        "vmtime"    -> IsEffectivePriceTable(EffectivePriceTable(EffectiveUnitPrice(0.01) :: Nil))
      ))
    )
  )

  @Test
  def testJson(): Unit = {
    val converters = StdConverters.AllConverters
    val json = converters.convertEx[PrettyJsonTextFormat](policy)
    val obj = converters.convertEx[StdPolicy](json)

    assert(policy == obj)
  }
}
