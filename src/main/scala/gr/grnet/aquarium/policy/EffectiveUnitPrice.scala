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

import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import collection.mutable
import java.util

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 * @author Prodromos Gerakios <pgerakios@grnet.gr>
 */

case class EffectiveUnitPrice(unitPrice: Double, when: Option[(CronSpec,CronSpec)]) { // TODO: use DSLTimeframe

  /* Split a timeslot T into two *sets* S and S2 consisting of timeslots such that
   *  (a) each element in S1,S2 is contained in T
   *  (b) for all x in S1 and y in S2 there is no overlap between x and y.
   *  (c) the union of all x in S1 and y S2 is T
   *  (d) the elements of S1 satisfy the cron spec ``when''
   *  (e) the elements of S2 do NOT satisfy the cron spec ``when''
   */
  def splitTimeslot(t:Timeslot) : (List[Timeslot],List[Timeslot])=
     when match {
       case None =>
         (List(t),Nil)
       case Some((start,end)) =>
         val result = new mutable.ListBuffer[Timeslot]()
         var offset = t.from
         while(start.nextValidDate(t,offset) match {
           case None =>
             false
           case Some(d_start) =>
             end.nextValidDate(t,d_start) match {
               case None =>
                 result += Timeslot(d_start,t.to)
                 false
               case Some(d_end) =>
                 result += Timeslot(d_start,d_end)
                 offset = d_end //new util.Date(d_end.getTime + 1000L)
                 d_end.before(t.to)
             }
         }) ()
         val l = result.toList
         val l1 = Timeslot mergeOverlaps l
         val l2 = t nonOverlappingTimeslots l1
         val l3 = Timeslot mergeOverlaps l2
         (l1,l3)
     }

  private def stringOfStartCron = when match {
    case None => "? ? ? ? ?"
    case Some((s,_)) => s.toString
  }
  private def stringOfEndCron = when match {
    case None => "? ? ? ? ?"
    case Some((_,s)) => s.toString
  }
  override def toString : String = "EffectiveUnitPrice(%d,%s,%s)".
                          format(unitPrice,stringOfStartCron,stringOfEndCron)
 }
