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

import gr.grnet.aquarium.util.Loggable
import akka.actor.ActorRef
import gr.grnet.aquarium.actor._
import gr.grnet.aquarium.processor.actor._


/**
 * Responsible for the management of user actors.
 *
 * The rest of the application should send UserActor-related requests
 * to this actor and not to a UserActor directly, since UserActors are
 * managed entities. For example, how many UserActor are currently live
 * in Aquarium is managed by UserActorManager
 *
 * Any UserActor-related request sent here is properly forwarded to
 * the intended UserActor.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class UserActorManager extends AquariumActor with Loggable {
  // TODO: Get the constructor values from configuration
  @volatile
  private[this] var _actorProvider: ActorProvider = _
  
  def role = UserActorManagerRole

  private[this] def _launchUserActor(userId: String): ActorRef = {
    // create a fresh instance
    val userActor = _actorProvider.actorForRole(UserActorRole)
    UserActorSupervisor.supervisor.link(userActor)
    userActor ! UserActorInitWithUserId(userId)
    logger.info("New actor for userId: %s".format(userId))
    userActor
  }
  
  private[this] def _forwardToUserActor(userId: String, m: DispatcherMessage): Unit = {
    logger.debug("Received %s".format(m))
    UserActorCache.get(userId) match {
      case Some(userActor) ⇒
        logger.debug("Found user actor and forwarding request %s".format(m))
        userActor forward m
      case None ⇒
        logger.debug("Not found user actor for request %s. Launching new actor".format(m))
        val userActor = _launchUserActor(userId)
        UserActorCache.put(userId, userActor)
        logger.debug("Launched new user actor and forwarding request %s".format(m))
        userActor forward m
    }
  }

  protected def receive = {
    case m @ AquariumPropertiesLoaded(props) ⇒
      logger.debug("Received and ignoring %s".format(m))

    case m @ ActorProviderConfigured(actorProvider) ⇒
      this._actorProvider = actorProvider
      logger.info("Configured %s with %s".format(this, m))

    case m @ RequestUserBalance(userId, timestamp) ⇒
      _forwardToUserActor(userId, m)

    case m @ UserRequestGetState(userId, timestamp) ⇒
      _forwardToUserActor(userId, m)

    case m @ ProcessResourceEvent(resourceEvent) ⇒
      _forwardToUserActor(resourceEvent.userId, m)

    case m @ ProcessUserEvent(userEvent) ⇒
      _forwardToUserActor(userEvent.userID, m)
  }

  override def postStop = {
    logger.debug("Shutting down and stopping all user actors")
    UserActorCache.stop
  }
}