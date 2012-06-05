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

package gr.grnet.aquarium.computation

import gr.grnet.aquarium.util.date.MutableDateCalc
import gr.grnet.aquarium.util.shortClassNameOf

/**
 * Provides information about the billing month and related calculation utilities.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

final class BillingMonthInfo(
    /**
     * The billing year.
     */
    final val year: Int,

    /**
     * The billing month, in the range from 1 to 12.
     */
    final val month: Int) extends Ordered[BillingMonthInfo] {

  final val (monthStartMillis, monthStopMillis) = {
    val mdc = new MutableDateCalc(year, month, 1)
    (
      mdc.goStartOfThisMonth.getMillis,
      mdc.goEndOfThisMonth.getMillis // no need to `copy` here, since we are discarding `mdc`
    )
  }

  def previousMonth: BillingMonthInfo = {
    BillingMonthInfo.fromDateCalc(new MutableDateCalc(year, month).goPreviousMonth)
  }

  def nextMonth: BillingMonthInfo = {
    BillingMonthInfo.fromDateCalc(new MutableDateCalc(year, month).goNextMonth)
  }


  def compare(that: BillingMonthInfo) = {
    val ds = this.monthStartMillis - that.monthStartMillis
    if(ds < 0) -1 else if(ds == 0) 0 else 1
  }


  override def equals(any: Any) = any match {
    case BillingMonthInfo(thatYear, thatMonth) ⇒
      this.year == thatYear && this.month == thatMonth // normally everything else MUST be the same by construction
    case _ ⇒
      false
  }

  override def hashCode() = {
    31 * year + month
  }

  override def toString = "%s(%s-%02d-01)".format(shortClassNameOf(this), year, month)

  def toShortDebugString = "%s-%02d-01".format(year, month)
}

object BillingMonthInfo {
  def fromMillis(millis: Long): BillingMonthInfo = {
    fromDateCalc(new MutableDateCalc(millis))
  }

  def fromDateCalc(mdc: MutableDateCalc): BillingMonthInfo = {
    val year = mdc.getYear
    val month = mdc.getMonthOfYear

    new BillingMonthInfo(year, month)
  }

  def unapply(bmi: BillingMonthInfo): Option[(Int, Int)] = {
    Some((bmi.year, bmi.month))
  }
}
