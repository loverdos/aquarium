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

/**
 * Represents an effectivity timeframe.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
case class DSLTimeFrame (
  from: Date,
  to: Option[Date],
  repeat: List[DSLTimeFrameRepeat]
) extends DSLItem {

  to match {
    case Some(x) =>
      assert(x.after(from), "Time frame to (%s) must be after from (%s)"
        .format(x.getTime, from.getTime))
    case None =>
  }

  override def toMap(): Map[String, Any] = {
    val toTS = to match {
      case Some(x) => Map(Vocabulary.to -> x.getTime)
      case _ => Map()
    }

    val repeatMap = if (repeat.size > 0) {
      Map(Vocabulary.repeat -> repeat.map{r => r.toMap})
    } else {
      Map()
    }

    toTS ++
    Map(Vocabulary.from -> from.getTime) ++
    repeatMap
  }

  override def toString = "%s(%s, %s, %s)".format(
    shortNameOfClass(classOf[DSLTimeFrame]),
    new MutableDateCalc(from).toString,
    to.map(t => new MutableDateCalc(t)),
    repeat
  )
}

object DSLTimeFrame{
  val emptyTimeFrame = DSLTimeFrame(new Date(0), None, List())
}