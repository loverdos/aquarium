
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

import gr.grnet.aquarium.processor.actor.{ResourceProcessorActor, DispatcherActor}
import gr.grnet.aquarium.rest.actor.RESTActor
import gr.grnet.aquarium.user.actor.{UserActor, UserActorManager}

/**
 * Each actor within Aquarium plays one role.
 *
 * A role also dictates which configuration messages the respective actor handles.
 */
sealed abstract class ActorRole(val role: String,
                                val actorType: Class[_ <: AquariumActor],
                                val handledConfigurationMessages: Set[Class[_ <: ActorConfigurationMessage]] = Set()) {

  def canHandleConfigurationMessage[A <: ActorConfigurationMessage](cl: Class[A]): Boolean = {
    handledConfigurationMessages contains cl
  }
}

/**
 * The generic router/dispatcher.
 */
case object DispatcherRole
    extends ActorRole("DispatcherRole",
                      classOf[DispatcherActor],
                      Set(classOf[ActorProviderConfigured]))

/**
 * Processes user-related resource events.
 */
case object ResourceProcessorRole
    extends ActorRole("ResourceProcessorRole", classOf[ResourceProcessorActor])

/**
 * REST request handler.
 */
case object RESTRole extends ActorRole("RESTRole", classOf[RESTActor])

/**
 * Role for the actor that is responsible for user actor provisioning.
 */
case object UserActorManagerRole
    extends ActorRole("UserActorManagerRole",
                      classOf[UserActorManager],
                      Set(classOf[ActorProviderConfigured], classOf[AquariumPropertiesLoaded]))

/**
 * User-oriented business logic handler role.
 */
case object UserActorRole
    extends ActorRole("UserActorRole",
                      classOf[UserActor],
                      Set(classOf[ActorProviderConfigured], classOf[AquariumPropertiesLoaded]))