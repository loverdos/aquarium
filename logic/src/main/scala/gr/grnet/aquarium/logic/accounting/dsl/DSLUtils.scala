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

package gr.grnet.aquarium.logic.accounting.dsl

import gr.grnet.aquarium.util.DateUtils
import java.util.{Date, GregorianCalendar, Calendar}

/**
 * Utility functions to use when working with DSL types.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */

trait DSLUtils extends DateUtils {

  val maxdate = new Date(Int.MaxValue * 1000L)
  val mindate = new Date(0)

  def resolveEffectiveAlgorithmsForTimeslot(timeslot: Timeslot,
                                           agr: DSLAgreement):
  Map[Timeslot, DSLAlgorithm] =
    resolveEffective[DSLAlgorithm](timeslot, Some(agr.algorithm))


  def resolveEffectivePricelistsForTimeslot(timeslot: Timeslot,
                                            agr: DSLAgreement):
  Map[Timeslot, DSLPriceList] =
    resolveEffective[DSLPriceList](timeslot, Some(agr.pricelist))

  /**
   * Resolves the DSLTimeBoundedItem which is active within the
   * provided timeslot. If the provided timeslot does not fit entirely or at all
   * into a timeslot within which a DSLTimeBoundedItem is active, then the
   * resolution takes the following paths:
   *
   *  - If the provided timeslot (a) partially fits into the DSLTimeBoundedItem
   *  timeslot (b) and the next active time slot is (c), then the provided
   *  timeslot is split in three parts `(a.start...b.end)`,
   *  `(b.end...c.start)` and `(c.start...a.end)`
   *
   */
  def resolveEffective[T <: DSLTimeBoundedItem[T]](timeslot: Timeslot,
                                                   tbi: Option[T]):
  Map[Timeslot, T] = {

    val policy = tbi match {
      case None => return Map()
      case _ => tbi.get
    }

    val eff = allEffectiveTimeslots(policy.effective,
      oneYearBack(timeslot.from, policy.effective.from),
      oneYearAhead (timeslot.to, policy.effective.to.getOrElse(maxdate)))

    Map() ++
      timeslot.overlappingTimeslots(eff).flatMap {
        t => Map(t -> policy)
      } ++
      timeslot.nonOverlappingTimeslots(eff).flatMap {
        t => resolveEffective(t, policy.overrides)
      }
  }

  /**
   * Get a list of timeslots within which a timeframe is not effective.
   */
  def ineffectiveTimeslots(spec: DSLTimeFrameRepeat, from: Date, to: Option[Date]):
    List[Timeslot] = {

    buildNotEffectiveList(effectiveTimeslots(spec, from, to)) sortWith sorter
  }

  private def buildNotEffectiveList(l :List[Timeslot]) :
    List[Timeslot] = {

    if (l.isEmpty) return List()
    if (l.tail.isEmpty) return List()

    assert(l.head.to.getTime < l.tail.head.from.getTime)

    List[Timeslot]() ++
      List(Timeslot(new Date(l.head.to.getTime + 1),
        new Date(l.tail.head.from.getTime - 1))) ++
      buildNotEffectiveList(l.tail)
  }

  /**
   * Merges overlapping timeslots. The merge is exhaustive only if the
   * provided list is sorted in increasing timeslot from order.
   */
  def mergeOverlaps(list: List[Timeslot]): List[Timeslot] = {
    list.foldLeft(List[Timeslot]()) {
      (a, b) =>
        if (a.isEmpty)
          List(b)
        else if (a.tail.isEmpty)
          a.head.merge(b)
        else {
          val merged = a.tail.head.merge(b)
          a ++ (if (merged.size == 1) merged else List(b))
        }
    }
  }

  /**
   * Get a list of all timeslots within which the provided time frame
   * is effective.
   */
  def allEffectiveTimeslots(spec: DSLTimeFrame):
  List[Timeslot] = {

    val l = spec.repeat.flatMap {
      r => effectiveTimeslots(r, spec.from, spec.to)
    } sortWith sorter
    mergeOverlaps(l)
  }

  /**
   * Get a list of all timeslots within which a timeframe
   * is effective, whithin the provided time bounds.
   */
  def allEffectiveTimeslots(spec: DSLTimeFrame, from: Date, to: Date):
  List[Timeslot] = {

    //A timeframe with no repetition defined
    if (spec.repeat.isEmpty) {
      val fromDate = if (spec.from.before(from)) from else spec.from
      val toDate = if (spec.to.getOrElse(to).after(to)) to else spec.to.getOrElse(to)
      return List(Timeslot(fromDate, toDate))
    }

    val l = spec.repeat.flatMap {
      r => effectiveTimeslots(r, from, Some(to))
    } sortWith sorter
    mergeOverlaps(l)
  }

  /**
   * Get a list of all time periods within which a time frame is active.
   * If the to date is None, the expansion takes place within a timeframe
   * between `from .. from` + 1 year. The result is returned sorted by
   * timeframe start date.
   */
  def effectiveTimeslots(spec: DSLTimeFrameRepeat, from: Date, to: Option[Date]):
    List[Timeslot] = {

    assert(spec.start.size == spec.end.size)

    val endDate = to match {
      case None => //One year from now
        val c = new GregorianCalendar()
        c.setTime(from)
        c.add(Calendar.YEAR, 1)
        c.getTime
      case Some(y) => y
    }

    coExpandTimespecs(spec.start.zip(spec.end), from, endDate) sortWith sorter
  }

  /**
   * Utility function to put timeslots in increasing start timestamp order
   */
  def sorter(x: Timeslot, y: Timeslot) : Boolean =
    if (y.from after x.from) true else false

  /**
   * Calculate periods of activity for a list of timespecs
   */
  private def coExpandTimespecs(input: List[(DSLTimeSpec, DSLTimeSpec)],
                                from: Date, to: Date): List[Timeslot] = {
    if (input.size == 0) return List()

    expandTimeSpec(input.head._1, from, to).zip(
      expandTimeSpec(input.head._2, from, to)).map(
        l => Timeslot(l._1, l._2)
      ) ++
      coExpandTimespecs(input.tail, from, to)
  }

  /**
   * Expand a List of timespecs.
   */
  def expandTimeSpecs(spec: List[DSLTimeSpec], from: Date,  to: Date):
    List[Date] =
    spec.flatMap { t => expandTimeSpec(t, from, to)}

  /**
   * Get the list of time points prescribed by the provided timespec,
   * within the timeframe between from and to.
   */
  def expandTimeSpec(spec: DSLTimeSpec, from: Date,  to: Date) : List[Date] = {
    val adjusted = adjustToTime(from, spec.hour, spec.min)
    findDays(adjusted, to, {
      c =>
        (if (spec.mon >= 0) {c.get(Calendar.MONTH) == spec.getCalendarMonth()} else true) &&
        (if (spec.dom >= 0) {c.get(Calendar.DAY_OF_MONTH) == spec.dom} else true) &&
        (if (spec.dow >= 0) {c.get(Calendar.DAY_OF_WEEK) == spec.getCalendarDow()} else true)
    })
  }
}
