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

import org.junit.Test
import gr.grnet.aquarium.logic.accounting.dsl.{Timeslot, DSLTimeFrameRepeat, DSLTimeSpec, DSLTimeFrame}
import gr.grnet.aquarium.util.TestMethods
import java.util.Date
import scala._
import scala.Some

/**
* Represents an effectivity timeframe.
*
* @author Prodromos Gerakios <pgerakios@grnet.gr>
*/
class DSLTimeFrameTest extends DSLTestBase with TestMethods {

  private def makeRepeat(startCron:String,endCron:String): DSLTimeFrameRepeat={
    new DSLTimeFrameRepeat(List(),List(),startCron,endCron)
  }

  private def makeRepeats(l:List[(String,String)]):List[DSLTimeFrameRepeat]=
    l.map {c=> val (a,b) = c ;  this.makeRepeat(a,b)}


  private def makeFrame(from:Date,to:Date,l:List[(String,String)]): DSLTimeFrame = {
    new DSLTimeFrame(from,Some(to),makeRepeats(l.toList))
  }

  private def intervals(from:Date,to:Date,l:List[(String,String)]):List[Timeslot]={
    var f1 = makeFrame(from,to,l)
    var i1 = f1.intervalsOf(Timeslot(from,to))
    Console.err.println("Time slots: " + i1.size)
    i1
  }

  @Test
  def intervalsTest = {
    val from1 =  new Date(1321621969000L) //Fri Nov 18 15:12:49 +0200 2011
    val to1 =  new Date(1324214719000L)   //Sun Dec 18 15:25:19 +0200 2011
    val specs1 = List(("33 12 * * *","33 13 * * *"))
    assert(intervals(from1,to1,specs1).size == 30)


    val from2 = new Date(1321621969000L) //Fri Nov 18 15:12:49 +0200 2011
    val to2 =  new Date(1321795519000L) //Sun  Nov 20 15:25:19 +0200 2011
    val specs2 =List(("33 12 * * *","33 13 * * *"))
    assert(intervals(from2,to2,specs2).size == 2)

    val from3 = new Date(1321621969000L) //Fri Nov 18 15:12:49 +0200 2011
    val to3 =  new Date(1321795519000L) //Sun  Nov 20 15:25:19 +0200 2011
    val specs3 = List(("33 12 * * *","33 13 * * *"),
                     ("33 14 * * *","15 15 * * *"))
    assert(intervals(from3,to3,specs3).size == 5)

    //TODO: add timeslot contained in big timeslot
  }
}