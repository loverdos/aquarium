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

import gr.grnet.aquarium.util.shortNameOfClass

import java.util.Date
import gr.grnet.aquarium.util.date.MutableDateCalc
import collection.mutable

/**
 * Represents an effectivity timeframe.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
case class DSLTimeFrame (
  from: Date,
  to: Option[Date],
  repeat: List[DSLTimeFrameRepeat] /*,
  cronRepeat: List[DSLCronSpec]*/
) {

  to match {
    case Some(x) =>
      assert(x.after(from), "Time frame to (%s) must be after from (%s)"
        .format(x.getTime, from.getTime))
    case None =>
  }

  private val start = from.getTime
  private val infinity = new Date(Long.MaxValue)
  private val end = to.getOrElse(infinity).getTime

  /*
   *  Given the timeslot "t" and the timeslot of this frame (start,end)
   *  compute the intersection of the two timeslots (return a list of common intervals)
   *  which satisfy the cron expressions. If no expressions are present which just
   *  take the intersection. If there exist cron specs (cron,start) we compute the
   *  intersection between "t" and the fine-grained subtimeslots (of this time frame).
   *
   *  */
  def intervalsOf(t:Timeslot) : List[Timeslot]=
    if(repeat.isEmpty)
      List(t) // .overlappingTimeslots(List(Timeslot(start,end)))
    else {
      val result = new mutable.ListBuffer[Timeslot]()
      var offset = t.from
      firstValidTimeslot(t.from) match {
        case None => ()
        case Some(t) =>
          result += t
          offset = t.to
      }
      val step = 1000L*60L // +1 minute step
      var continue = true
      while(continue)
        minValidAfter(t,offset) match {
          case None =>
            continue = false
          case Some(next_timeslot) =>
            result += next_timeslot
            offset =  next_timeslot.to
        }
      result.toList
    }
    /*nextValidAfter(t.from) match {
      case None =>
        val ret = addRest(t.from)
        ret
      case Some(d_next) =>
        if(d_next.after(t.to)){
          if(repeat.isEmpty)
            List(t)
          else
            Nil
        }
        else {
          val ret =  Timeslot(t.from,d_next) :: addRest(d_next)
          ret
        }
    } */

  /*
   * Find the first smallest (smallest here means with the least "end" timestamp) timeslot
   * from beginning "offset" and ending at "d_end".
   *
   * Return None if "offset" is not bounded in this time frame and is not bounded by
   * a valid "start" / "end" cron spec.
   *
   * */
   private def firstValidTimeslot(offset:Date) : Option[Timeslot] = {
    def minimum (min:Option[Timeslot],d_start:Date,d_end:Date) : Option[Timeslot] = {
      min match {
        case None =>
          Some(Timeslot(d_start,d_end))
        case Some(Timeslot(_,d_min)) =>
          if(d_min.getTime < d_start.getTime)
            min
          else
            Some(Timeslot(d_start,d_end))
      }
    }
    val to = this.to.getOrElse(infinity)
    repeat.foldLeft (None:Option[Timeslot]) {
      (min,r)  =>
        r.getEnd.nextValidDate(from,to,offset) match {
          case None =>
            min
          case Some(d_end) =>
            r.getStart.nextValidDate(from,to,from) match {
              case None => /* there is no valid starting date in the entire interval!*/
                  min
              case Some(_) =>  /**/
                r.getStart.nextValidDate(from,to,offset) match {
                  case None => /* so there is at least one starting date before offset
                                  and there are no starting dates after. So we are inside
                                  an interval*/
                    if(from.getTime <= offset.getTime && offset.getTime <= to.getTime)
                       minimum(min,offset,d_end)
                    else
                       min
                  case Some(d_start) =>
                     if(d_start.getTime > d_end.getTime) {
                        /* there is another starting date which occurs after the ending date.
                        *  So we are within the interval*/
                       minimum(min,offset,d_end)
                     } else { /* we are outside the interval*/
                       min
                     }
                }
            }
     }
   }
  }


  private def minValidAfter(t:Timeslot,offset:Date) : Option[Timeslot] =
       repeat.foldLeft  (None:Option[Timeslot]) {
        (min,r)  =>
          r.getStart.nextValidDate(t.from,t.to,offset) match {
            case None =>
              min
            case Some(d_start) =>
              r.getEnd.nextValidDate(t.from,t.to,d_start) match {
                case None =>
                  min
                case Some(d_end) =>
                  min match {
                    case None =>
                      Some(Timeslot(d_start,d_end))
                    case Some(Timeslot(d_min,_)) =>
                      if(d_min.getTime < d_start.getTime)
                        min
                      else
                        Some(Timeslot(d_start,d_end))
                  }
              }
          }
     }


  override def toString =
    //"%s(%s, %s,\n %s\n || cron: %s)".format(
    "%s(%s, %s,%s)".format(
    shortNameOfClass(classOf[DSLTimeFrame]),
    new MutableDateCalc(from).toString,
    to.map(t => new MutableDateCalc(t)),
    repeat /*,
    cronRepeat*/
  )

  /*def includes(d :Date) : Boolean = {
    val now = d.getTime
    start <= now && now <= end && (cronRepeat.isEmpty || (cronRepeat.exists {_ occurs d}))
  } */

  /*/*  Return the next VALID, MAXIMUM and CONTINUOUS date after "d" for this time frame.
   *  The *returned time* is the end of the time frame if there exist no cron specs.
   *  Otherwise, the returned time is maximum of the dates that
   *  1. are continuous (no interruptions) from "d"
   *  2. occur after "d"
   *  3. belong in the time frame
   *
   *  The input parameter "d" must occur within the time frame. If there exist
   *  cron specs "d" must belong in the time spec specified by cron.
   */
  def nextValidAfter1(d: Date) : Option[Date] = {
    val now = d.getTime
    if (now < start || now > end) None /* undefined valid is date not contained here*/
    else if (cronRepeat.isEmpty) Some(to.getOrElse(infinity)) /* No specs! The next valid date is the end of time frame*/
    else {
      /* filter out cron specs that DO NOT contain d */
      val includes_d = cronRepeat filter { _ includes d }
    /* get the next CONTINUOUS valid date from "d" using the DSLCronSpec class */
      val next_cont_d =  includes_d  map {_ nextContinuousValidDate d}
      /* filter the resulting dates "d1" so that they occur after "d" and are within the timeframe bounds*/
      val filter_invalid = next_cont_d filter {d1:Date =>  d1 != null &&
                                                           d1.getTime > now &&
                                                           start <= d1.getTime &&
                                                           d1.getTime <= end}
      /* sort the next dates and select the MAXIMUM one*/
      val sorted = filter_invalid  sortWith {_.getTime > _.getTime}
      sorted match {
        case Nil => None /* Nothing interesting was found*/
        case hd::_ => if(hd!=null) Some(hd) else None  /* the MAXIMAL next date */
      }
    }
  }
  var time_var : List[Long] = Nil
  private def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    time_var =  ((t1-t0)/1000000L) :: time_var
    //Console.err.println("Time__xxxx: " + time_var)
    result
  } */
  /*/* the nextValidDate cannot be outside the end limit of this time frame
     Description: given date "d" get the nextValidDate after "d"
   * */
  def nextValidAfter(d:Date) : Option[Date] = {
     def nextDate(r:DSLTimeFrameRepeat) : Option[Date] =
       time(r.getStart.getMaxValidBeforeDate(from,to.getOrElse(infinity),d)) match {
         case None =>
           None
         case Some(min) =>
           r.getEnd.nextValidDate(from,to.getOrElse(infinity),min) match {
             case None =>
               None
             case Some(d_max) =>
               if(d_max.getTime < d.getTime)
                 None
               else
                 Some(d_max)
           }

       }
    if(repeat.isEmpty){
      val d_time = d.getTime
      if(d_time < start || d_time > end) None
      else Some(to.getOrElse(infinity))
    }
    else {
      var tmpDate : Option[Date] = null
      repeat.collectFirst {
        case r  if({tmpDate=nextDate(r);tmpDate} != None) => tmpDate.get
      }
    }
  }*/
}

object DSLTimeFrame{
  val emptyTimeFrame = DSLTimeFrame(new Date(0), None, List())//,List())
}