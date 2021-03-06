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

package gr.grnet.aquarium.logic.test

import org.junit.Assert._
import org.junit.{Test}
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import java.util.Date
import gr.grnet.aquarium.util.date.MutableDateCalc

/**
 * Tests for the Timeslot class
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class TimeslotTest /*extends TestMethods*/ {

  @Test
  def testOverlappingTimeslots = {
    var t = Timeslot(new Date(7), new Date(20))
    val list = List(Timeslot(new Date(1), new Date(3)),
      Timeslot(new Date(6), new Date(8)),
      Timeslot(new Date(11), new Date(15)))

    var result = t.overlappingTimeslots(list)
    assertEquals(2, result.size)
    assertEquals(Timeslot(new Date(7), new Date(8)), result.head)
    assertEquals(Timeslot(new Date(11), new Date(15)), result.tail.head)

    t = Timeslot(new Date(9), new Date(10))
    result = t.overlappingTimeslots(list)
    assertEquals(0, result.size)

    t = Timeslot(new Date(10), new Date(50))
    result = t.overlappingTimeslots(List(Timeslot(new Date(0), new Date(100))))
    assertEquals(1, result.size)
    assertEquals(t, result.head)

    t = Timeslot(new Date(0), new Date(100))
    result = t.overlappingTimeslots(List(Timeslot(new Date(10), new Date(50))))
    assertEquals(1, result.size)
    assertEquals(Timeslot(new Date(10), new Date(50)), result.head)
  }

  @Test
  def testNonOverlappingTimeslots = {
    var t = Timeslot(new Date(7L), new Date(20L))
    val list = List(Timeslot(new Date(1L), new Date(3L)),
      Timeslot(new Date(6L), new Date(8L)),
      Timeslot(new Date(11L), new Date(15L)))

    var result = t.nonOverlappingTimeslots(list)
    assertEquals(2L, result.size)

    t = Timeslot(new Date(9L), new Date(20L))
    result = t.nonOverlappingTimeslots(list)
    assertEquals(2, result.size)

    t = Timeslot(new Date(9), new Date(20))
    result = t.nonOverlappingTimeslots(list)
    assertEquals(2, result.size)

    t = Timeslot(new Date(0), new Date(20))
    result = t.nonOverlappingTimeslots(list)
    assertEquals(4, result.size)
    assertEquals(Timeslot(new Date(0), new Date(1)), result.head)
    assertEquals(Timeslot(new Date(3), new Date(6)), result.tail.head)

    t = Timeslot(new Date(13), new Date(20))
    result = t.nonOverlappingTimeslots(list)
    assertEquals(1, result.size)
    assertEquals(Timeslot(new Date(15), new Date(20)), result.head)

    result = t.nonOverlappingTimeslots(List())
    assertEquals(1, result.size)
    assertEquals(t, result.head)
  }

  @Test
  def testMergeOverlaps = {
    var l = List(Timeslot(new Date(3), new Date(5)),Timeslot(new Date(1), new Date(3)))

    var result = Timeslot.mergeOverlaps(l)
    assertEquals(1, result.size)
    assertEquals(Timeslot(new Date(1), new Date(5)), result.head)

    l = l ++ List(Timeslot(new Date(4), new Date(6)))
    result = Timeslot.mergeOverlaps(l)
    assertEquals(1, result.size)
    assertEquals(Timeslot(new Date(1), new Date(6)), result.head)

    l = l ++ List(Timeslot(new Date(7), new Date(8)))
    result = Timeslot.mergeOverlaps(l)
    assertEquals(2, result.size)
    assertEquals(Timeslot(new Date(1), new Date(6)), result.head)
    assertEquals(Timeslot(new Date(7), new Date(8)), result.tail.head)

    l = l ++ List(Timeslot(new Date(2), new Date(20)))
    result = Timeslot.mergeOverlaps(l)
    assertEquals(1, result.size)
    assertEquals(Timeslot(new Date(1), new Date(20)), result.head)
  }


}