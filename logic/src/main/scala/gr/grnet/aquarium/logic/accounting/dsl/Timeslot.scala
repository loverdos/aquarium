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

/**
 * A representation of a timeslot with a start and end date.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
case class Timeslot(from: Date, to: Date) {

  /* Preconditions to ensure correct object creations */
  assert(from != null)
  assert(to != null)
  assert(from.before(to))

  /**
   * Check whether this time slot fully contains the provided one.
   */
  def contains(t: Timeslot) : Boolean = {
    if (this.from.after(t.from))
      return false

    if (this.to.before(t.to))
      return false

    true
  }

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
}