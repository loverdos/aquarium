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

//package gr.grnet.aquarium
//package event
//
//import gr.grnet.aquarium.util.makeString
//import gr.grnet.aquarium.logic.accounting.dsl._
//import com.ckkloverdos.maybe.Maybe
//import java.util.Date
//import gr.grnet.aquarium.util.date.MutableDateCalc
//import collection.SeqLike
//import converter.{JsonTextFormat, StdConverters}

/**
 * Event sent to Aquarium by clients for resource accounting.
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 * @author Georgios Gousios <gousiosg@gmail.com>.
 */
//case class ResourceEvent(
//    id: String,           // The id at the client side (the sender) TODO: Rename to remoteId or something...
//    occurredMillis: Long, // When it occurred at client side (the sender)
//    receivedMillis: Long, // When it was received by Aquarium
//    userID: String,                    // The user for which this resource is relevant
//    clientID: String,                  // The unique client identifier (usually some hash)
//    resource: String,                  // String representation of the resource type (e.g. "bndup", "vmtime").
//    instanceID: String,                // String representation of the resource instance id
//    eventVersion: String,
//    value: Double,
//    details: Map[String, String])
//extends ExternalEventModel {
//
//  def validate() : Boolean = {
//    !safeResource.isEmpty
//  }
//
//  def safeResource   = if(resource eq null)   "" else resource
//  def safeInstanceId = if(instanceID eq null) "" else instanceID
//
//  def fullResourceInfo = (safeResource, safeInstanceId)
//
//  def occurredDate = new Date(occurredMillis)
//
//  def isOccurredWithinMillis(fromMillis: Long, toMillis: Long): Boolean = {
//    require(fromMillis <= toMillis, "fromMillis <= toMillis")
//    fromMillis <= occurredMillis && occurredMillis <= toMillis
//  }
//
//  def isReceivedWithinMillis(fromMillis: Long, toMillis: Long): Boolean = {
//    require(fromMillis <= toMillis, "fromMillis <= toMillis")
//    fromMillis <= receivedMillis && receivedMillis <= toMillis
//  }
//
//  def isOccurredOrReceivedWithinMillis(fromMillis: Long, toMillis: Long): Boolean = {
//    isOccurredWithinMillis(fromMillis, toMillis) ||
//    isReceivedWithinMillis(fromMillis, toMillis)
//  }
//
//  def isOutOfSyncForBillingMonth(yearOfBillingMonth: Int, billingMonth: Int) = {
//    val billingStartDateCalc = new MutableDateCalc(yearOfBillingMonth, billingMonth)
//    val billingStartMillis = billingStartDateCalc.toMillis
//    // NOTE: no need to `copy` the mutable `billingStartDateCalc` here because we use it once
//    val billingStopMillis  = billingStartDateCalc.goEndOfThisMonth.toMillis
//
//    isOutOfSyncForBillingPeriod(billingStartMillis, billingStopMillis)
//  }
//
//  def isOutOfSyncForBillingPeriod(billingStartMillis: Long, billingStopMillis: Long): Boolean = {
//    isReceivedWithinMillis(billingStartMillis, billingStopMillis) &&
//    (occurredMillis < billingStartMillis || occurredMillis > billingStopMillis)
//  }
//
//  def toDebugString(useOnlyInstanceId: Boolean = false): String = {
//    val instanceInfo = if(useOnlyInstanceId) instanceID else "%s::%s".format(resource, instanceID)
//    val occurredFormatted = new MutableDateCalc(occurredMillis).toYYYYMMDDHHMMSS
//    if(occurredMillis == receivedMillis) {
//      "%sEVENT(%s, [%s], %s, %s, %s, %s, %s)".format(
//        if(isSynthetic) "*" else "",
//        id,
//        occurredFormatted,
//        value,
//        instanceInfo,
//        details,
//        userID,
//        clientID
//      )
//    } else {
//      "%sEVENT(%s, [%s], [%s], %s, %s, %s, %s, %s)".format(
//        if(isSynthetic) "*" else "",
//        id,
//        occurredFormatted,
//        new MutableDateCalc(receivedMillis),
//        value,
//        instanceInfo,
//        details,
//        userID,
//        clientID
//      )
//    }
//  }
//
//  def withReceivedMillis(millis: Long) = copy(receivedMillis = millis)

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
//  def findCostPolicyM(defaultPolicy: DSLPolicy): Maybe[DSLCostPolicy] = {
//    defaultPolicy.findResource(this.safeResource).map(_.costPolicy): Maybe[DSLCostPolicy]
//  }

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
//  def findCostPolicyM(resourcesMap: DSLResourcesMap): Maybe[DSLCostPolicy] = {
//    for {
//      rc <- resourcesMap.findResource(this.safeResource)
//    } yield {
//      rc.costPolicy
//    }
//  }

  /**
   * `Synthetic` means that Aquarium has manufactured this resource event for some purpose. For example, the implicitly
   * issued resource events at the end a a billing period.
   *
   * @return `true` iff this resource event is synthetic.
   */
//  def isSynthetic = {
//    details contains ResourceEvent.JsonNames.details_aquarium_is_synthetic
//  }
//}

object ResourceEvent {
//  type ResourceType = String
//  type ResourceIdType = String
//  type FullResourceType = (ResourceType, ResourceIdType)
//  type FullResourceTypeMap = Map[FullResourceType, ResourceEvent]
//  type FullMutableResourceTypeMap = scala.collection.mutable.Map[FullResourceType, ResourceEvent]

//  def fromJson(json: String): ResourceEvent = {
//    StdConverters.AllConverters.convertEx[ResourceEvent](JsonTextFormat(json))
//  }
//
//  def fromBytes(bytes: Array[Byte]): ResourceEvent = {
//    StdConverters.AllConverters.convertEx[ResourceEvent](JsonTextFormat(makeString(bytes)))
//  }
//
//  def setAquariumSynthetic(map: Map[String, String]): Map[String, String] = {
//    map.updated(JsonNames.details_aquarium_is_synthetic, "true")
//  }

//  def setAquariumSyntheticAndImplicitEnd(map: Map[String, String]): Map[String, String] = {
//    map.
//      updated(JsonNames.details_aquarium_is_synthetic, "true").
//      updated(JsonNames.details_aquarium_is_implicit_end, "true")
//  }
//
//  def sortByOccurred[S <: Seq[ResourceEvent]](events: S with SeqLike[ResourceEvent, S]): S = {
//    events.sortWith(_.occurredMillis <= _.occurredMillis)
//  }
//
//  object JsonNames {
//    final val _id = "_id"
//    final val id = "id"
//    final val userId = "userId"
//    final val occurredMillis = "occurredMillis"
//    final val receivedMillis = "receivedMillis"
//    final val clientId = "clientId"
//    final val resource = "resource"
//    final val resourceId = "resourceId"
//    final val eventVersion = "eventVersion"
//    final val value = "value"
//    final val details = "details"

    // This is set in the details map to indicate a synthetic resource event (ie not a real one).
    // Examples of synthetic resource events are those that are implicitly generated at the
    // end of the billing period (e.g. `OFF`s).
//    final val details_aquarium_is_synthetic    = "__aquarium_is_synthetic__"
//
//    final val details_aquarium_is_implicit_end = "__aquarium_is_implicit_end__"
//  }
}