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
package gr.grnet.aquarium.actor

import service.user.UserActor
import message.{GetUserBillRequest, GetUserWalletRequest, GetUserStateRequest, GetUserBalanceRequest}
import gr.grnet.aquarium.actor.message.config.{AquariumPropertiesLoaded, ActorConfigurationMessage}
import gr.grnet.aquarium.message.avro.gen.{IMEventMsg, ResourceEventMsg}

/**
 * Each actor within Aquarium plays one role.
 *
 * A role also dictates which configuration messages the respective actor handles.
 */
sealed abstract class ActorRole(
    val role: String,
    val isCacheable: Boolean,
    val actorType: Class[_ <: RoleableActor],
    val handledServiceMessages: Set[Class[_]],
    val handledConfigurationMessages: Set[Class[_ <: ActorConfigurationMessage]] = Set()
) {

  val knownMessageTypes = handledServiceMessages ++ handledConfigurationMessages

  def canHandleConfigurationMessage[A <: ActorConfigurationMessage](cl: Class[A]): Boolean = {
    handledConfigurationMessages contains cl
  }

  def canHandleConfigurationMessage[A <: ActorConfigurationMessage : Manifest](msg: A): Boolean = {
    canHandleConfigurationMessage(msg.getClass)
  }

  def canHandleServiceMessage[A <: AnyRef](cl: Class[A]): Boolean = {
    handledServiceMessages contains cl
  }

  def canHandleServiceMessage[A <: AnyRef : Manifest](msg: A): Boolean = {
    canHandleServiceMessage(msg.getClass)
  }

  def canHandleMessage[A <: AnyRef](cl: Class[A]): Boolean = {
    knownMessageTypes contains cl
  }

  def canHandleMessage[A <: AnyRef: Manifest](msg: A): Boolean = {
    canHandleMessage(msg.getClass)
  }
}

/**
 * User-oriented business logic handler role.
 */
case object UserActorRole
    extends ActorRole("UserActorRole",
                      false,
                      classOf[UserActor],
                      Set(classOf[ResourceEventMsg],
                          classOf[IMEventMsg],
                          classOf[GetUserWalletRequest],
                          classOf[GetUserBalanceRequest],
                          classOf[GetUserBillRequest],
                          classOf[GetUserStateRequest]),
                      Set(classOf[AquariumPropertiesLoaded]))
