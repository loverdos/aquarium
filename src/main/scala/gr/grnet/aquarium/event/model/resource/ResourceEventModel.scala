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
package resource

import java.util.Date
import gr.grnet.aquarium.util.date.MutableDateCalc
import gr.grnet.aquarium.event.model.ExternalEventModel

/**
 * The model of any resource event.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

trait ResourceEventModel extends ExternalEventModel {
  /**
   * Identifies the client that sent this event.
   */
  def clientID: String

  /**
   * String representation of the resource type (e.g. "bndup", "vmtime").
   */
  def resource: String

  /**
   * String representation of the resource instance id
   */
  def instanceID: String


  /**
   * The resource value.
   */
  def value: Double

  def withDetails(newDetails: Map[String, String], newOccurredMillis: Long): ResourceEventModel

  def withDetailsAndValue(newDetails: Map[String, String], newValue: Double, newOccurredMillis: Long): ResourceEventModel

  def safeResource   = if(resource eq null)   "" else resource
  def safeInstanceId = if(instanceID eq null) "" else instanceID

  def fullResourceInfo = (safeResource, safeInstanceId)

  def occurredDate = new Date(occurredMillis)

  def isOccurredWithinMillis(fromMillis: Long, toMillis: Long): Boolean = {
    require(fromMillis <= toMillis, "fromMillis <= toMillis")
    fromMillis <= occurredMillis && occurredMillis <= toMillis
  }

  def isReceivedWithinMillis(fromMillis: Long, toMillis: Long): Boolean = {
    require(fromMillis <= toMillis, "fromMillis <= toMillis")
    fromMillis <= receivedMillis && receivedMillis <= toMillis
  }

  def isOccurredOrReceivedWithinMillis(fromMillis: Long, toMillis: Long): Boolean = {
    isOccurredWithinMillis(fromMillis, toMillis) ||
    isReceivedWithinMillis(fromMillis, toMillis)
  }

  def isOutOfSyncForBillingMonth(yearOfBillingMonth: Int, billingMonth: Int) = {
    val billingStartDateCalc = new MutableDateCalc(yearOfBillingMonth, billingMonth)
    val billingStartMillis = billingStartDateCalc.toMillis
    // NOTE: no need to `copy` the mutable `billingStartDateCalc` here because we use it once
    val billingStopMillis  = billingStartDateCalc.goEndOfThisMonth.toMillis

    isOutOfSyncForBillingPeriod(billingStartMillis, billingStopMillis)
  }

  def isOutOfSyncForBillingPeriod(billingStartMillis: Long, billingStopMillis: Long): Boolean = {
    // Out of sync events are those that were received within the billing period
    // but actually occurred outside the billing period.
     isReceivedWithinMillis(billingStartMillis, billingStopMillis) &&
    !isOccurredWithinMillis(billingStartMillis, billingStopMillis)
  }

  def toDebugString(useOnlyInstanceId: Boolean = false): String = {
    val instanceInfo = if(useOnlyInstanceId) instanceID else "%s::%s".format(resource, instanceID)
    val occurredFormatted = new MutableDateCalc(occurredMillis).toYYYYMMDDHHMMSS
    if(occurredMillis == receivedMillis) {
      "%sEVENT(%s, [%s], %s, %s, %s, %s, %s)".format(
        if(isSynthetic) "*" else "",
        id,
        occurredFormatted,
        value,
        instanceInfo,
        details,
        userID,
        clientID
      )
    } else {
      "%sEVENT(%s, [%s], [%s], %s, %s, %s, %s, %s)".format(
        if(isSynthetic) "*" else "",
        id,
        occurredFormatted,
        new MutableDateCalc(receivedMillis),
        value,
        instanceInfo,
        details,
        userID,
        clientID
      )
    }
  }

  /**
   * `Synthetic` means that Aquarium has manufactured this resource event for some purpose. For example, the implicitly
   * issued resource events at the end a a billing period.
   *
   * @return `true` iff this resource event is synthetic.
   */
  def isSynthetic = {
    details contains ResourceEventModel.Names.details_aquarium_is_synthetic
  }

}

object ResourceEventModel {
  type ResourceType = String
  type ResourceIdType = String
  type FullResourceType = (ResourceType, ResourceIdType)
  type FullResourceTypeMap = Map[FullResourceType, ResourceEventModel]
  type FullMutableResourceTypeMap = scala.collection.mutable.Map[FullResourceType, ResourceEventModel]

  trait NamesT extends ExternalEventModel.NamesT {
    final val clientID = "clientID"
    final val resource = "resource"
    final val instanceID = "instanceID"
    final val value = "value"

    // This is set in the details map to indicate a synthetic resource event (ie not a real one).
    // Examples of synthetic resource events are those that are implicitly generated at the
    // end of the billing period (e.g. `OFF`s).
    final val details_aquarium_is_synthetic    = "__aquarium_is_synthetic__"

    final val details_aquarium_is_implicit_end = "__aquarium_is_implicit_end__"
  }

  object Names extends NamesT

  def setAquariumSyntheticAndImplicitEnd(map: Map[String, String]): Map[String, String] = {
    map.
      updated(Names.details_aquarium_is_synthetic, "true").
      updated(Names.details_aquarium_is_implicit_end, "true")
  }

}
