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
import gr.grnet.aquarium.logic.accounting.dsl._

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
    userId: String,                    // The user for which this resource is relevant
    clientId: String,                  // The unique client identifier (usually some hash)
    resource: String,                  // String representation of the resource type (e.g. "bndup", "vmtime").
    eventVersion: String,
    value: Float,
    details: ResourceEvent.Details)
  extends AquariumEvent(id, occurredMillis, receivedMillis) {

  def validate() : Boolean = {

    if (getInstanceId(Policy.policy).isEmpty)
      return false

    true
  }

  /**
   * Return the instance id affected by this resource event. If either the
   * resource or the instance id field cannot be found, this method returns an
   * empty String.
   *
   * If no policy is given, then a default policy is loaded.
   */
  def getInstanceId(policy: DSLPolicy): String = {
    policy.findResource(this.resource) match {
      case Some(DSLComplexResource(_, _, _, descriminatorField)) ⇒
        details.getOrElse(descriminatorField, "")
      case Some(DSLSimpleResource(_, _, _)) ⇒
        "1" // TODO: put this constant somewhere centrally...
      case None => ""
    }
  }

  /**
   * Return `true` iff this is an event regarding a resource with an
   * [[gr.grnet.aquarium.logic.accounting.dsl.OnOffCostPolicy]].
   */
  def isOnOffEvent(policy: DSLPolicy): Boolean = {
    policy.findResource(this.resource).map(_.costpolicy) match {
      case Some(OnOffCostPolicy) ⇒ true
      case _ ⇒ false
    }
  }

  /**
   * Return `true` iff this is an event regarding a resource with an
   * [[gr.grnet.aquarium.logic.accounting.dsl.OnOffCostPolicy]] and a
   * `value` of `"on"`.
   */
  def isOnEvent(policy: DSLPolicy): Boolean = {
    policy.findResource(this.resource) match {
      case Some(DSLComplexResource(_, _, OnOffCostPolicy, _)) ⇒
        OnOffPolicyResourceState(this.value).isOn
      case Some(DSLSimpleResource(_, _, OnOffCostPolicy)) ⇒
        OnOffPolicyResourceState(this.value).isOn
      case _ ⇒
        false
    }
  }

  /**
   * Return `true` iff this is an event regarding a resource with an
   * [[gr.grnet.aquarium.logic.accounting.dsl.OnOffCostPolicy]] and a
   * `value` of `"off"`.
   */
  def isOffEvent(policy: DSLPolicy): Boolean = {
    policy.findResource(this.resource) match {
      case Some(DSLComplexResource(_, _, OnOffCostPolicy, _)) ⇒
        OnOffPolicyResourceState(this.value).isOff
      case Some(DSLSimpleResource(_, _, OnOffCostPolicy)) ⇒
        OnOffPolicyResourceState(this.value).isOff
      case _ ⇒
        false
    }
  }

  def setRcvMillis(millis: Long) = copy(receivedMillis = millis)
}

object ResourceEvent {
  type Details = Map[String, String]
  
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
    //final val timestamp = "timestamp" // TODO: deprecate in favor of "occurredMillis"
    final val occurredMillis = "occurredMillis"
    final val receivedMillis = "receivedMillis"
    final val clientId = "clientId"
    final val resource = "resource"
    final val eventVersion = "eventVersion"
    final val value = "value"
    final val details = "details"

    // ResourceType: VMTime
    final val vmId = "vmId"
    final val action = "action" // "on", "off"
  }

  def emtpy = ResourceEvent("", 0, 0, "", "1", "", "", 0, Map())
}