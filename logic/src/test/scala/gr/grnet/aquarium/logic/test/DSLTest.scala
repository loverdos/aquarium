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
import gr.grnet.aquarium.logic.accounting.dsl._
import org.junit.{Test}
import java.util.Date

class DSLTest {

  var creditpolicy : DSLCreditPolicy = _

  def before = {
    creditpolicy = DSL.parse(
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

    val d = DSL.findResource(creditpolicy, "diskspace").get
    assertNotNone(d)

    assertNotSame(creditpolicy.policies(0).algorithms(d),
      creditpolicy.policies(1).algorithms(d))
  }

  @Test
  def testMergePolicies = {
    val vm = DSLResource("vmtime")
    val bup = DSLResource("bup")
    val bdown = DSLResource("bdown")

    val a = DSLPolicy(
      "1", Some("2"),
      Map(vm -> "abc", bup -> "def"),
      DSLTimeFrame(
        new Date(0), Option(new Date(12345)), Some(List(
          DSLTimeFrameRepeat(
            List(DSLCronSpec(12, 34, 2, -1, -1)),
            List(DSLCronSpec(12, 34, 4, -1, -1))
          ))
        )
      )
    )
    val b = DSLPolicy("2", Some(""),
      Map(vm -> "xyz", bdown -> "foo"),
      DSLTimeFrame(new Date(0), Option(new Date(45678)), Option(List()))
    )

    val result = DSL.mergePolicy(a, b)

    assertEquals(result.name, "2")
    assertEquals(result.algorithms.size, 3)
    assertEquals(result.algorithms.get(vm), Some("abc"))
    assertEquals(result.algorithms.get(bup), Some("def"))
    assertEquals(result.algorithms.get(bdown), Some("foo"))
    assertEquals(1, result.effective.repeat.size)
  }

  @Test
  def testMergeTimeframes = {
    val a = DSLTimeFrame(
        new Date(0), Option(new Date(12345)), Some(List(
          DSLTimeFrameRepeat(
            List(DSLCronSpec(12, 34, 2, -1, -1)),
            List(DSLCronSpec(12, 34, 4, -1, -1))
          ))
        )
      )
    val b = DSLTimeFrame(new Date(0), Option(new Date(45678)), Some(List()))

    var result = DSL.mergeTimeFrames(a, b)
    assertEquals(a.from, result.from)
    assertEquals(a.to, result.to)
    assertEquals(1, result.repeat.get.size)

    result = DSL.mergeTimeFrames(b, a)
    assertEquals(b.from, result.from)
    assertEquals(b.to, result.to)
    assertEquals(1, result.repeat.get.size)
  }

  @Test
  def testCronParse = {
    var input = "12 * * * *"
    var output = DSL.parseCronString(input)
    assertEquals(output, List(DSLCronSpec(12, -1, -1, -1, -1)))

    input = "12 4 3 jaN-ApR *"
    output = DSL.parseCronString(input)
    assertEquals(output.size, 4)
    assertEquals(output(2), DSLCronSpec(12, 4, 3, 3, -1))

    input = "12 4 3 jaN-ApR MOn-FRi"
    output = DSL.parseCronString(input)
    assertEquals(output.size, 20)

    input = "12 4 foo jaN-ApR *"
    assertThrows(DSL.parseCronString(input))

    input = "12 4 foo jaN-ApR 9"
    assertThrows(DSL.parseCronString(input))

    input = "@midnight"
    assertThrows(DSL.parseCronString(input))
  }

  @Test
  def testSerialization = {
    before
    val a = creditpolicy.toYaml()
    println(a)
  }

  def assertThrows(f: => Unit) = {
    try {
      f
      assert(false)
    } catch {
      case e: Exception => assert(true)
    }
  }

  def assertNone(a: AnyRef) = a match {
    case None =>
    case x => fail()
  }

  def assertNotNone(a: AnyRef) = a match {
    case None => fail()
    case _ =>
  }
}