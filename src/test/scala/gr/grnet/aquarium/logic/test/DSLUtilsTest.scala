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

import org.junit.Test
import org.junit.Assert._
import gr.grnet.aquarium.util.TestMethods
import gr.grnet.aquarium.logic.accounting.dsl._
import annotation.tailrec
import java.util.Date

class DSLUtilsTest extends DSLTestBase with DSLUtils with TestMethods {

  @Test
  def testExpandTimeSpec = {
    val from =  new Date(1321621969000L) //Fri Nov 18 15:12:49 +0200 2011
    val to =  new Date(1324214719000L)   //Sun Dec 18 15:25:19 +0200 2011

    var a = DSLTimeSpec(33, 12, -1, -1, 3)
    var result = expandTimeSpec(a, from, to)
    assertEquals(4, result.size)

    a = DSLTimeSpec(33, 12, -1, 10, 3)   // Timespec falling outside from-to
    result = expandTimeSpec(a, from, to)
    assertEquals(0, result.size)

    // Would only return an entry if the 1rst of Dec 2011 is Thursday
    a = DSLTimeSpec(33, 12, 1, -1, 3)
    result = expandTimeSpec(a, from, to)
    assertEquals(0, result.size)

    // The 9th of Dec 2011 is Friday
    a = DSLTimeSpec(33, 12, 9, -1, 5)
    result = expandTimeSpec(a, from, to)
    assertEquals(1, result.size)

    // Every day
    a = DSLTimeSpec(33, 12, -1, -1, -1)
    result = expandTimeSpec(a, from, to)
    assertEquals(31, result.size)
  }

  @Test
  def testExpandTimeSpecs = {
    val from =  new Date(1321621969000L) //Fri Nov 18 15:12:49 +0200 2011
    val to =  new Date(1324214719000L)   //Sun Dec 18 15:25:19 +0200 2011

    val a = DSLTimeSpec(33, 12, -1, -1, 3)
    var result = expandTimeSpecs(List(a), from, to)
    assertNotEmpty(result)
    assertEquals(4, result.size)

    val b = DSLTimeSpec(00, 18, -1, -1, -1)
    result = expandTimeSpecs(List(a,b), from, to)
    assertNotEmpty(result)
    assertEquals(34, result.size)
  }

  @Test
  def testMergeOverlaps = {
    var l = List(Timeslot(new Date(12345000), new Date(13345000)),
      Timeslot(new Date(12845000), new Date(13845000)))

    var result = mergeOverlaps(l)
    assertEquals(1, result.size)
    assertEquals(Timeslot(new Date(12345000), new Date(13845000)), result.head)

    l = l ++ List(Timeslot(new Date(13645000), new Date(14845000)))
    result = mergeOverlaps(l)
    assertEquals(1, result.size)
    assertEquals(Timeslot(new Date(12345000), new Date(14845000)), result.head)

    l = l ++ List(Timeslot(new Date(15845000), new Date(16845000)))
    result = mergeOverlaps(l)
    assertEquals(2, result.size)
    assertEquals(Timeslot(new Date(12345000), new Date(14845000)), result.head)
    assertEquals(Timeslot(new Date(15845000), new Date(16845000)), result.tail.head)
  }

  @Test
  def testEffectiveTimeslots = {
    val from =  new Date(1321621969000L) //Fri Nov 18 15:12:49 +0200 2011
    val to =  new Date(1324214719000L)   //Sun Dec 18 15:25:19 +0200 2011

    var repeat = DSLTimeFrameRepeat(
      parseCronString("00 12 * * *"),
      parseCronString("00 14 * * *"),
      "00 12 * * *",
      "00 14 * * *"
    )

    var result = effectiveTimeslots(repeat, from, Some(to))

    assertNotEmpty(result)
    testSuccessiveTimeslots(result)
    assertEquals(31, result.size)

    //Expansion outside timeframe
    repeat = DSLTimeFrameRepeat(
      parseCronString("00 12 * May *"),
      parseCronString("00 14 * Sep *"),
      "00 12 * May *",
      "00 14 * Sep *")
    result = effectiveTimeslots(repeat, from, Some(to))
    assertEquals(0, result.size)

    repeat = DSLTimeFrameRepeat(
      parseCronString("00 12 * * 5"),
      parseCronString("00 14 * * 1"),
      "00 12 * * 5",
      "00 14 * * 1")
    result = effectiveTimeslots(repeat, from, Some(to))
    testSuccessiveTimeslots(result)
    assertEquals(4, result.size)

    repeat = DSLTimeFrameRepeat(
      parseCronString("00 12 * * Mon,Wed,Fri"),
      parseCronString("00 14 * * Tue,Thu,Sat"),
      "00 12 * * Mon,Wed,Fri",
      "00 14 * * Tue,Thu,Sat")
    result = effectiveTimeslots(repeat, from, Some(to))
    testSuccessiveTimeslots(result)
    assertEquals(13, result.size)

    repeat = DSLTimeFrameRepeat(
      parseCronString("00 00 * May *"),
      parseCronString("59 23 * Sep *"),
      "00 00 * May *",
      "59 23 * Sep *")
    result = effectiveTimeslots(repeat, new Date(1304121600000L),
      Some(new Date(1319932800000L)))
    assertNotEmpty(result)
  }

