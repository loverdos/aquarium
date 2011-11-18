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

import gr.grnet.aquarium.util.DateUtils
import java.util.{Calendar, Date}

/**
 * Utility functions to use when working with DSL types.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait DSLUtils extends DateUtils {

  /**
   * Get a list of all time periods within which a time frame is active.
   */
  def expandTimeRepeat(spec: DSLTimeFrameRepeat, from: Date, to: Option[Date]) : List[(Date, Date)] = {

    

    List()
  }


  /**
   * 
   */
  def expandTimeSpecs(spec: List[DSLTimeSpec], from: Date,  to: Date): List[Date] =
    spec.flatMap { t => expandTimeSpec(t, from, to)}

  /**
   * Get the list of time points prescribed by the provided timespec,
   * within the timeframe between from and to.
   */
  def expandTimeSpec(spec: DSLTimeSpec, from: Date,  to: Date) : List[Date] = {
    val adjusted = adjustToTime(from, spec.hour, spec.min)
    findDays(adjusted, to, {
      c =>
        (if (spec.mon >= 0) {c.get(Calendar.MONTH) == spec.getCalendarMonth()} else true) &&
        (if (spec.dom >= 0) {c.get(Calendar.DAY_OF_MONTH) == spec.dom} else true) &&
        (if (spec.dow >= 0) {c.get(Calendar.DAY_OF_WEEK) == spec.getCalendarDow()} else true)
    })
  }
}