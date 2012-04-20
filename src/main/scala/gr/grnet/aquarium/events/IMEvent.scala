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

import gr.grnet.aquarium.util.makeString
import gr.grnet.aquarium.Configurator._
import com.ckkloverdos.maybe.{Failed, NoVal, Just}
import converter.{StdConverters, JsonTextFormat}
import org.bson.types.ObjectId

/**
 * Represents an event from the `Identity Management` (IM) external system.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
case class  IMEvent(
    override val id: String,           // The id at the sender side
    override val occurredMillis: Long, // When it occurred at the sender side
    override val receivedMillis: Long, // When it was received by Aquarium
    userID: String,
    clientID: String,
    isActive: Boolean,
    role: String,
    override val eventVersion: String,
    eventType: String,
    override val details: Map[String, String],
    _id: ObjectId = new ObjectId())
  extends AquariumEvent(id, occurredMillis, receivedMillis) with IMEventModel {


//  assert(eventType.equalsIgnoreCase(IMEvent.EventTypes.create) ||
//    eventType.equalsIgnoreCase(IMEvent.EventTypes.modify))

//  assert(!role.isEmpty)

  override def storeID = {
    _id match {
      case null ⇒ None
      case _    ⇒ Some(_id)
    }
  }

  /**
   * Validate this event according to the following rules:
   *
   * Valid event states: `(eventType, state)`:
   *  - `a := CREATE, ACTIVE`
   *  - `b := MODIFY, ACTIVE`
   *  - `c := MODIFY, SUSPENDED`
   *
   * Valid transitions:
   *  - `(non-existent) -> a`
   *  - `a -> c`
   *  - `c -> b`
   */
  def validate: Boolean = {

    MasterConfigurator.userStateStore.findUserStateByUserId(userID) match {
      case Just(x) =>
        if (eventType.equalsIgnoreCase(IMEvent.EventTypes.create)){
          logger.warn("User to create exists: IMEvent".format(this.toJson));
          return false
        }
      case NoVal =>
        if (!eventType.equalsIgnoreCase(IMEvent.EventTypes.modify)){
          logger.warn("Inexistent user to modify. IMEvent:".format(this.toJson))
          return false
        }
      case Failed(x) =>
        logger.warn("Error retrieving user state: %s".format(x))
    }

    true
  }

  def copyWithReceivedMillis(millis: Long) = copy(receivedMillis = millis)

  def isCreateUser = eventType.equalsIgnoreCase(IMEvent.EventTypes.create)

  def isModifyUser = eventType.equalsIgnoreCase(IMEvent.EventTypes.modify)

  def isStateActive = isActive

  def isStateSuspended = !isActive
}

object IMEvent {

  object EventTypes {
    val create = "create"
    val modify = "modify"
  }

  def fromJson(json: String): IMEvent = {
    StdConverters.StdConverters.convertEx[IMEvent](JsonTextFormat(json))
  }

  def fromBytes(bytes: Array[Byte]): IMEvent = {
    StdConverters.StdConverters.convertEx[IMEvent](JsonTextFormat(makeString(bytes)))
  }

  object JsonNames {
    final val _id = "_id"
    final val id = "id"

    final val occurredMillis = "occurredMillis"
    final val receivedMillis = "receivedMillis"
    final val userID = "userID"
    final val clientID = "clientID"
    final val isActive = "isActive"
    final val role = "role"
    final val eventVersion = "eventVersion"
    final val eventType = "eventType"
    final val details = "details"
  }
}
