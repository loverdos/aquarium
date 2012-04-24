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
package event
package im

import gr.grnet.aquarium.Configurator._
import com.ckkloverdos.maybe.{Failed, NoVal, Just}
import converter.{StdConverters, JsonTextFormat}
import util.{Loggable, makeString}

/**
 * Represents an event from the `Identity Management` (IM) external system.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
case class _IMEvent(
                    id: String, // The id at the sender side
                    occurredMillis: Long, // When it occurred at the sender side
                    receivedMillis: Long, // When it was received by Aquarium
                    userID: String,
                    clientID: String,
                    isActive: Boolean,
                    role: String,
                    eventVersion: String,
                    eventType: String,
                    details: Map[String, String],
                    _id: String = "")
  extends AquariumEventModel with IMEventModel with Loggable {


  //  assert(eventType.equalsIgnoreCase(IMEvent.EventTypes.create) ||
  //    eventType.equalsIgnoreCase(IMEvent.EventTypes.modify))

  //  assert(!role.isEmpty)

  /**
   * Validate this event according to the following rules:
   *
   * Valid event states: `(eventType, state)`:
   * - `a := CREATE, ACTIVE`
   * - `b := MODIFY, ACTIVE`
   * - `c := MODIFY, SUSPENDED`
   *
   * Valid transitions:
   * - `(non-existent) -> a`
   * - `a -> c`
   * - `c -> b`
   */
  def validate: Boolean = {

    MasterConfigurator.userStateStore.findUserStateByUserId(userID) match {
      case Just(x) =>
        if(eventType.equalsIgnoreCase(_IMEvent.EventTypes.create)) {
          logger.warn("User to create exists: IMEvent".format(this.toJsonString));
          return false
        }
      case NoVal =>
        if(!eventType.equalsIgnoreCase(_IMEvent.EventTypes.modify)) {
          logger.warn("Inexistent user to modify. IMEvent:".format(this.toJsonString))
          return false
        }
      case Failed(x) =>
        logger.warn("Error retrieving user state: %s".format(x))
    }

    true
  }

  def withReceivedMillis(millis: Long) = copy(receivedMillis = millis)
}

object _IMEvent {

  object EventTypes {
    val create = "create"
    val modify = "modify"
  }

  def fromJson(json: String): _IMEvent = {
    StdConverters.AllConverters.convertEx[_IMEvent](JsonTextFormat(json))
  }

  def fromBytes(bytes: Array[Byte]): _IMEvent = {
    StdConverters.AllConverters.convertEx[_IMEvent](JsonTextFormat(makeString(bytes)))
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
