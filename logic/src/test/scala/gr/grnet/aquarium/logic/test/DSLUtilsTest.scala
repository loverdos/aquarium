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
import java.util.Date
import gr.grnet.aquarium.util.TestMethods
import gr.grnet.aquarium.logic.accounting.dsl._
import annotation.tailrec

class DSLUtilsTest extends DSLUtils with TestMethods with DSL {

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
  def testEffectiveTimeslots = {
    val from =  new Date(1321621969000L) //Fri Nov 18 15:12:49 +0200 2011
    val to =  new Date(1324214719000L)   //Sun Dec 18 15:25:19 +0200 2011

    var repeat = DSLTimeFrameRepeat(parseCronString("00 12 * * *"),
      parseCronString("00 14 * * *"))

    var result = effectiveTimeslots(repeat, from, Some(to))

    assertNotEmpty(result)
    testSuccessiveTimeslots(result)
    assertEquals(31, result.size)

    //Expansion outside timeframe
    repeat = DSLTimeFrameRepeat(parseCronString("00 12 * May *"),
      parseCronString("00 14 * Sep *"))
    result = effectiveTimeslots(repeat, from, Some(to))
    assertEquals(0, result.size)

    repeat = DSLTimeFrameRepeat(parseCronString("00 12 * * 5"),
      parseCronString("00 14 * * 1"))
    result = effectiveTimeslots(repeat, from, Some(to))
    testSuccessiveTimeslots(result)
    assertEquals(4, result.size)

    repeat = DSLTimeFrameRepeat(parseCronString("00 12 * * Mon,Wed,Fri"),
      parseCronString("00 14 * * Tue,Thu,Sat"))
    result = effectiveTimeslots(repeat, from, Some(to))
    testSuccessiveTimeslots(result)
    assertEquals(13, result.size)
  }

  @Test
  def testAllEffectiveTimeslots = {
    val from =  new Date(1321621969000L) //Fri Nov 18 15:12:49 +0200 2011
    val to =  new Date(1324214719000L)   //Sun Dec 18 15:25:19 +0200 2011

    val repeat1 = DSLTimeFrameRepeat(parseCronString("00 12 * * *"),
      parseCronString("00 14 * * *"))
    val repeat2 = DSLTimeFrameRepeat(parseCronString("00 18 * * 5"),
      parseCronString("00 20 * * 5"))
    val tf = DSLTimeFrame(from, None, List(repeat1, repeat2))

    val result = allEffectiveTimeslots(tf, from, to)
    assertEquals(36, result.size)
    testSuccessiveTimeslots(result)
  }

  @tailrec
  private def testSuccessiveTimeslots(result: List[(Date, Date)]): Unit = {
    if (result.isEmpty) return
    if (result.tail.isEmpty) return
    if (result.head._2.after(result.tail.head._1))
      fail("Effectivity timeslots not successive: %s %s".format(result.head, result.tail.head))
    testSuccessiveTimeslots(result.tail)
  }

  private def printTimeslots(result: List[(Date, Date)]) = {
    result.foreach(p => print("from:%s to:%s\n".format(p._1, p._2)))
  }
}