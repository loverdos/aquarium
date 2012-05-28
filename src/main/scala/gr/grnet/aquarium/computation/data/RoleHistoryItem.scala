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

package gr.grnet.aquarium.computation.data

import gr.grnet.aquarium.util.date.MutableDateCalc
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.Aquarium

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class RoleHistoryItem(
    /**
     * The role name.
     */
    name: String,

    /**
     * Validity start time for this role. The time is inclusive.
     */
    validFrom: Long,

    /**
     * Validity stop time for this role. The time is exclusive.
     */
    validTo: Long = Long.MaxValue) {

  try {
    require(
      validFrom <= validTo,
      "validFrom(%s) <= validTo(%s)".format(new MutableDateCalc(validFrom), new MutableDateCalc(validTo)))
  }
  catch {
    case e: IllegalArgumentException ⇒
      Aquarium.Instance.debug(this, "!! validFrom = %s, validTo = %s, dx=%s", validFrom, validTo, validTo-validFrom)
      throw e
  }

  require(name ne null, "Name is not null")

  require(!name.trim.isEmpty, "Name '%s' is not empty".format(name))

  def timeslot = Timeslot(validFrom, validTo)

  def copyWithValidTo(newValidTo: Long) = copy(validTo = newValidTo)

  def isUpperBounded = {
    validTo != Long.MaxValue
  }

  def contains(time: Long) = {
    validFrom <= time && time < validTo
  }

  def isStrictlyAfter(time: Long) = {
    validFrom > time
  }

  override def toString =
    "RoleHistoryItem(%s, [%s, %s))".
      format(name, new MutableDateCalc(validFrom), new MutableDateCalc(validTo))
}