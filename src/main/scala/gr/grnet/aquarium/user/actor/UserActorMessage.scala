/*
 * Copyright 2011 GRNET S.A. All rights reserved.
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

package gr.grnet.aquarium.user.actor

import gr.grnet.aquarium.actor.ActorMessage
import gr.grnet.aquarium.logic.accounting.dsl.DSLResource
import java.util.Date

/**
 * Messages handled by a UserActor.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

trait UserActorMessage extends ActorMessage

case class UserActorInitWithUserId(userId: String) extends UserActorMessage

case object UserActorStop extends UserActorMessage

/**
 * A request to get the current state for a resource. If the resource is
 * complex, the optional instance field is used to find the appropriate
 * instance to query.
 */
case class UserActorResourceStateRequest(resource: DSLResource,
                                         instance: Option[String])
extends UserActorMessage

/**
 * The response to a
 * [[gr.grnet.aquarium.user.actor.UserActorResourceStateRequest]] message
 */
case class UserActorResourceStateResponse(resource: DSLResource,
                                          lastUpdate: Date,
                                          value: Any)
extends UserActorMessage

/**
 * Update the state for the resource to the provided value
 */
case class UserActorResourceStateUpdate(resource: DSLResource,
                                        instanceid: Option[String],
                                        value: Any)
extends UserActorMessage

/**
 * Create a new instance for a complex resource
 */
case class UserActorResourceCreateInstance(resource: DSLResource,
                                           instanceid: String)
extends UserActorMessage

/**
 * Delete an instance for a complex resource
 */
case class UserActorResourceDeleteInstance(resource: DSLResource,
                                           instanceid: String)
extends UserActorMessage