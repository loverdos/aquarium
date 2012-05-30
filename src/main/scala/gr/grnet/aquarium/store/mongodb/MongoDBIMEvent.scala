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

import gr.grnet.aquarium.converter.{JsonTextFormat, StdConverters}
import gr.grnet.aquarium.util._
import com.mongodb.DBObject
import com.mongodb.util.JSON
import gr.grnet.aquarium.event.model.im.IMEventModel
import gr.grnet.aquarium.util.date.MutableDateCalc


/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class MongoDBIMEvent(
   id: String,
   occurredMillis: Long,
   receivedMillis: Long,
   userID: String,
   clientID: String,
   isActive: Boolean,
   role: String,
   eventVersion: String,
   eventType: String,
   details: Map[String, String],
   _id: String
) extends IMEventModel with MongoDBEventModel {

  def withReceivedMillis(newReceivedMillis: Long) =
    this.copy(receivedMillis = newReceivedMillis)

  def withDetails(newDetails: Map[String, String], newOccurredMillis: Long) =
    this.copy(details = newDetails, occurredMillis = newOccurredMillis)

  override def toString = {
    "%s(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)".format(
      shortClassNameOf(this),
      id,
      new MutableDateCalc(occurredMillis).toString,
      new MutableDateCalc(receivedMillis).toString,
      userID,
      clientID,
      isActive,
      role,
      eventVersion,
      eventType,
      details,
      _id
    )
  }
}

object MongoDBIMEvent {
  final def fromJsonString(json: String): MongoDBIMEvent = {
    StdConverters.AllConverters.convertEx[MongoDBIMEvent](JsonTextFormat(json))
  }

  final def fromJsonBytes(jsonBytes: Array[Byte]): MongoDBIMEvent = {
    fromJsonString(makeString(jsonBytes))
  }

  final def fromDBObject(dbObject: DBObject): MongoDBIMEvent = {
    fromJsonString(JSON.serialize(dbObject))
  }

  final def fromOther(event: IMEventModel, _id: String): MongoDBIMEvent = {
    MongoDBIMEvent(
      event.id,
      event.occurredMillis,
      event.receivedMillis,
      event.userID,
      event.clientID,
      event.isActive,
      event.role,
      event.eventVersion,
      event.eventType,
      event.details,
      _id
    )
  }
}