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

package gr.grnet.aquarium.util.date

import java.util.Date
import org.joda.time.MutableDateTime


/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

final object TimeHelpers {
  @inline
  final def nowMillis() = System.currentTimeMillis()

  @inline
  final def nowDate = new Date(nowMillis())

  def secDiffOfMillis(ms0: Long, ms1: Long) = (ms1 - ms0).toDouble / 1000.0

  def timed[U](f: ⇒ U): (Long, Long, U) = {
    val ms0 = nowMillis()
    val u = f
    val ms1 = nowMillis()
    (ms0, ms1, u)
  }

  def toYYYYMMDDHHMMSSSSS(millis: Long): String = {
    new MutableDateCalc(millis).toYYYYMMDDHHMMSSSSS
  }

  def calcCalendarMonthDiff(fromMillis: Long, toMillis: Long): Int = {
    val fromDate = new MutableDateTime(fromMillis)
    val toDate = new MutableDateTime(toMillis)

    val fromYear  = fromDate.getYear
    val fromMonth = fromDate.getMonthOfYear

    val toYear = toDate.getYear
    val toMonth = toDate.getMonthOfYear

    val _from = fromYear * 12 + fromMonth
    val _to = toYear * 12 + toMonth

    _to - _from
  }
}