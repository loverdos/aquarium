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
import java.util.{Date}
import org.junit.Assume._
import gr.grnet.aquarium.LogicTestsAssumptions
import gr.grnet.aquarium.logic.accounting.dsl._

/**
 * Performance tests for various critical path functions.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class PerfTest extends DSLUtils with DSL {

  @Test
  def testAllEffectiveTimeslotPerf = {
    assumeTrue(LogicTestsAssumptions.EnablePerfTests)

    val iter = 1000
    var start = System.currentTimeMillis()
    var numResolved = 0

    val from = new Date(0)
    var to = new Date(2048576095000L) //Fri, 01 Dec 2034 08:54:55 GMT
    val today = new Date()

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

    val min = oneYearBack(today, new Date(0)).getTime
    val max = oneYearAhead(today, new Date(Int.MaxValue * 1000L)).getTime

    (1 to iter).foreach {
      i => 
        val rndStart = new Date((min + (scala.math.random * (max - min) + 1)).toLong)
        var rndEnd = new Date((min + (scala.math.random * (max - min) + 1)).toLong)

        while (rndEnd.before(rndStart)) rndEnd = new Date((min + (scala.math.random * (max - min) + 1)).toLong)
        val tf = DSLTimeFrame(rndStart, Some(rndEnd), List(repeat1, repeat2))

        numResolved += allEffectiveTimeslots(tf, Timeslot(new Date(min), new Date(max))).size
    }

    var total = System.currentTimeMillis() - start
    print("allEffectiveTimeslots: %d calls in %s msec. (%s resolved)\n".format(iter, total, numResolved))
  }
}