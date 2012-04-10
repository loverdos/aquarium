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

package gr.grnet.aquarium
package events

import gr.grnet.aquarium.logic.accounting.Chargeslot
import gr.grnet.aquarium.util.json.JsonHelpers
import gr.grnet.aquarium.util.date.MutableDateCalc
import gr.grnet.aquarium.logic.accounting.dsl.{Timeslot, DSLResource}

/**
 * The following equation must hold: `newTotalCredits = oldTotalCredits + entryCredits`.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 *
 * @param userId The user ID this wallet entry is related to.
 * @param entryCredits The credit amount generated for this wallet entry.
 * @param oldTotalCredits
 * @param newTotalCredits
 * @param whenComputedMillis When the computation took place
 * @param yearOfBillingMonth
 * @param billingMonth
 * @param resourceEvents
 * @param chargeslots The details of the credit computation
 * @param resourceDef
 */
case class NewWalletEntry(userId: String,
                          entryCredits: Double,
                          oldTotalCredits: Double,
                          newTotalCredits: Double,
                          whenComputedMillis: Long,
                          referenceTimeslot: Timeslot,
                          yearOfBillingMonth: Int,
                          billingMonth: Int,
                          resourceEvents: List[ResourceEvent], // current is at the head
                          chargeslots: List[Chargeslot],
                          resourceDef: DSLResource,
                          isSynthetic: Boolean) {

  def currentResourceEvent = resourceEvents.head
  def resource = currentResourceEvent.resource
  def instanceId = currentResourceEvent.instanceID
  def chargslotCount = chargeslots.length
  def isOutOfSync = currentResourceEvent.isOutOfSyncForBillingMonth(yearOfBillingMonth, billingMonth)

  def toDebugString = "%s%s(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)".format(
    if(isSynthetic) "*" else "",
    gr.grnet.aquarium.util.shortClassNameOf(this),
    userId,
    referenceTimeslot,
    entryCredits,
    oldTotalCredits,
    newTotalCredits,
    new MutableDateCalc(whenComputedMillis).toYYYYMMDDHHMMSSSSS,
    yearOfBillingMonth,
    billingMonth,
    resourceEvents,
    chargeslots,
    resourceDef
  )
}

object NewWalletEntry {
  def fromJson(json: String): NewWalletEntry = {
    JsonHelpers.jsonToObject[NewWalletEntry](json)
  }

  object JsonNames {
    final val _id = "_id"
    final val id = "id"
    final val entryCredits = "entryCredits"
    final val oldTotalCredits = "oldTotalCredits"
    final val newTotalCredits = "newTotalCredits"
    final val whenComputedMillis = "whenComputedMillis"
    final val yearOfBillingMonth = "yearOfBillingMonth"
    final val billingMonth = "billingMonth"
    final val currentResourceEvent = "currentResourceEvent"
    final val previousResourceEvent = "previousResourceEvent"
    final val chargeslots = "chargeslots"
    final val resourceDef = "resourceDef"
  }
}
