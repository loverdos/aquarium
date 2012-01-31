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

package gr.grnet.aquarium.logic.events

import net.liftweb.json.{Extraction, parse => parseJson}
import gr.grnet.aquarium.util.json.JsonHelpers

/**
 * Store entry for serialized policy data.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
case class PolicyEntry(
  override val id: String,           //SHA-1 of the provided policyYaml string
  override val occurredMillis: Long, //Time this event was stored
  override val receivedMillis: Long, //Does not make sense for local events -> not used
  policyYAML: String,                //The serialized policy
  validFrom: Long,                   //The timestamp since when the policy is valid
  validTo: Long                      //The timestamp until when the policy is valid
)
extends AquariumEvent(id, occurredMillis, receivedMillis) {

  assert (validTo > validFrom)

  def validate = true

  def copyWithReceivedMillis(millis: Long) = copy(receivedMillis = millis)
}

object PolicyEntry {

  def fromJson(json: String): PolicyEntry = {
    implicit val formats = JsonHelpers.DefaultJsonFormats
    val jsonAST = parseJson(json)
    Extraction.extract[PolicyEntry](jsonAST)
  }

  def fromBytes(bytes: Array[Byte]): PolicyEntry = {
    JsonHelpers.jsonBytesToObject[PolicyEntry](bytes)
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