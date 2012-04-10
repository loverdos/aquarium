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

import message.config.user.UserActorInitWithUserId
import message.service.dispatcher._
import service.dispatcher.DispatcherActor
import service.pinger.PingerActor
import service.rest.RESTActor
import service.user.{UserActorManager, UserActor}
import message.config.{AquariumPropertiesLoaded, ActorProviderConfigured, ActorConfigurationMessage}
import cc.spray.can.{Timeout, RequestContext}

/**
 * Each actor within Aquarium plays one role.
 *
 * A role also dictates which configuration messages the respective actor handles.
 */
sealed abstract class ActorRole(val role: String,
                                val isCacheable: Boolean,
                                val actorType: Class[_ <: AquariumActor],
                                val handledServiceMessages: Set[Class[_]],
                                val handledConfigurationMessages: Set[Class[_ <: ActorConfigurationMessage]] = Set()) {

  val knownMessageTypes = handledServiceMessages ++ handledConfigurationMessages

  def canHandleConfigurationMessage[A <: ActorConfigurationMessage](cl: Class[A]): Boolean = {
    handledConfigurationMessages contains cl
  }

  def canHandleServiceMessage[A <: AnyRef](cl: Class[A]): Boolean = {
    handledServiceMessages contains cl
  }

  def canHandleMessage[A <: AnyRef](cl: Class[A]): Boolean = {
    knownMessageTypes contains cl
  }
}

/**
 * The actor that pings several internal services
 */
case object PingerRole
    extends ActorRole("PingerRole",
                      true,
                      classOf[PingerActor],
                      Set(classOf[AdminRequestPingAll]))

/**
 * The generic router/dispatcher.
 */
case object DispatcherRole
    extends ActorRole("DispatcherRole",
                      true,
                      classOf[DispatcherActor],
                      Set(classOf[RequestUserBalance],
                          classOf[UserRequestGetState],
                          classOf[ProcessResourceEvent],
                          classOf[ProcessUserEvent],
                          classOf[AdminRequestPingAll]),
                      Set(classOf[ActorProviderConfigured]))

/**
 * REST request handler.
 */
case object RESTRole
    extends ActorRole("RESTRole",
                      true,
                      classOf[RESTActor],
                      Set(classOf[RequestContext],
                          classOf[Timeout]))

/**
 * Role for the actor that is responsible for user actor provisioning.
 */
case object UserActorManagerRole
    extends ActorRole("UserActorManagerRole",
                      true,
                      classOf[UserActorManager],
                      Set(classOf[RequestUserBalance],
                          classOf[UserRequestGetState],
                          classOf[ProcessResourceEvent],
                          classOf[ProcessUserEvent]),
                      Set(classOf[ActorProviderConfigured],
                          classOf[AquariumPropertiesLoaded]))

/**
 * User-oriented business logic handler role.
 */
case object UserActorRole
    extends ActorRole("UserActorRole",
                      false,
                      classOf[UserActor],
                      Set(classOf[ProcessResourceEvent],
                          classOf[ProcessUserEvent],
                          classOf[RequestUserBalance],
                          classOf[UserActorInitWithUserId],
                          classOf[UserRequestGetState]),
                      Set(classOf[ActorProviderConfigured],
                          classOf[AquariumPropertiesLoaded],
                          classOf[UserActorInitWithUserId]))