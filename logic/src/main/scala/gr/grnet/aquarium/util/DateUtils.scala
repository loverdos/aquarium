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

package gr.grnet.aquarium.util

import scala.collection.mutable
import java.util.{Calendar, Date, GregorianCalendar}

/**
 * Various utils for manipulating dates, with special
 * emphasis on credit DSL requirements.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait DateUtils {


  /**
   * Determines whether a time instant is contained within the
   * provided timeslot.
   */
  def contains(timeslot: (Date, Date), moment: Date): Boolean = {

    if (timeslot._1.before(moment) && timeslot._2.after(moment))
      return true

    false
  }

  /**
   * Determines whether timeslot1 can fully contain timeslot2
   */
  def contains(timeslot1: (Date, Date), timeslot2: (Date, Date)): Boolean = {
    if (timeslot1._1.after(timeslot2._1))
      return false

    if (timeslot1._2.before(timeslot2._2))
      return false

    true
  }

  /**
   * Search within
   */
  def findDays(from: Date, to: Date, f: Calendar => Boolean) : List[Date] = {
    val c = new GregorianCalendar()
    val result = new mutable.ListBuffer[Date]()

    c.setTime(from)

    while (c.getTime().getTime <= to.getTime) {
      if (f(c))
        result += new Date(c.getTime.getTime)
      c.add(Calendar.DAY_OF_YEAR, 1)
    }

    result.toList
  }

  /**
   * Adjust time in the provided date to the provided values
   */
  def adjustToTime(d: Date, h: Int,  m: Int): Date = {

    assert((0 <= h) && (h <= 23))
    assert((0 <= m) && (m <= 59))

    val c = new GregorianCalendar()
    c.setTime(d)
    c.roll(Calendar.MINUTE, m - c.get(Calendar.MINUTE))
    c.roll(Calendar.HOUR_OF_DAY, h - c.get(Calendar.HOUR_OF_DAY))
    c.getTime
  }
}