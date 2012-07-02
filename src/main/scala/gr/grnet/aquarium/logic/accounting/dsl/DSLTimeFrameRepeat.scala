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

package gr.grnet.aquarium.logic.accounting.dsl

import org.quartz.CronExpression
import java.util.Date

/**
 * Encapsulates a repeating item
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
case class DSLTimeFrameRepeat (
  start: List[DSLTimeSpec],
  end: List[DSLTimeSpec],
  startCron: String,
  endCron: String
) extends DSLItem {

  private def makeCronExpression(s: String) : CronExpression  = {
    val e = "0 " + s.trim
    val l = e.split(" ")
    (l(3),l(5))  match {
      case ("?",_) | (_,"?") => ()
      case (_,"*") => l.update(5,"?")
      case ("*",_) => l.update(3,"?")
    }
    val e1 = l.foldLeft("") { (s,elt) => s + " " + elt}
    new CronExpression(e1)
  }
  val getStart = DSLCronSpec(startCron)
  val getEnd =  DSLCronSpec(endCron)

  assert(start.size == end.size,
    ("start (%s) and end (%s) cron-like specs do not expand to equal" +
      " number of repetition definitions").format(startCron, endCron))

  //Ensures that fields that have repeating entries, do so in both patterns
  start.zip(end).foreach {
    x =>
      assert((x._1.dom == -1 && x._2.dom == -1) ||
        (x._1.dom != -1 && x._2.dom != -1))

      assert((x._1.mon == -1 && x._2.mon == -1) ||
        (x._1.mon != -1 && x._2.mon != -1))

      assert((x._1.dow == -1 && x._2.dow == -1) ||
        (x._1.dow != -1 && x._2.dow != -1))
  }

  override def toMap(): Map[String, Any] = {
    Map[String,  Any]() ++
    Map(Vocabulary.start -> startCron) ++
    Map(Vocabulary.end -> endCron)
  }
}

object DSLTimeFrameRepeat {
  val emptyTimeFramRepeat = DSLTimeFrameRepeat(List(), List(), "", "")
}