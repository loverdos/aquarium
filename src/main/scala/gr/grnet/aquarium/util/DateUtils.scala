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

package gr.grnet.aquarium.util

import scala.collection.mutable
import java.util.{Calendar, Date, GregorianCalendar}
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot

/**
 * Various utils for manipulating dates, with special
 * emphasis on credit DSL requirements.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait DateUtils extends Loggable {

  /**
   * Returns a date that is equal to the latest of
   * (`d - 1 year`, `limit`)
   */
  def oneYearBack(d: Date, limit: Date): Date = {
    val c = new GregorianCalendar()
    c.setTime(d)
    c.add(Calendar.YEAR, -1)
    if (c.getTime.before(limit))
      limit
    else
      c.getTime
  }

  /**
   * Returns a date that is equal to the earliest of
   * (`d + 1 year`, `limit`)
   */
  def oneYearAhead(d: Date, limit: Date): Date = {
    val c = new GregorianCalendar()
    c.setTime(d)
    c.add(Calendar.YEAR, 1)
    if (c.getTime.before(limit))
      c.getTime
    else
      limit
  }
}
