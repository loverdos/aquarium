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

import gr.grnet.aquarium.util.yaml.YAMLHelpers

/**
 * 
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
case class DSLTimeFrameRepeat (
  start: List[DSLTimeSpec],
  end: List[DSLTimeSpec],
  startCron: String,
  endCron: String
) extends DSLItem {
  assert(start.size == end.size)

  //Ensure that fields that have repeating entries, do so in both patterns
  start.zip(end).foreach {
    x =>
      assert((x._1.dom == -1 && x._2.dom == -1) ||
        (x._1.dom != -1 && x._2.dom != -1))

      assert((x._1.mon == -1 && x._2.mon == -1) ||
        (x._1.mon != -1 && x._2.mon != -1))

      assert((x._1.dow == -1 && x._2.dow == -1) ||
        (x._1.dow != -1 && x._2.dow != -1))
  }

  override def toMap(): Map[String, Any] = {
    val data = new scala.collection.mutable.HashMap[String, Any]()
    
    data += (Vocabulary.start -> startCron)
    data += (Vocabulary.end -> endCron)

    data.toMap
  }
}

object DSLTimeFrameRepeat {
  val emptyTimeFramRepeat = DSLTimeFrameRepeat(List(), List(), "", "")
}