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
import gr.grnet.aquarium.logic.accounting.dsl.{DSLTimeFrame, DSLTimeFrameRepeat, DSL, DSLUtils}
import java.util.{GregorianCalendar, Calendar, Date}

/**
 * Performance tests for various critical path functions.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class PerfTest extends DSLUtils with DSL {

  @Test
  def testAllEffectiveTimeslotPerf = {

    val iter = 1000
    var start = System.currentTimeMillis()
    var numResolved = 0

    val from = new Date(0)
    val to = new Date(1324214719000L) //Sun Dec 18 15:25:19 +0200 2011

    val repeat1 = DSLTimeFrameRepeat(parseCronString("00 12 * * *"),
      parseCronString("00 14 * * *"))
    val repeat2 = DSLTimeFrameRepeat(parseCronString("00 18 * * 5"),
      parseCronString("00 20 * * 5"))
    val tf = DSLTimeFrame(from, None, List(repeat1, repeat2))

    (1 to iter).foreach {
      i =>
        val fromTS = (0 + (scala.math.random * (1324214719000L - 0) + 1)).asInstanceOf[Long]
        numResolved += allEffectiveTimeslots(tf, new Date(fromTS), to).size
    }

    var total = System.currentTimeMillis() - start
    print("allEffectiveTimeslots: 1000 calls in %s msec. (%s resolved)\n".format(total, numResolved))

    start = System.currentTimeMillis()
    numResolved = 0
    val c = new GregorianCalendar()
    c.setTime(to)
    c.add(Calendar.YEAR, -1)
    val min = c.getTime.getTime

    (1 to iter).foreach {
      i =>
        val fromTS = (min + (scala.math.random * (1324214719000L - min) + 1)).asInstanceOf[Long]
        numResolved += allEffectiveTimeslots(tf, new Date(fromTS), to).size
    }

    total = System.currentTimeMillis() - start
    print("allEffectiveTimeslots(1 yr back): 1000 calls in %s msec. (%s resolved)\n".format(total, numResolved))
  }
}