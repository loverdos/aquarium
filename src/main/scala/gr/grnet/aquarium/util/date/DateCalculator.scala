package gr.grnet.aquarium.util.date

import org.joda.time.{MutableDateTime, DateMidnight}
import java.util.{Date, Calendar}


/**
 * Date calculator.
 *
 * Utility class for date manipulations.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class DateCalculator private(private[this] var dateTime: MutableDateTime) {
  def this(millis: Long)  = this(new MutableDateTime(millis))
  def this(date: Date)    = this(new MutableDateTime(date))
  def this(cal: Calendar) = this(new MutableDateTime(cal))

  def this(year: Int, monthOfYear: Int, dayOfMonth: Int) =
    this(new MutableDateTime(year, monthOfYear, dayOfMonth, 0, 0, 0, 0))

  def this(year: Int, monthOfYear: Int) =
    this(year, monthOfYear, 1)

  def nextMonths(n: Int): this.type = {
    dateTime.addMonths(n)

    this
  }

  def previousMonths(n: Int): this.type = {
    dateTime.addMonths(-n)

    this
  }

  def nextMonth: this.type = {
    nextMonths(1)
  }

  def previousMonth: this.type = {
    previousMonths(1)
  }

  def nextDays(n: Int): this.type = {
    dateTime.addDays(n)
    this
  }

  def previousDays(n: Int): this.type = {
    dateTime.addDays(n)
    this
  }
  
  def nextDay: this.type = {
    nextDays(1)
  }
  
  def previousDay: this.type = {
    previousDays(1)
  }

  def nextMillis(n: Long): this.type = {
    dateTime.add(n)

   this
 }

 def previousMillis(n: Long): this.type = {
   dateTime.add(-n)

   this
 }

 def nextMilli: this.type = {
   nextMillis(1L)
 }

 def previousMilli: this.type = {
   previousMillis(1L)
 }

  def year: Int = {
    dateTime.getYear
  }

  /**
   * Months range from 1 to 12.
   */
  def monthOfYear: Int = {
    dateTime.getMonthOfYear
  }

  /**
   * Month days start from 1
   */
  def dayOfMonth: Int = {
    dateTime.getDayOfMonth
  }

  /**
   * Year days start from 1
   */
  def dayOfYear: Int = {
    dateTime.getDayOfYear
  }

  /**
   * Week days start from 1, which is Sunday
   */
  def dayOfWeek: Int = {
    dateTime.getDayOfWeek
  }

  def midnight: this.type = {
    this.dateTime = new DateMidnight(dateTime).toMutableDateTime
    this
  }

  /**
   * At the first millisecond of this month. This month is the month indicated by the
   * state of the [[gr.grnet.aquarium.util.date.DateCalculator]] and not the real-life month.
   */
  def startOfThisMonth: this.type = {
    midnight
    while(dayOfMonth > 1) {
      previousDay
    }
    this
  }

  /**
   * At the first millisecond of the next month.
   */
  def startOfNextMonth: this.type = {
    nextMonth.startOfThisMonth
  }

  /**
   * At the last millisecond of the month
   */
  def endOfThisMonth: this.type = {
    startOfNextMonth.previousMilli
  }
  
  def isSameYearAndMonthAs(other: Long): Boolean = {
    isSameYearAndMonthAs(new DateCalculator(other))
  }

  def isSameYearAndMonthAs(otherDate: DateCalculator): Boolean = {
    this.year == otherDate.year && this.monthOfYear == otherDate.monthOfYear
  }
  
  def toMillis: Long = {
    dateTime.getMillis
  }

  def toDate: Date = {
    dateTime.toDate
  }
  
  def beforeMillis(millis: Long): Boolean = {
    toMillis < millis
  }

  def afterMillis(millis: Long): Boolean = {
    toMillis > millis
  }

  def beforeEqMillis(millis: Long): Boolean = {
    toMillis <= millis
  }

  def afterEqMillis(millis: Long): Boolean = {
    toMillis >= millis
  }

  override def toString = {
    dateTime.toString
  }
}