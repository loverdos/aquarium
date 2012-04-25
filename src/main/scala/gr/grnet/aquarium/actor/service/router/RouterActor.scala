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
package service
package router

import gr.grnet.aquarium.util.Loggable
import gr.grnet.aquarium.service.ActorProviderService
import message.service.router._
import akka.actor.ActorRef
import message.config.user.UserActorInitWithUserId
import user.{UserActorCache, UserActorSupervisor}
import message.config.{AquariumPropertiesLoaded, ActorProviderConfigured}

/**
 * Business logic router. Incoming messages are routed to appropriate destinations. Replies are routed back
 * appropriately.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class RouterActor extends ReflectiveAquariumActor {
  private[this] var _actorProvider: ActorProviderService = _

  def role = RouterRole

  private[this] def _forwardToUserManager(m: RouterMessage): Unit = {
    logger.debug("Received %s".format(m))
    val userActorManager = _actorProvider.actorForRole(UserActorManagerRole)
    // forward to the user actor manager, which in turn will
    // forward to the appropriate user actor (and create one if it does not exist)
    userActorManager forward m
  }

  private[this] def _launchUserActor(userId: String): ActorRef = {
    // create a fresh instance
    val userActor = _actorProvider.actorForRole(UserActorRole)
    UserActorSupervisor.supervisor.link(userActor)
    userActor ! UserActorInitWithUserId(userId)
    logger.info("New actor for userId: %s".format(userId))
    userActor
  }

  private[this] def _forwardToUserActor(userId: String, m: RouterMessage): Unit = {
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

  def onAquariumPropertiesLoaded(m: AquariumPropertiesLoaded): Unit = {
  }

  def onActorProviderConfigured(m: ActorProviderConfigured): Unit = {
    this._actorProvider = m.actorProvider
    logger.info("Configured %s with %s".format(this, m))
  }

  def onRequestUserBalance(m: RequestUserBalance): Unit = {
    _forwardToUserActor(m.userId, m)
  }

  def onUserRequestGetState(m: UserRequestGetState): Unit = {
    _forwardToUserActor(m.userId, m)
  }

  def onProcessResourceEvent(m: ProcessResourceEvent): Unit = {
    _forwardToUserActor(m.rcEvent.userID, m)
  }

  def onProcessIMEvent(m: ProcessIMEvent): Unit = {
    _forwardToUserActor(m.imEvent.userID, m)
  }

  def onAdminRequestPingAll(m: AdminRequestPingAll): Unit = {

  }

  override def postStop = {
    UserActorCache.stop
  }
}