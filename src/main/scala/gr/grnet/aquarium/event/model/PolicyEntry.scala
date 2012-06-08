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

package gr.grnet.aquarium.event.model

import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import java.util.Date
import gr.grnet.aquarium.converter.{JsonTextFormat, StdConverters}

/**
 * Store entry for serialized policy parts.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
case class PolicyEntry(
                        id: String, //SHA-1 of the provided policyYaml string
                        occurredMillis: Long, //Time this event was stored
                        receivedMillis: Long, //Does not make sense for local events -> not used
                        policyYAML: String, //The serialized policy
                        validFrom: Long, //The timestamp since when the policy is valid
                        validTo: Long, //The timestamp until when the policy is valid
                        eventVersion: String = "1.0",
                        userID: String = "",
                        details: Map[String, String] = Map()
                        ) extends ExternalEventModel {

  assert(if(validTo != -1) validTo > validFrom else validFrom > 0)

  def validate = true

  def withReceivedMillis(millis: Long) = copy(receivedMillis = millis)

  def withDetails(newDetails: Map[String, String], newOccurredMillis: Long) =
    this.copy(details = newDetails, occurredMillis = newOccurredMillis)

  def fromToTimeslot = Timeslot(new Date(validFrom), new Date(validTo))
}

object PolicyEntry {

  def fromJson(json: String): PolicyEntry = {
    StdConverters.AllConverters.convertEx[PolicyEntry](JsonTextFormat(json))
  }

  object JsonNames {
    final val _id = "_id"
    final val id = "id"
    final val occurredMillis = "occurredMillis"
    final val receivedMillis = "receivedMillis"
    final val policyYAML = "policyYAML"
    final val validFrom = "validFrom"
    final val validTo = "validTo"
  }

}