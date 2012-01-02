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

import gr.grnet.aquarium.logic.accounting.Policy
import net.liftweb.json.{JsonAST, Xml}
import gr.grnet.aquarium.util.json.JsonHelpers

/**
 * Event sent to Aquarium by clients for resource accounting.
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 * @author Georgios Gousios <gousiosg@gmail.com>.
 */
case class ResourceEvent(
    override val id: String,           // The id at the client side (the sender) TODO: Rename to remoteId or something...
    override val occurredMillis: Long, // When it occurred at client side (the sender)
    override val receivedMillis: Long, // When it was received by Aquarium
    userId: String,
    clientId: String,
    resource: String,                  // String representation of the resource type (e.g. "bndup", "vmtime").
    eventVersion: String,
    value: Float,
    details: Map[String, String])
  extends AquariumEvent(id, occurredMillis, receivedMillis) {

  def validate() : Boolean = {

    val res = Policy.policy.findResource(resource) match {
      case Some(x) => x
      case None => return false
    }

    if (!details.keySet.contains("value"))
      return false

    //if (resource.complex && !details.keySet.contains("instance-id"))
    //  return false
    //TODO: See how to describe complex resources

    true
  }
}

object ResourceEvent {
  def fromJson(json: String): ResourceEvent = {
    JsonHelpers.jsonToObject[ResourceEvent](json)
  }

  def fromJValue(jsonAST: JsonAST.JValue): ResourceEvent = {
    JsonHelpers.jValueToObject[ResourceEvent](jsonAST)
  }

  def fromBytes(bytes: Array[Byte]): ResourceEvent = {
    JsonHelpers.jsonBytesToObject[ResourceEvent](bytes)
  }

  def fromXml(xml: String): ResourceEvent = {
    fromJValue(Xml.toJson(scala.xml.XML.loadString(xml)))
  }
  
  object JsonNames {
    final val _id = "_id"
    final val id = "id"
    final val userId = "userId"
    final val timestamp = "timestamp"
    final val clientId = "clientId"

    final val vmId = "vmId"
  }
}