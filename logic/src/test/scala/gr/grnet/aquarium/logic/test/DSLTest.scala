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

package gr.grnet.aquarium.logic.test

import org.junit.Assert._
import org.junit.{Test}
import java.util.Date
import gr.grnet.aquarium.logic.accounting.dsl._

class DSLTest extends DSL with TestMethods {

  var creditpolicy : DSLCreditPolicy = _

  def before = {
    creditpolicy = parse(
      getClass.getClassLoader.getResourceAsStream("policy.yaml")
    )
    assertNotNull(creditpolicy)
  }

  @Test
  def testParsePolicies = {
    before
    assertEquals(creditpolicy.policies.size, 2)
    assertEquals(creditpolicy.policies(0).algorithms.size,
      creditpolicy.resources.size)
    assertEquals(creditpolicy.policies(1).algorithms.size,
      creditpolicy.resources.size)

    val d = creditpolicy.findResource("diskspace").get
    assertNotNone(d)

    assertNotSame(creditpolicy.policies(0).algorithms(d),
      creditpolicy.policies(1).algorithms(d))
  }

  @Test
  def testParsePricelists = {
    before
    assertEquals(3, creditpolicy.pricelists.size)
    assertNotNone(creditpolicy.findPriceList("everyTue2"))
    val res = creditpolicy.findResource("diskspace")
    assertNotNone(res)
    assertEquals(0.05F,
      creditpolicy.findPriceList("everyTue2").get.prices.get(res.get).get, 0.01F)
  }

  @Test
  def testCronParse = {
    var input = "12 * * * *"
    var output = parseCronString(input)
    assertEquals(output, List(DSLTimeSpec(12, -1, -1, -1, -1)))

    input = "12 4 3 jaN-ApR *"
    output = parseCronString(input)
    assertEquals(output.size, 4)
    assertEquals(output(2), DSLTimeSpec(12, 4, 3, 3, -1))

    input = "12 4 3 jaN-ApR MOn-FRi"
    output = parseCronString(input)
    assertEquals(output.size, 20)

    input = "12 4 foo jaN-ApR *"
    assertThrows[DSLParseException](parseCronString(input))

    input = "12 4 foo jaN-ApR 9"
    assertThrows[DSLParseException](parseCronString(input))

    input = "@midnight"
    assertThrows[DSLParseException](parseCronString(input))
  }

  @Test
  def testSerialization = {
    before
  }
}