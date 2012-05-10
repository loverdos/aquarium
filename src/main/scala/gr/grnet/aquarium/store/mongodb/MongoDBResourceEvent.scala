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

package gr.grnet.aquarium.store.mongodb

import com.mongodb.DBObject

import gr.grnet.aquarium.util.makeString
import gr.grnet.aquarium.converter.{StdConverters, JsonTextFormat}
import com.mongodb.util.JSON
import gr.grnet.aquarium.event.model.resource.ResourceEventModel

/**
 * A [[gr.grnet.aquarium.event.model.resource.ResourceEventModel]] as represented for MongoDB.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class MongoDBResourceEvent(
  id: String,
  occurredMillis: Long,
  receivedMillis: Long,
  userID: String,
  clientID: String,
  resource: String,
  instanceID: String,
  value: Double,
  eventVersion: String,
  details: Map[String, String],
  _id: String
) extends ResourceEventModel with MongoDBEventModel {

  def withReceivedMillis(newReceivedMillis: Long) =
    this.copy(receivedMillis = newReceivedMillis)

  def withDetails(newDetails: Map[String, String], newOccurredMillis: Long) =
    this.copy(details = newDetails, occurredMillis = newOccurredMillis)

  def withDetailsAndValue(newDetails: Map[String, String], newValue: Double, newOccurredMillis: Long) =
    this.copy(details = newDetails, value = newValue, occurredMillis = newOccurredMillis)
}

object MongoDBResourceEvent {
  final def fromJsonString(json: String): MongoDBResourceEvent = {
    StdConverters.AllConverters.convertEx[MongoDBResourceEvent](JsonTextFormat(json))
  }

  final def fromJsonBytes(bytes: Array[Byte]): MongoDBResourceEvent = {
    fromJsonString(makeString(bytes))
  }

  final def fromDBObject(dbObject: DBObject): MongoDBResourceEvent = {
    fromJsonString(JSON.serialize(dbObject))
  }


  final def fromOther(rcEvent: ResourceEventModel, _id: String): MongoDBResourceEvent = {
    import rcEvent._

    MongoDBResourceEvent(
      id,
      occurredMillis,
      receivedMillis,
      userID,
      clientID,
      resource,
      instanceID,
      value,
      eventVersion,
      details,
      _id
    )
  }
}