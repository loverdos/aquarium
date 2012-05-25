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

import gr.grnet.aquarium.util.shortClassNameOf
import gr.grnet.aquarium.service.RoleableActorProviderService
import akka.actor.ActorRef
import user.{UserActorCache}
import message.config.{AquariumPropertiesLoaded, ActorProviderConfigured}
import gr.grnet.aquarium.actor.message.event.{ProcessResourceEvent, ProcessIMEvent}
import gr.grnet.aquarium.actor.message.admin.PingAllRequest
import gr.grnet.aquarium.actor.message.{UserActorRequestMessage, GetUserStateRequest, GetUserBalanceRequest}
import gr.grnet.aquarium.{AquariumException, AquariumInternalError}

/**
 * Business logic router. Incoming messages are routed to appropriate destinations. Replies are routed back
 * appropriately.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class RouterActor extends ReflectiveRoleableActor {
  private[this] var _actorProvider: RoleableActorProviderService = _

  def role = RouterRole

  private[this] def _launchUserActor(userID: String): ActorRef = {
    // create a fresh instance
    val userActor = _actorProvider.actorForRole(UserActorRole)
    UserActorCache.put(userID, userActor)

    userActor
  }

  private[this] def _findOrCreateUserActor(userID: String): ActorRef = {
    UserActorCache.get(userID) match {
      case Some(userActorRef) ⇒
        userActorRef

      case None ⇒
        _launchUserActor(userID)
    }
  }

  private[this] def _forwardToUserActor(userID: String, m: UserActorRequestMessage): Unit = {
    _findOrCreateUserActor(userID) forward m
  }


  /**
   * Handles an exception that occurred while servicing a message.
   *
   * @param t
   * The exception.
   * @param servicingMessage
   * The message that was being served while the exception happened.
   * Note that the message can be `null`, in which case the exception
   * is an NPE.
   */
  override protected def onThrowable(t: Throwable, servicingMessage: AnyRef) = {
    logChainOfCauses(t)

    def logIgnore(e: Throwable) = {
      logger.error("Ignoring %s".format(shortClassNameOf(e)), e)
    }

    t match {
      case e: Error ⇒
        throw e

      case e: AquariumInternalError ⇒
        logIgnore(e)

      case e: AquariumException ⇒
        logIgnore(e)

      case e: Throwable ⇒
        logIgnore(e)
    }
  }

  def onAquariumPropertiesLoaded(m: AquariumPropertiesLoaded): Unit = {
    logger.info("Configured with {}", shortClassNameOf(m))
  }

  def onActorProviderConfigured(m: ActorProviderConfigured): Unit = {
    this._actorProvider = m.actorProvider
    logger.info("Configured with {}", shortClassNameOf(m))
  }

  def onProcessIMEvent(m: ProcessIMEvent): Unit = {
     _forwardToUserActor(m.imEvent.userID, m)
  }

  def onGetUserBalanceRequest(m: GetUserBalanceRequest): Unit = {
    _forwardToUserActor(m.userID, m)
  }

  def onGetUserStateRequest(m: GetUserStateRequest): Unit = {
    _forwardToUserActor(m.userID, m)
  }

  def onProcessResourceEvent(m: ProcessResourceEvent): Unit = {
    _forwardToUserActor(m.rcEvent.userID, m)
  }

  def onPingAllRequest(m: PingAllRequest): Unit = {
  }

  override def postStop = {
    UserActorCache.stop
  }
}