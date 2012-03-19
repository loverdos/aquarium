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

import net.liftweb.json.{JsonAST, Xml}
import gr.grnet.aquarium.util.json.JsonHelpers
import gr.grnet.aquarium.logic.accounting.dsl._
import com.ckkloverdos.maybe.{MaybeOption, Maybe}
import java.util.Date
import gr.grnet.aquarium.util.date.MutableDateCalc

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
    instanceId: String,                // String representation of the resource instance id
    eventVersion: String,
    value: Double,
    details: ResourceEvent.Details)
  extends AquariumEvent(id, occurredMillis, receivedMillis) {

  def validate() : Boolean = {
    !safeResource.isEmpty
  }

  def safeResource   = if(resource eq null)   "" else resource
  def safeInstanceId = if(instanceId eq null) "" else instanceId

  def hasResource   = !safeResource.isEmpty
  def hasInstanceId = !safeInstanceId.isEmpty

  def fullResourceInfo = (safeResource, safeInstanceId)

  def occurredDate = new Date(occurredMillis)

  def occurredDeltaFrom(that: ResourceEvent): Long = {
    this.occurredMillis - that.occurredMillis
  }

  def isOccurredWithinMillis(fromMillis: Long, toMillis: Long): Boolean = {
    require(fromMillis <= toMillis, "fromMillis <= toMillis")
    fromMillis <= occurredMillis && occurredMillis <= toMillis
  }

  def isOccurredWithinDates(fromDate: Date, toDate: Date): Boolean = {
    isOccurredWithinMillis(fromDate.getTime, toDate.getTime)
  }

  def isReceivedWithinMillis(fromMillis: Long, toMillis: Long): Boolean = {
    require(fromMillis <= toMillis, "fromMillis <= toMillis")
    fromMillis <= receivedMillis && receivedMillis <= toMillis
  }
  
  def isReceivedWithinDates(fromDate: Date, toDate: Date): Boolean = {
    isReceivedWithinMillis(fromDate.getTime, toDate.getTime)
  }

  def isReceivedWithinDateCalcs(fromDate: MutableDateCalc, toDate: MutableDateCalc): Boolean = {
    isReceivedWithinMillis(fromDate.getMillis, toDate.getMillis)
  }

  def isOccurredOrReceivedWithinMillis(fromMillis: Long, toMillis: Long): Boolean = {
    isOccurredWithinMillis(fromMillis, toMillis) ||
    isReceivedWithinMillis(fromMillis, toMillis)
  }

  def isOccurredOrReceivedWithinDates(fromDate: Date, toDate: Date): Boolean = {
    isOccurredWithinDates(fromDate, toDate) ||
    isReceivedWithinDates(fromDate, toDate)
  }
  
  def isOutOfSyncForBillingMonth(yearOfBillingMonth: Int, billingMonth: Int) = {
    val billingStartDateCalc = new MutableDateCalc(yearOfBillingMonth, billingMonth)
    val billingStartMillis = billingStartDateCalc.toMillis
    // NOTE: no need to `copy` the mutable `billingStartDateCalc` here because we use it once
    val billingStopMillis  = billingStartDateCalc.goEndOfThisMonth.toMillis

    isOutOfSyncForBillingPeriod(billingStartMillis, billingStopMillis)
  }
  
  def isOutOfSyncForBillingPeriod(billingStartMillis: Long, billingStopMillis: Long): Boolean = {
    isReceivedWithinMillis(billingStartMillis, billingStopMillis) &&
    (occurredMillis < billingStartMillis || occurredMillis > billingStopMillis)
  }

  def toDebugString(useOnlyInstanceId: Boolean = false): String = {
    val instanceInfo = if(useOnlyInstanceId) instanceId else "%s::%s".format(resource, instanceId)
    val occurredFormatted = new MutableDateCalc(occurredMillis).toYYYYMMDDHHMMSS
    if(occurredMillis == receivedMillis) {
      "EVENT(%s, [%s], %s, %s, %s, %s, %s)".format(
        id,
        occurredFormatted,
        value,
        instanceInfo,
        details,
        userId,
        clientId
      )
    } else {
      "EVENT(%s, [%s], [%s], %s, %s, %s, %s, %s)".format(
        id,
        occurredFormatted,
        new MutableDateCalc(receivedMillis),
        value,
        instanceInfo,
        details,
        userId,
        clientId
      )
    }
  }

  /**
   * Return `true` iff this is an event regarding a resource with an
   * [[gr.grnet.aquarium.logic.accounting.dsl.OnOffCostPolicy]].
   */
  def isOnOffEvent(policy: DSLPolicy): Boolean = {
    policy.findResource(this.resource).map(_.costPolicy) match {
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
      case Some(DSLResource(_, _, OnOffCostPolicy, _, _)) ⇒
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
      case Some(DSLResource(_, _, OnOffCostPolicy, _, _)) ⇒
        OnOffPolicyResourceState(this.value).isOff
      case _ ⇒
        false
    }
  }

  def copyWithReceivedMillis(millis: Long) = copy(receivedMillis = millis)

  /**
   * Find the cost policy of the resource named in this resource event.
   *
   * We do not expect cost policies for resources to change, because they are supposed
   * to be one of their constant characteristics. That is why do not issue a time-dependent
   * query here for the event's current policy.
   *
   * Should the need arises to change the cost policy for a resource, this is a good enough
   * reason to consider creating another type of resource.
   */
  def findCostPolicyM(defaultPolicy: DSLPolicy): Maybe[DSLCostPolicy] = {
    defaultPolicy.findResource(this.safeResource).map(_.costPolicy): Maybe[DSLCostPolicy]
  }

  /**
   * Find the cost policy of the resource named in this resource event.
   *
   * We do not expect cost policies for resources to change, because they are supposed
   * to be one of their constant characteristics. That is why do not issue a time-dependent
   * query here for the event's current policy.
   *
   * Should the need arises to change the cost policy for a resource, this is a good enough
   * reason to consider creating another type of resource.
   */
  def findCostPolicyM(resourcesMap: DSLResourcesMap): Maybe[DSLCostPolicy] = {
    for {
      rc <- resourcesMap.findResource(this.safeResource)
    } yield {
      rc.costPolicy
    }
  }
}

object ResourceEvent {
  type Details = Map[String, String]
  final val EmptyDetails: Details = Map()

  type ResourceType = String
  type ResourceIdType = String
  type FullResourceType = (ResourceType, ResourceIdType)
//  type FullResourceTypeMap = Map[FullResourceType, ResourceEvent]
  type FullMutableResourceTypeMap = scala.collection.mutable.Map[FullResourceType, ResourceEvent]

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
    final val occurredMillis = "occurredMillis"
    final val receivedMillis = "receivedMillis"
    final val clientId = "clientId"
    final val resource = "resource"
    final val resourceId = "resourceId"
    final val eventVersion = "eventVersion"
    final val value = "value"
    final val details = "details"
  }
}