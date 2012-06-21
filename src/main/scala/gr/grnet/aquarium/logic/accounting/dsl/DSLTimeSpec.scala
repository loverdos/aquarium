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

import java.util.{GregorianCalendar, Date, Calendar}
import collection.mutable

/**
 * Represents an instance of an expanded cronstring declaration. Enforces,
 * at object creation time, the following conditions:
 *
 *  - 0 < `min` < 60
 *  - 0 < `hour` < 24
 *  - -1 < `dom` < 31 and `dom` not equal to 0
 *  - -1 < `mon` < 12 and `mon` not equal to 0
 *  - -1 < `dow` < 7
 * 
 * A value of -1 for the fields `dom`,`mon` and `dow` means that the defined
 * time moment can be repeated within a timeframe.
 * `min` and `hour` fields cannot be used to define repetitive time moments.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
  case class DSLTimeSpec(
    min: Int,
    hour: Int,
    dom: Int,
    mon: Int,
    dow: Int
  ) extends DSLItem {
  //Preconditions to force correct values on object creation
  assert(0 <= min && 60 > min)
  assert(0 <= hour && 24 > hour)
  assert(-1 <= dom && 31 > dom && dom != 0)
  assert(-1 <= mon && 12 > mon && mon != 0)
  assert(-1 <= dow && 7 > dow)


  def expandTimeSpec(from: Date,  to: Date) : List[Date] = {
    val c = new GregorianCalendar()
    c.setTime { /*adjust time given from "from" variable  */
      c.setTime(from)
      c.set(Calendar.MINUTE,this.min)
      c.set(Calendar.HOUR_OF_DAY,this.hour)
      /*if(c.getTimeInMillis < from.getTime) {
        //c.add(Calendar.DAY_OF_YEAR, 1)
       // assert(c.getTimeInMillis >= from.getTime,c.getTime + " >=" + from) /* sanity check */
      }*/
      c.getTime
   }
   def equals () : Boolean =
      (this.mon < 0  || c.get(Calendar.MONTH) == this.getCalendarMonth()) &&
      (this.dom < 0  || c.get(Calendar.DAY_OF_MONTH) == this.dom) &&
      (this.dow < 0  || c.get(Calendar.DAY_OF_WEEK) == this.getCalendarDow())

    val result = new mutable.ListBuffer[Date]()
    //Console.err.println("\n\nBEGIN\n\n")
    while (c.getTimeInMillis <= to.getTime) {
      val b : Boolean = equals
      //Console.err.println(c.getTime +  "\t\t"  + to + " ==>" + (if(b) "YES" else "NO"))
      if (b) result += new Date(c.getTime.getTime)
      c.add(Calendar.DAY_OF_YEAR, 1)
    }
    //Console.err.println("\n\nEND\n\n")
    result.toList
  }


  /** Day of week conversions to stay compatible with [[java.util.Calendar]] */
  private def getCalendarDow(): Int = dow match {
    case 0 => Calendar.SUNDAY
    case 1 => Calendar.MONDAY
    case 2 => Calendar.TUESDAY
    case 3 => Calendar.WEDNESDAY
    case 4 => Calendar.THURSDAY
    case 5 => Calendar.FRIDAY
    case 6 => Calendar.SATURDAY
    case 7 => Calendar.SUNDAY
  }

  /** Month conversions to stay compatible with [[java.util.Calendar]] */
  private def getCalendarMonth(): Int = mon match {
    case 1 => Calendar.JANUARY
    case 2 => Calendar.FEBRUARY
    case 3 => Calendar.MARCH
    case 4 => Calendar.APRIL
    case 5 => Calendar.MAY
    case 6 => Calendar.JUNE
    case 7 => Calendar.JULY
    case 8 => Calendar.AUGUST
    case 9 => Calendar.SEPTEMBER
    case 10 => Calendar.OCTOBER
    case 11 => Calendar.NOVEMBER
    case 12 => Calendar.DECEMBER
  }
}

object DSLTimeSpec {
  val emtpyTimeSpec = DSLTimeSpec(0, 0, 0, 0, 0)
}