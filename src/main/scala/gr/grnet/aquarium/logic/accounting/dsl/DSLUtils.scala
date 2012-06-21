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

import gr.grnet.aquarium.util.DateUtils
import java.util.{Date, GregorianCalendar, Calendar}
import scala.collection.immutable
import java.util

/**
 * Utility functions to use when working with DSL types.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */

trait DSLUtils extends DateUtils {

  val maxdate = new Date(Int.MaxValue * 1000L)

  /**
   * Resolves the effective algorithm for each chunk of the
   * provided timeslot and returns it as a Map
   */
  def resolveEffectiveAlgorithmsForTimeslot(timeslot: Timeslot,
                                           agr: DSLAgreement):
  immutable.SortedMap[Timeslot, DSLAlgorithm] =
    resolveEffective[DSLAlgorithm](timeslot, agr.algorithm)

  /**
   * Resolves the effective price list for each chunk of the
   * provided timeslot and returns it as a Map
   */
  def resolveEffectivePricelistsForTimeslot(timeslot: Timeslot,
                                            agr: DSLAgreement):
  immutable.SortedMap[Timeslot, DSLPriceList] =
    resolveEffective[DSLPriceList](timeslot,agr.pricelist)

  /**
   * Splits the provided timeslot into chunks according to the validity
   * timeslots specified by the provided time bounded item. It
   * returns a map whose keys are the timeslot chunks and the values
   * correspond to the effective time bounded item (algorithm or pricelist).
   */
  def resolveEffective[T <: DSLTimeBoundedItem[T]](timeslot: Timeslot,policy: T):
  immutable.SortedMap[Timeslot, T] = {
      assert(policy.toTimeslot contains timeslot,"Policy does not contain timeslot")

     /* Get a list of all effective/expanded policy timeslots within
        a timeslot specified by "variable timeslot".
      * NOTICE: The returned timeslots may be slightly out of "timeslot" bounds
      *         so we need to invoke overlappingTimeslots and nonOverlapping timeslots
      */
     val all_timeslots = allEffectiveTimeslots(policy.effective,timeslot)
     val timeslots_IN_policy = timeslot.overlappingTimeslots(all_timeslots)
     val timeslots_NOT_IN_policy = timeslot.nonOverlappingTimeslots(all_timeslots)

      immutable.SortedMap[Timeslot, T]() ++
      /*add [timeslots -> policy] covered by this policy*/
      timeslots_IN_policy.flatMap {
         effective_timeslot => Map(effective_timeslot -> policy)
      } ++
      /*add [timeslots -> policy] covered by parent policies */
      timeslots_NOT_IN_policy.flatMap { /* search the policy hierarchy for effective timeslots not covered by this policy.*/
        not_effective_timeslot => policy.overrides match {
          case None => immutable.SortedMap[Timeslot, T]() /*Nothing to do. TODO: throw exception ?*/
          case Some(parent_policy) => resolveEffective(not_effective_timeslot,parent_policy) /* search the policy hierarchy*/
        }
      }
  }

  /**
   * Utility function to put timeslots in increasing start timestamp order
   */
  private def sorter(x: Timeslot, y: Timeslot) : Boolean =   y.from after x.from

  /**
   * Get a list of all timeslots within which a timeframe
   * is effective, whithin the provided time bounds.
   */
  def allEffectiveTimeslots(spec: DSLTimeFrame, t: Timeslot):
  List[Timeslot] =
    if (spec.repeat.isEmpty) { //A  simple timeframe with no repetition defined
      val fromDate = if (spec.from.before(t.from)) t.from else spec.from
      val toDate = if (spec.to.getOrElse(t.to).after(t.to)) t.to else spec.to.getOrElse(t.to)
      List(Timeslot(fromDate, toDate))
    } /* otherwise for all repetitions determine timeslots*/
    else mergeOverlaps(for { r <- spec.repeat
                            ts <- effectiveTimeslots(r, t.from, t.to) }
                       yield ts)

  /**
   * Merges overlapping timeslots.
   */
  private[logic] def mergeOverlaps(list: List[Timeslot]): List[Timeslot] =
    (list sortWith sorter).foldLeft(List[Timeslot]()) {
      case (Nil,b) =>
        List(b)
      case (hd::Nil,b) =>
        if (hd overlaps  b) (hd merge b)::Nil
        else b::hd::Nil
      case (a @ hd::tl,b) =>
        if(hd overlaps b) (hd merge b)::tl
        else b :: a
      }.reverse


  /**
   * Get a list of all timeslots within which a time frame is active.
     The result is returned sorted by timeframe start date.
   */
  def effectiveTimeslots(spec: DSLTimeFrameRepeat, from: Date, to : Date):
    List[Timeslot] =
      for { (h1,h2) <- spec.start zip spec.end
            (d1,d2) <- h1.expandTimeSpec(from, to) zip h2.expandTimeSpec(from, to)
          }
      yield Timeslot(d1,d2)
}
