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

import java.util.Date
import scala.collection.mutable
import annotation.tailrec
import gr.grnet.aquarium.util.date.MutableDateCalc
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot

/**
 * A representation of a timeslot with a start and end date.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
final case class Timeslot(from: Date, to: Date)
  extends DSLItem with Ordered[Timeslot] {

  /* Preconditions to ensure correct object creations */
  assert(from != null)
  assert(to != null)
  assert(from.before(to), "from = %s, to = %s".format(new MutableDateCalc(from), new MutableDateCalc(to)))

  def startsBefore(t: Timeslot) : Boolean =  start < t.start

  def startsAfter(t: Timeslot) : Boolean =   start > t.start

  def endsBefore(t: Timeslot) : Boolean = end < t.end

  def endsAfter(t: Timeslot) : Boolean =  end > t.end

  def after(t: Timeslot): Boolean =  start > t.end

  def before(t: Timeslot): Boolean = end < t.start

  def start : Long =  this.from.getTime

  def end : Long =  this.to.getTime

  /**
   * Check whether this time slot fully contains the provided one.
   */
  def contains(t: Timeslot) : Boolean = this.start <= t.start && this.end >= t.end


  def containsTimeInMillis(millis: Long) =  start <= millis && millis <= end


  /**
   * Check whether this timeslot contains the provided time instant.
   */
  private def includes(t: Date) : Boolean = start <= t.getTime &&  t.getTime <= end


  /**
   * Check whether this timeslot overlaps with the provided one.
   */
  def overlaps(t: Timeslot) : Boolean =
    contains(t) || t.contains(this) || this.includes(t.from) || this.includes(t.to)


  /**
   * Merges this timeslot with the provided one. If the timeslots overlap,
   * a list with the resulting merge is returned. If the timeslots do not
     * overlap, the returned list contains both timeslots in increasing start
   * date order.
   */
  def merge(t: Timeslot) : Timeslot  = {
   assert(overlaps(t),this +" has no overlap with " + t)
   val nfrom = if (start < t.start) from else t.from
   val nto   = if (end > t.end) to else t.to
   Timeslot(nfrom, nto)
  }

  /**
   * Split the timeslot in two parts at the provided timestamp, if the
   * timestamp falls within the timeslot boundaries.
   */
   def slice(d: Date) : List[Timeslot] =
    if (includes(d) && d.getTime != start && d.getTime != end)
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
  def overlappingTimeslots(list: List[Timeslot]) : List[Timeslot] =
    list.foldLeft(List[Timeslot]()) { (ret,t) =>
      if (t.contains(this)) this :: ret
      else if (this.contains(t)) t :: ret
      else if (t.overlaps(this) && t.startsBefore(this)) slice(t.to).head :: ret
      else if (t.overlaps(this) && t.startsAfter(this))  slice(t.from).last :: ret
      else ret
    }.reverse


  /**
   * Find and return the timeslots whithin which this Timeslot does not
   * override with the provided list of timeslots. For example if:
   *
   *  - `this == Timeslot(7,20)`
   *  - `list == List(Timeslot(1,3), Timeslot(6,8), Timeslot(11,15))`
   *
   * the result will be: `List(Timeslot(9,10), Timeslot(15,20))`
   */
  def nonOverlappingTimeslots(list: List[Timeslot]): List[Timeslot] =
    overlappingTimeslots(list) sortWith {_.start < _.start} match  {
      case Nil => List(this)
      case over =>
        val (head,last) = (over.head,over.last)
        val hd = if (head.start > this.start) List(Timeslot(this.from, head.from)) else List()
        val tl = if (last.end < this.end) List(Timeslot(last.to, this.to)) else List()
        hd ++ over.tail.foldLeft((List[Timeslot](),over.head)) {
          case ((l,x),y) => (l ++ List(Timeslot(x.to,  y.from)),y)
        }._1  ++ tl
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

    val result : Option[Timeslot] =
      if (!this.overlaps(l.head)) None
      else if (l.head.contains(this)) Some(this)
      else if (l.head.startsBefore(this)) Some(Timeslot(this.from, l.head.to))
      else if (l.head.endsAfter(this)) Some(Timeslot(l.head.from, this.to))
      else Some(this)

    result match {
      case Some(x) => x :: align(l.tail)
      case None => align(l.tail)
    }
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

  def deltaMillis = to.getTime - from.getTime


  def myString : String = "Timeslot(" + this.start + "," + this.end + ")"
  //override def toString() = myString
  override def toString() = toDateString

  def toDateString = "Timeslot(%s, %s)".format(new MutableDateCalc(from), new MutableDateCalc(to))
  def toISODateString = "Timeslot(%s, %s)".format(new MutableDateCalc(from).toISOString, new MutableDateCalc(to).toISOString)
}

object Timeslot {
  def apply(x: Long, y: Long): Timeslot =
    new Timeslot(new Date(x), new Date(y))

  def apply(x: Int, y: Int): Timeslot =
    new Timeslot(new Date(x), new Date(y))
}