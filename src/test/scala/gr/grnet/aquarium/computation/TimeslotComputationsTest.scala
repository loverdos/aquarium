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

package gr.grnet.aquarium.computation

import gr.grnet.aquarium.util.TestMethods
import org.junit.Test
import java.util.Date
import junit.framework.Assert._
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.logic.test.DSLTestBase

/**
 * Tests for the methods that do accounting
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class TimeslotComputationsTest extends DSLTestBase with TimeslotComputations with TestMethods {
  @Test
  def testAlignTimeslots() {
    var a = List(Timeslot(0, 1))
    var b = List(Timeslot(0, 2))
    var result = alignTimeslots(a, b)
    assertEquals(2, result.size)
    assertEquals(result.head, Timeslot(0, 1))
    assertEquals(result.tail.head, Timeslot(1, 2))

    a = List(Timeslot(0, 10))
    b = List(Timeslot(0, 4), Timeslot(4, 12))
    result = alignTimeslots(a, b)
    assertEquals(3, result.size)
    assertEquals(result.head, Timeslot(0, 4))
    assertEquals(result.tail.head, Timeslot(4, 10))
    assertEquals(result.last, Timeslot(10, 12))

    a = List(Timeslot(0, 1), Timeslot(1, 3), Timeslot(3, 4))
    b = List(Timeslot(0, 2), Timeslot(2, 4))
    result = alignTimeslots(a, b)
    assertEquals(4, result.size)
    assertEquals(result.head, Timeslot(0, 1))
    assertEquals(result.tail.head, Timeslot(1, 2))
    assertEquals(result.tail.tail.head, Timeslot(2, 3))
    assertEquals(result.last, Timeslot(3, 4))

    before
    val from = new Date(1322555880000L) //Tue, 29 Nov 2011 10:38:00 EET
    val to = new Date(1322689082000L) //Wed, 30 Nov 2011 23:38:02 EET
    val agr = dsl.findAgreement("complextimeslots").get
    a = dslUtils.resolveEffectiveAlgorithmsForTimeslot(Timeslot(from, to), agr).keySet.toList
    b = dslUtils.resolveEffectivePricelistsForTimeslot(Timeslot(from, to), agr).keySet.toList

    result = alignTimeslots(a, b)
    assertEquals(9, result.size)
    assertEquals(result.last, b.last)
  }

  @Test
  def testSplitChargeChunks() = {
    before
    val from = new Date(1322555880000L) //Tue, 29 Nov 2011 10:38:00 EET
    val to = new Date(1322689082000L) //Wed, 30 Nov 2011 23:38:02 EET

    val agr = dsl.findAgreement("scaledbandwidth").get

    val alg = dslUtils.resolveEffectiveAlgorithmsForTimeslot(Timeslot(from, to), agr)
    val price = dslUtils.resolveEffectivePricelistsForTimeslot(Timeslot(from, to), agr)
    val chunks = splitChargeChunks(alg, price)
    val algChunks = chunks._1
    val priceChunks = chunks._2

    assertEquals(algChunks.size, priceChunks.size)

    testSuccessiveTimeslots(algChunks.keySet.toList)
    testSuccessiveTimeslots(priceChunks.keySet.toList)

    algChunks.keySet.zip(priceChunks.keySet).foreach {
      t => assertEquals(t._1, t._2)
    }
  }
}