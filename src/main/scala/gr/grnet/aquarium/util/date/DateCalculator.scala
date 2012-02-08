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

import org.joda.time.{MutableDateTime, DateMidnight}
import java.util.{Date, Calendar}
import org.joda.time.format.ISODateTimeFormat


/**
 * Mutable date calculator.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class DateCalculator private(private[this] var dateTime: MutableDateTime) extends Cloneable {
  def this(millis: Long)  = this(new MutableDateTime(millis))
  def this(date: Date)    = this(new MutableDateTime(date))
  def this(cal: Calendar) = this(new MutableDateTime(cal))

  def this(year: Int, monthOfYear: Int, dayOfMonth: Int) =
    this(new MutableDateTime(year, monthOfYear, dayOfMonth, 0, 0, 0, 0))

  def this(year: Int, monthOfYear: Int) =
    this(year, monthOfYear, 1)


  override def clone(): DateCalculator = new DateCalculator(this.dateTime.clone().asInstanceOf[MutableDateTime])

  def copy: DateCalculator = clone()

  def <=(other: DateCalculator): Boolean = {
    getMillis <= other.getMillis
  }

  def <=(other: Date): Boolean = {
    getMillis <= other.getTime
  }

  def <=(other: Long): Boolean = {
    getMillis <= other
  }

  def <(other: DateCalculator): Boolean = {
    getMillis < other.getMillis
  }

  def <(other: Date): Boolean = {
    getMillis < other.getTime
  }

  def <(other: Long): Boolean = {
    getMillis < other
  }

  def >=(other: DateCalculator): Boolean = {
    getMillis >= other.getMillis
  }

  def >=(other: Date): Boolean = {
    getMillis >= other.getTime
  }

  def >=(other: Long): Boolean = {
    getMillis >= other
  }

  def >(other: DateCalculator): Boolean = {
    getMillis >= other.getMillis
  }

  def >(other: Date): Boolean = {
    getMillis > other.getTime
  }

  def >(other: Long): Boolean = {
    getMillis > other
}

  def plusMonths(n: Int): this.type = {
    dateTime.addMonths(n)

    this
  }

  def goMinusMonths(n: Int): this.type = {
    dateTime.addMonths(-n)

    this
  }

  def goNextMonth: this.type = {
    plusMonths(1)
  }

  def goPreviousMonth: this.type = {
    goMinusMonths(1)
  }

  def goPlusDays(n: Int): this.type = {
    dateTime.addDays(n)
    this
  }

  def goMinusDays(n: Int): this.type = {
    dateTime.addDays(n)
    this
  }
  
  def goNextDay: this.type = {
    goPlusDays(1)
  }
  
  def goPreviousDay: this.type = {
    goMinusDays(1)
  }

  def goPlusSeconds(n: Int): this.type = {
    dateTime.addSeconds(n)
    this
  }

  def goMinusSeconds(n: Int): this.type = {
    goPlusSeconds(-n)
  }

  def goPlusHours(n: Int): this.type = {
    dateTime.addHours(n)
    this
  }

  def goMinusHours(n: Int): this.type = {
    goPlusHours(-n)
  }

  def goPlusMinutes(n: Int): this.type = {
    dateTime.addMinutes(n)
    this
  }

  def goMinusMinutes(n: Int): this.type = {
    goPlusMinutes(-n)
  }

  def goPlusMillis(n: Long): this.type = {
    dateTime.add(n)
    this
  }

 def goMinusMillis(n: Long): this.type = {
   goPlusMillis(-n)
 }

 def goNextMilli: this.type = {
   goPlusMillis(1L)
 }

 def goPreviousMilli: this.type = {
   goMinusMillis(1L)
 }

  def getYear: Int = {
    dateTime.getYear
  }

  /**
   * Months range from 1 to 12.
   */
  def getMonthOfYear: Int = {
    dateTime.getMonthOfYear
  }

  /**
   * Month days start from 1
   */
  def getDayOfMonth: Int = {
    dateTime.getDayOfMonth
  }

  /**
   * Year days start from 1
   */
  def getDayOfYear: Int = {
    dateTime.getDayOfYear
  }

  /**
   * Week days start from 1, which is Sunday
   */
  def getDayOfWeek: Int = {
    dateTime.getDayOfWeek
  }

  def goMidnight: this.type = {
    this.dateTime = new DateMidnight(dateTime).toMutableDateTime
    this
  }

  /**
   * Go to the first millisecond of this month. This month is the month indicated by the
   * state of the [[gr.grnet.aquarium.util.date.DateCalculator]] and not the real-life month.
   */
  def goStartOfThisMonth: this.type = {
    goMidnight
    while(getDayOfMonth > 1) {
      goPreviousDay
    }
    this
  }

  /**
   * Go to the first millisecond of the next month.
   */
  def goStartOfNextMonth: this.type = {
    goNextMonth.goStartOfThisMonth
  }

  /**
   * Go to the first millisecond of the previous month.
   */
  def goStartOfPreviousMongth: this.type = {
    goPreviousMonth.goStartOfThisMonth
  }

  /**
   * Go to the last millisecond of the month
   */
  def goEndOfThisMonth: this.type = {
    goStartOfNextMonth.goPreviousMilli
  }
  
  def isSameYearAndMonthAs(other: Long): Boolean = {
    isSameYearAndMonthAs(new DateCalculator(other))
  }

  def isSameYearAndMonthAs(otherDate: DateCalculator): Boolean = {
    this.getYear == otherDate.getYear && this.getMonthOfYear == otherDate.getMonthOfYear
  }
  
  def toMillis: Long = {
    dateTime.getMillis
  }

  def getMillis: Long = {
    dateTime.getMillis
  }

  def toDate: Date = {
    dateTime.toDate
  }
  
  def isBeforeMillis(millis: Long): Boolean = {
    toMillis < millis
  }

  def isAfterMillis(millis: Long): Boolean = {
    toMillis > millis
  }

  def isBeforeEqMillis(millis: Long): Boolean = {
    toMillis <= millis
  }

  def isAfterEqMillis(millis: Long): Boolean = {
    toMillis >= millis
  }

  def format(fmt: String) = {
    dateTime.formatted(fmt)
  }

  def toISOString: String = {
    ISODateTimeFormat.dateTime().print(dateTime);
  }

  override def toString = {
    dateTime.toString("yyyy-MM-dd HH:mm:ss.SSS")
  }
}