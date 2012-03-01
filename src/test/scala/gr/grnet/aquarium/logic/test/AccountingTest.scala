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

import gr.grnet.aquarium.util.TestMethods
import org.junit.{Test}
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import java.util.Date
import junit.framework.Assert._
import gr.grnet.aquarium.logic.accounting.{AccountingException, Accounting}
import gr.grnet.aquarium.logic.events.{WalletEntry, ResourceEvent}
import com.ckkloverdos.maybe.{NoVal, Failed, Just}

/**
 * Tests for the methods that do accounting
 * 
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class AccountingTest extends DSLTestBase with Accounting with TestMethods {

  @Test
  def testAlignTimeslots() {
    before
    val from = new Date(1322555880000L) //Tue, 29 Nov 2011 10:38:00 EET
    val to = new Date(1322689082000L)  //Wed, 30 Nov 2011 23:38:02 EET
    val agr = dsl.findAgreement("complextimeslots").get
    val a = resolveEffectiveAlgorithmsForTimeslot(Timeslot(from, to), agr).keySet.toList
    val b = resolveEffectivePricelistsForTimeslot(Timeslot(from, to), agr).keySet.toList

    val result = alignTimeslots(a, b)
    assertEquals(9, result.size)
    assertEquals(result.last,  b.last)
  }

  @Test
  def testSplitChargeChunks() = {
    before 
    val from = new Date(1322555880000L) //Tue, 29 Nov 2011 10:38:00 EET
    val to = new Date(1322689082000L) //Wed, 30 Nov 2011 23:38:02 EET

    val agr = dsl.findAgreement("scaledbandwidth").get

    val alg = resolveEffectiveAlgorithmsForTimeslot(Timeslot(from, to), agr)
    val price = resolveEffectivePricelistsForTimeslot(Timeslot(from, to), agr)
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

  @Test
  def testChargeEvent(): Unit = {
    before
    val agr = dsl.findAgreement("scaledbandwidth").get

    //Simple, continuous resource
    var evt = ResourceEvent("123", 1325762772000L, 1325762774000L, "12", "1", "bandwidthup", "1", "1", 123, Map())
    var wallet = chargeEvent(evt, agr, 112, new Date(1325755902000L), List(), None)
    wallet match {
      case Just(x) => assertEquals(2, x.size)
      case _ => fail("No results returned")
    }

    //Complex resource event without details, should fail
    evt = ResourceEvent("123", 1325762772000L, 1325762774000L, "12", "1", "vmtime", "1", "1", 1, Map())
    assertFailed[Exception, List[WalletEntry]](chargeEvent(evt, agr, 1, new Date(1325755902000L), List(), None))

    //Complex, onoff resource
    evt = ResourceEvent("123", 1325762772000L, 1325762774000L, "12", "1", "vmtime", "1", "1", 1, Map("vmid" -> "3"))
    wallet = chargeEvent(evt, agr, 0, new Date(1325755902000L), List(), None)
    wallet match {
      case Just(x) => assertEquals(2, x.size)
      case _ => fail("No results returned")
    }

    //Complex, onoff resource, with wrong states, should fail
    evt = ResourceEvent("123", 1325762772000L, 1325762774000L, "12", "1", "vmtime", "1", "1", 1, Map("vmid" -> "3"))
    assertFailed[Exception, List[WalletEntry]](chargeEvent(evt, agr, 1, new Date(1325755902000L), List(), None))

    //Simple, discrete resource
    evt = ResourceEvent("123", 1325762772000L, 1325762774000L, "12", "1", "bookpages", "1", "1", 120, Map())
    wallet = chargeEvent(evt, agr, 15, new Date(1325755902000L), List(), None)
    wallet match {
      case Just(x) => assertEquals(1, x.size)
      case _ => fail("No results returned")
    }

    //Simple, discrete resource, time of last update equal to current event's occurred time
    evt = ResourceEvent("123", 1325762772000L, 1325762774000L, "12", "1", "bookpages", "1", "1", 120, Map())
    wallet = chargeEvent(evt, agr, 15, new Date(1325762772000L), List(), None)
    assertEquals(1, wallet.getOr(List(WalletEntry.zero, WalletEntry.zero)).size)

    //Simple, continuous resource, time of last update equal to current event's occurred time
    evt = ResourceEvent("123", 1325762772000L, 1325762774000L, "12", "1", "bandwidthup", "1", "1", 123, Map())
    wallet = chargeEvent(evt, agr, 15, new Date(1325762772000L), List(), None)
    assertEquals(0, wallet.getOr(List(WalletEntry.zero)).size)
  }
}