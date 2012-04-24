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

package gr.grnet.aquarium.actor.message
package service
package dispatcher

import gr.grnet.aquarium.user.UserState
import gr.grnet.aquarium.util.json.JsonSupport
import gr.grnet.aquarium.event.im.IMEventModel
import gr.grnet.aquarium.event.ResourceEvent
import gr.grnet.aquarium.converter.{PrettyJsonTextFormat, StdConverters}


/**
 * This is the base class of the messages the dispatcher understands.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
sealed trait DispatcherMessage extends ActorMessage {
  def isError: Boolean = false
}

sealed trait DispatcherResponseMessage extends DispatcherMessage {
  def error: Option[String]

  override def isError = error.isDefined

  def responseBody: Any
  def responseBodyToJson: String = {
    responseBody match {
      case null ⇒
        throw new NullPointerException("Unexpected null response body in %s".format(this))
      case other ⇒
        StdConverters.AllConverters.convertEx[PrettyJsonTextFormat](other).value
    }
  }
}

case class RequestUserBalance(userId: String, timestamp: Long) extends DispatcherMessage
case class BalanceValue(balance: Double) extends JsonSupport
case class ResponseUserBalance(userId: String, balance: Double, error: Option[String]) extends DispatcherResponseMessage {
  def responseBody = BalanceValue(balance)
}

case class UserResponseGetBalance(userId: String, balance: Double) extends DispatcherResponseMessage {
  def responseBody = BalanceValue(balance)
  def error = None
}

case class UserRequestGetState(userId: String, timestamp: Long) extends DispatcherMessage
case class UserResponseGetState(userId: String, state: UserState) extends DispatcherResponseMessage {
  def responseBody = state
  val error = None
}

/**
 * Dispatcher message that triggers the resource event processing pipeline.
 *
 * Note that the prefix `Process` means that no reply is created or needed.
 */
case class ProcessResourceEvent(rcEvent: ResourceEvent) extends DispatcherMessage

/**
 * Dispatcher message that triggers the user event processing pipeline.
 *
 * Note that the prefix `Process` means that no reply is created or needed.
 */
case class ProcessIMEvent(imEvent: IMEventModel) extends DispatcherMessage


case class AdminRequestPingAll() extends DispatcherMessage
