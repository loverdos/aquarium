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

import java.util.Date
import gr.grnet.aquarium.converter.{JsonTextFormat, StdConverters}

/**
 * A WalletEntry is a derived entity. Its data represent money/credits and are calculated based on
 * resource events.
 *
 * Wallet entries give us a picture of when credits are calculated from resource events.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class WalletEntry(
                        id: String, // The id at the client side (the sender) TODO: Rename to remoteId or something...
                        occurredMillis: Long, // The time of oldest matching resource event
                        receivedMillis: Long, // The time the cost calculation was done
                        sourceEventIDs: List[String], // The events that triggered this WalletEntry
                        value: Double,
                        reason: String,
                        userId: String,
                        resource: String,
                        instanceId: String,
                        finalized: Boolean,
                        eventVersion: String = "1.0",
                        userID: String = "",
                        details: Map[String, String] = Map()
                        ) extends ExternalEventModel {


  assert(occurredMillis > 0)
  assert(value >= 0F)
  assert(!userId.isEmpty)

  def validate = true

  def fromResourceEvent(rceId: String): Boolean = {
    sourceEventIDs contains rceId
  }

  def withReceivedMillis(millis: Long) = copy(receivedMillis = millis)

  def withDetails(newDetails: Map[String, String], newOccurredMillis: Long) =
    this.copy(details = newDetails, occurredMillis = newOccurredMillis)

  def occurredDate = new Date(occurredMillis)

  def receivedDate = new Date(receivedMillis)
}

object WalletEntry {
  def fromJson(json: String): WalletEntry = {
    StdConverters.AllConverters.convertEx[WalletEntry](JsonTextFormat(json))
  }

  def zero = WalletEntry("", 1L, 1L, Nil, 1, "", "foo", "bar", "0", false)

  object JsonNames {
    final val _id = "_id"
    final val id = "id"
    final val occurredMillis = "occurredMillis"
    final val receivedMillis = "receivedMillis"
    final val sourceEventIDs = "sourceEventIDs"
    final val value = "value"
    final val reason = "reason"
    final val userId = "userId"
    final val resource = "resource"
    final val instanceId = "instanceId"
    final val finalized = "finalized"
  }

}