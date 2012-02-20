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

import java.util.Date
import scala.collection.mutable
import annotation.tailrec

/**
 * A representation of a timeslot with a start and end date.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
final case class Timeslot(from: Date, to: Date)
  extends DSLItem with Ordered[Timeslot] {

  /* Preconditions to ensure correct object creation */
  assert(from != null, "From cannot be null")
  assert(to != null, "From cannot be null")
  assert(from.before(to), "Timeslot to is before from")

  def startsBefore(t: Timeslot) : Boolean = this.from.before(t.from)

  def startsAfter(t: Timeslot) : Boolean = this.from.after(t.from)

  def endsBefore(t: Timeslot) : Boolean = this.to.before(t.to)

  def endsAfter(t: Timeslot) : Boolean = this.to.after(t.to)

  def after(t: Timeslot): Boolean = if (this.from.after(t.to)) true else false

  def before(t: Timeslot): Boolean = if (this.to.before(t.from)) true else false

  /**
   * Check whether this time slot fully contains the provided one.
   */
  def contains(t: Timeslot) : Boolean = t.startsAfter(this) && t.endsBefore(this)

  /**
   * Check whether this timeslot contains the provided time instant.
   */
  def includes(t: Date) : Boolean =
    if (from.before(t) && to.after(t)) true else false

  /**
   * Check whether this timeslot overlaps with the provided one.
   */
  def overlaps(t: Timeslot) : Boolean = {
    if (contains(t) || t.contains(this))
      return true

    if (this.includes(t.from) || this.includes(t.to))
      return true

    false
  }

  /**
   * Merges this timeslot with the provided one. If the timeslots overlap,
   * a list with the resulting merge is returned. If the timeslots do not
   * overlap, the returned list contains both timeslots in increasing start
   * date order.
   */
  def merge(t: Timeslot) : List[Timeslot] = {
    if (overlaps(t)) {
      val nfrom = if (from.before(t.from)) from else t.from
      val nto   = if (to.after(t.to)) to else t.to
      List(Timeslot(nfrom, nto))
    } else
      if (this.from.before(t.from))
        List(this, t)
      else
        List(t, this)
  }

  /**
   * Split the timeslot in two parts at the provided timestamp, if the
   * timestamp falls within the timeslot boundaries.
   */
  def slice(d: Date) : List[Timeslot] =
    if (includes(d))
      List(Timeslot(from, d), Timeslot(d,to))
    else
      List(this)

  /**
   * Find and return the timeslots within which this Timeslot overrides
   * with the provided list of timeslots. For example if:
   * 
   *  - `this == Timeslot(7,20)`
   *  - `list == List(Timeslot(1,3), Timeslot(6,8), Timeslot(11,15))`
   *
   * the result will be: `List(Timeslot(7,8), Timeslot(11,15))`
   */
  def overlappingTimeslots(list: List[Timeslot]) : List[Timeslot] = {

    val result = new mutable.ListBuffer[Timeslot]()

    list.foreach {
      t =>
        if (t.contains(this)) result += this
        else if (this.contains(t)) result += t
        else if (t.overlaps(this) && t.startsBefore(this)) result += this.slice(t.to).head
        else if (t.overlaps(this) && t.startsAfter(this)) result += this.slice(t.from).last
    }
    result.toList
  }

  /**
   * Find and return the timeslots whithin which this Timeslot does not
   * override with the provided list of timeslots. For example if:
   *
   *  - `this == Timeslot(7,20)`
   *  - `list == List(Timeslot(1,3), Timeslot(6,8), Timeslot(11,15))`
   *
   * the result will be: `List(Timeslot(9,10), Timeslot(15,20))`
   */
  def nonOverlappingTimeslots(list: List[Timeslot]): List[Timeslot] = {

    val overlaps = list.filter(t => this.overlaps(t))

    if (overlaps.isEmpty)
      return List(this)

    def build(acc: List[Timeslot], listPart: List[Timeslot]): List[Timeslot] = {

      listPart match {
        case Nil => acc
        case x :: Nil => build(acc, List())
        case x :: y :: rest =>
          build(acc ++ List(Timeslot(x.to,  y.from)), y :: rest)
      }
    }

    val head = overlaps.head
    val last = overlaps.reverse.head

    val start = if (head.startsAfter(this)) List(Timeslot(this.from, head.from)) else List()
    val end = if (last.endsBefore(this)) List(Timeslot(last.to, this.to)) else List()

    start ++ build(List(), overlaps) ++ end
  }

  /**
   * Align a list of consecutive timeslots to the boundaries
   * defined by this timeslot. Elements that do not overlap
   * with this timeslot are rejected, while elements not
   * contained in the timeslot are trimmed to this timeslot's
   * start and end time.
   */
  def align(l: List[Timeslot]): List[Timeslot] = {
    if (l.isEmpty) return List()

    val result =
      if (!this.overlaps(l.head)) List()
      else if (this.contains(l.head)) List(l.head)
      else if (l.head.startsBefore(this)) List(Timeslot(this.from, l.head.to))
      else if (l.head.endsAfter(this)) List(Timeslot(l.head.from, this.to))
      else List(this)

    if (!result.isEmpty)
      result.head :: align(l.tail)
    else
      align(l.tail)
  }

  /**
   * Compares the starting times of two timeslots.
   */
  def compare(that: Timeslot): Int = {
    if (this.startsBefore(that)) -1
    else if (this.startsAfter(that)) 1
    else 0
  }

  /**
   * Converts the timeslot to the amount of hours it represents
   */
  def hours: Double = (to.getTime - from.getTime).toDouble / 1000.0 / 60.0 / 60.0

  override def toString() = "from: %d, to: %d".format(from.getTime, to.getTime)
}