  @Test
  def testAllEffectiveTimeslots = {
    var from = new Date(1321621969000L) //Fri Nov 18 15:12:49 +0200 2011
    val to =  new Date(1324214719000L)   //Sun Dec 18 15:25:19 +0200 2011

    val repeat1 = DSLTimeFrameRepeat(
      parseCronString("00 12 * * *"),
      parseCronString("00 14 * * *"),
      "00 12 * * *",
      "00 14 * * *")
    val repeat2 = DSLTimeFrameRepeat(
      parseCronString("00 18 * * 5"),
      parseCronString("00 20 * * 5"),
      "00 18 * * 5",
      "00 20 * * 5")
    val tf = DSLTimeFrame(from, None, List(repeat1, repeat2))

    var result = allEffectiveTimeslots(tf, Timeslot(from, to))
    assertEquals(36, result.size)
    testSuccessiveTimeslots(result)

    result = allEffectiveTimeslots(DSLTimeFrame(new Date(0), None, List()),
      Timeslot(new Date(14), new Date(40)))
    assertEquals(1, result.size)
  }

  @Test
  def testNonEffectiveTimeslots = {
    val from =  new Date(1321621969000L) //Fri Nov 18 15:12:49 +0200 2011
    val to =  new Date(1324214719000L)   //Sun Dec 18 15:25:19 +0200 2011

    var repeat = DSLTimeFrameRepeat(
      parseCronString("00 12 * * *"),
      parseCronString("00 14 * * *"),
      "00 12 * * *",
      "00 14 * * *")

    var result = ineffectiveTimeslots(repeat, from, Some(to))
    assertEquals(30, result.size)
    testSuccessiveTimeslots(result)
    //printTimeslots(result)
  }

  @Test
  def testTimeContinuum : Unit = {
    val from =  new Date(1321621969000L) //Fri Nov 18 15:12:49 +0200 2011
    val to =  new Date(1324214719000L)   //Sun Dec 18 15:25:19 +0200 2011

    var repeat = DSLTimeFrameRepeat(
      parseCronString("00 12 * * *"),
      parseCronString("00 14 * * *"),
      "00 12 * * *",
      "00 14 * * *"
    )

    val continuum = effectiveTimeslots(repeat, from, Some(to)) ++
      ineffectiveTimeslots(repeat, from, Some(to)) sortWith sorter

    testSuccessiveTimeslots(continuum)
    testNoGaps(continuum)

    return
  }

  @Test
  def testFindEffective = {
    before
    val agr = dsl.findAgreement("scaledbandwidth").get

    val ts1 = 1322649482000L //Wed, 30 Nov 2011 12:38:02 EET
    val ts2 = 1322656682000L //Wed, 30 Nov 2011 14:38:02 EET
    val ts3 = 1322660282000L //Wed, 30 Nov 2011 15:38:02 EET
    val ts4 = 1322667482000L //Wed, 30 Nov 2011 17:38:02 EET
    val ts5 = 1322689082000L //Wed, 30 Nov 2011 23:38:02 EET
    val ts6 = 1322555880000L //Tue, 29 Nov 2011 10:38:00 EET

    var pricelists = resolveEffectivePricelistsForTimeslot(Timeslot(new Date(ts1), new Date(ts2)), agr)
    assertEquals(2, pricelists.keySet.size)
    assertNotNone(pricelists.get(new Timeslot(new Date(1322654402000L), new Date(1322656682000L))))
    assertEquals("foobar", pricelists.head._2.name)

    pricelists = resolveEffectivePricelistsForTimeslot(Timeslot(new Date(ts2), new Date(ts3)), agr)
    assertEquals(1, pricelists.keySet.size)
    assertEquals("default", pricelists.head._2.name)

    pricelists = resolveEffectivePricelistsForTimeslot(Timeslot(new Date(ts1), new Date(ts4)), agr)
    assertEquals(2, pricelists.keySet.size)
    assertEquals("foobar", pricelists.head._2.name)
    assertEquals("default", pricelists.tail.head._2.name)

    pricelists = resolveEffectivePricelistsForTimeslot(Timeslot(new Date(ts1), new Date(ts5)), agr)
    assertEquals(4, pricelists.keySet.size)

    pricelists = resolveEffectivePricelistsForTimeslot(Timeslot(new Date(ts6), new Date(ts5)), agr)
    assertEquals(9, pricelists.keySet.size)
  }


  private def printTimeslots(result: List[Timeslot]) = {
    result.foreach(p => print("from:%s to:%s\n".format(p.from, p.to)))
  }
}