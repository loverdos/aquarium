package gr.grnet.aquarium.policy.test

import gr.grnet.aquarium.util.TestMethods
import org.junit.Test
import java.util
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.policy.{EffectiveUnitPriceModel, CronSpec}

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

/**
 * Tests for the Timeslot class
 *
 * @author Prodromos Gerakios <pgerakios@grnet.gr>
 */
class EffectiveUnitPriceTest extends TestMethods {

  private type EFU = EffectiveUnitPriceModel
  private type Intervals = List[Timeslot]

  private val printScreen = false

  private def noOverlap(i1:Intervals,i2:Intervals)  = {
    for { t1 <- i1
          t2 <- i2
    } assert(!t1.weakOverlaps(t2),"Intervals\n t1="+t1 + " \nand\n t2="+t2 + "\n overlap.")
  }

  private def singleT(t:Timeslot,i1:Intervals,i2:Intervals)  = {
    var l =  Timeslot.mergeOverlaps(i1++i2)
    assert(l.size == 1)
    assert(l.head == t)

  }
  private def cronOK(cs:CronSpec,d:util.Date) = {
    assert(cs.includes(d))
  }

  private def cronNotOK(cs:CronSpec,d:util.Date) = {
    assert(!cs.includes(d))
  }
  private def testEFU(start:Long,end:Long,v:Double,cronStart:String,cronEnd:String) :
  (Intervals,Intervals) = {
    var cronStart0 : CronSpec = null
    var cronEnd0 : CronSpec = null
    val opt = if (cronStart.isEmpty || cronEnd.isEmpty) None
    else Some(({cronStart0=new CronSpec(cronStart);cronStart0},
      {cronEnd0=new CronSpec(cronEnd);cronEnd0}))
    val ts=Timeslot(start,end)
    if(printScreen) Console.err.println("Timeslot: " + ts)
    val efu = new EffectiveUnitPriceModel(v,opt)
    val (l1,l2) =  efu.splitTimeslot(ts)
    noOverlap(l1,l2)
    singleT(ts,l1,l2)
    if(cronStart0!=null && cronEnd0!=null){
      for {
        t <- l1
      } {
        cronOK(cronStart0,t.from)
        cronOK(cronEnd0,t.to)
      }
      for {
        t <- l2
      } {
        cronNotOK(cronStart0,t.from)
        cronNotOK(cronEnd0,t.to)
      }
    }
    (l1,l2)
  }

  private def print(i:Intervals){
    if(printScreen) {
      Console.err.println("BEGIN INTERVAL")
      for { ii <-i } Console.err.println(ii)
      Console.err.println("END INTERVAL")
    }
  }


  @Test
  def splitTest1 = {
    testEFU(
      1321621969000L, //Fri Nov 18 15:12:49 +0200 2011
      1324214719000L, //Sun Dec 18 15:25:19 +0200 2011
      5.0,
      "33 12 * * *",
      "33 13 * * *"
    )
    val (l2_a,l2_b) = testEFU(
      1321621969000L, //Fri Nov 18 15:12:49 +0200 2011
      1321795519000L, //Sun  Nov 20 15:25:19 +0200 2011
      8.0,
      "15 12 * * *",
      "33 13 * * *"
    )
    assert(l2_a.size == 2)
    print(l2_a)

    val (l3_a,l3_b) = testEFU(
      1321621969000L, //Fri Nov 18 15:12:49 +0200 2011
      1321795519000L, //Sun  Nov 20 15:25:19 +0200 2011
      10.0,
      "33 12 * * *",
      "33 13 * * *"
    )
    print(l3_a)
    assert(l3_a.size == 2)

    val (l4_a,l4_b) = testEFU(
      1321621969000L, //Fri Nov 18 15:12:49 +0200 2011
      1321795519000L, //Sun  Nov 20 15:25:19 +0200 2011
      15.0,
      "33 12 * * *",
      "33 13 * * *"
    )
    print(l4_a)
    assert(l4_a.size == 2)

    ()
  }

}