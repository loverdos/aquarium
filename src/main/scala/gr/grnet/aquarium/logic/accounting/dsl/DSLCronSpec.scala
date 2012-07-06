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
 * @author Prodromos Gerakios <pgerakios@grnet.gr>
 */

case class DSLCronSpec(cronSpec: String) {

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
  def nextValidDate(min0:Date,max0:Date,d:Date) : Option[Date] =
    (cronExpr getNextValidTimeAfter d) match {
      case null =>
        None
      case d1 =>
        val (min,max,e) = (min0.getTime,max0.getTime,d1.getTime)
        if(e < min || e>max)
          None
        else
          Some({assert(d1.getTime>=d.getTime);d1})
    }

  /*def nextValidDate(d:Date) : Date = {
   val ret = cronExpr getNextValidTimeAfter d
   ret
 } */

  /*def nextContinuousValidDate(d:Date) : Date = {
      val d1 = cronExpr getNextInvalidTimeAfter d
      if (d1 != null)
        d1.setTime(d1.getTime - 1000) /* 1 sec before was valid so return this*/
      d1
    }


    def getMaxValidBeforeDate(min0:Date , max0:Date,d:Date) : Option[Date] ={
      val (min,max,e) = (min0.getTime,max0.getTime,d.getTime)
      if(min > e || max < e) None
      else if(includes(d)) Some(d)
      else {
        var end = e
        var start = min //0L
        var best:Date = null
        var iterations=0L
        val tmp = new Date(0L)
        val step = 10000L * 60L // 1 minute
        while(start < end) {
          iterations += 1
          val pivot = (end - start) / 2 + start
          tmp.setTime(pivot)
          val next = nextValidDate(tmp)
          if(next == null){ /* no valid time after pivot*/
            end = pivot-step; // pivot minus one
          } else {
            val p = next.getTime()
            if(p < e) { /* next date occurs before e*/
              val post =  next.getTime < d.getTime
              assert(post,"BUG!!!")
              best = next
              start = p + step
            } else if( p > e) { /* next date occurs after e*/
              end = pivot-step
            }
            else assert(false,"This should not happen")
          }
        //  System.err.println("Start: " + new Date(start) + " end: " + new Date(end));
        }
        System.err.println("Iterations " + iterations);
        if(best!=null) Some(best) else None
      }
    }
    */
}

object DSLCronSpec {
  val emptyCronSpec = new DSLCronSpec("? ? ? ? ?")
}