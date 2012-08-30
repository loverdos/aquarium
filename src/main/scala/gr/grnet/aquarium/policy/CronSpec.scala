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
package gr.grnet.aquarium.policy

import org.quartz.CronExpression
import java.util.Date
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot

/**
 * @author Prodromos Gerakios <pgerakios@grnet.gr>
 */

/*
 *  Format:
 *  minutes hours day-of-month Month Day-of-Week (we do not specify seconds)
 *  see: http://quartz-scheduler.org/api/2.0.0/org/quartz/CronExpression.html
 * */
case class CronSpec(cronSpec: String) {

  private val cronExpr = {
    val e = "00 " + cronSpec.trim //IMPORTANT: WE DO NOT CARE ABOUT SECONDS!!!
    val l = e.split(" ")
    assert(l.size == 6,"Invalid cron specification")
    //for {ll <- l } Console.err.println(ll)
    (l(3),l(5))  match {
      case ("?",_) | (_,"?") => ()
      case (_,"*") => l.update(5,"?")
      case ("*",_) => l.update(3,"?")
    }
    val e1 = l.foldLeft("") { (s,elt) => s + " " + elt}
    //Console.err.println("e = " + e + " and e1 = " + e1)
    new CronExpression(e1)
  }

  def includes(d:Date) : Boolean =
    cronExpr isSatisfiedBy d

  /* the next valid date cannot outlive (min,max)*/
  def nextValidDate(t:Timeslot,d:Date) : Option[Date] =
    (cronExpr getNextValidTimeAfter d) match {
      case null =>
        None
      case d1 =>
        val (min0,max0) = (t.from,t.to)
        val (min,max,e) = (min0.getTime,max0.getTime,d1.getTime)
        if(e < min || e>max)
          None
        else
          Some({assert(d1.getTime>=d.getTime);d1})
    }

  override def toString : String = cronSpec
}