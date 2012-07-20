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

package gr.grnet.aquarium.charging

import gr.grnet.aquarium.util.shortClassNameOf
import gr.grnet.aquarium.util.date.TimeHelpers.toYYYYMMDDHHMMSSSSS

/**
 * A credit value computed for a particular time period, using a specific unit price.
 *
 * @param startMillis
 * @param stopMillis
 * @param unitPrice
 * @param creditsToSubtract The computed amount of credits to subtract from the total user credits. If this is
 *                          an infinite number, then the convention is that no credits have been specified.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class Chargeslot(
    startMillis: Long,
    stopMillis: Long,
    unitPrice: Double,
    explanation: String = "",
    creditsToSubtract: Double = Double.NaN
) {

  def hasCreditsToSubtract: Boolean = {
    !creditsToSubtract.isInfinite
  }

  def copyWithCreditsToSubtract(credits: Double, _explanation: String) = {
    copy(creditsToSubtract = credits, explanation = _explanation)
  }

  override def toString = "%s(%s, %s, %s, %s, %s)".format(
    shortClassNameOf(this),
    toYYYYMMDDHHMMSSSSS(startMillis),
    toYYYYMMDDHHMMSSSSS(stopMillis),
    unitPrice,
    explanation,
    creditsToSubtract
  )
}
