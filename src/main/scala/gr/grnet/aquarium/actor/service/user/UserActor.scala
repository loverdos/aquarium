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
package user


import gr.grnet.aquarium.actor._
import gr.grnet.aquarium.user._

import gr.grnet.aquarium.util.shortClassNameOf
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.actor.message.service.router._
import message.config.{ActorProviderConfigured, AquariumPropertiesLoaded}
import gr.grnet.aquarium.event.im.IMEventModel
import akka.config.Supervision.Temporary
import akka.actor.PoisonPill
import gr.grnet.aquarium.{AquariumException, Configurator}


/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class UserActor extends ReflectiveAquariumActor {
  private[this] var _userID: String = _
  private[this] var _userState: UserState = _

  self.lifeCycle = Temporary

  override protected def onThrowable(t: Throwable) = {
    logger.error("Terminating due to: %s".format(t.getMessage), t)
    UserActorCache.invalidate(this._userID)

    self ! PoisonPill
  }

  def role = UserActorRole

  private[this] def _configurator: Configurator = Configurator.MasterConfigurator
//  private[this] def _userId = _userState.userId

  private[this] def _timestampTheshold =
    _configurator.props.getLong(Configurator.Keys.user_state_timestamp_threshold).getOr(10000)


  def onAquariumPropertiesLoaded(event: AquariumPropertiesLoaded): Unit = {
  }

  def onActorProviderConfigured(event: ActorProviderConfigured): Unit = {
  }

  private[this] def _computeAgreementForNewUser(imEvent: IMEventModel): String = {
    // FIXME: Implement based on the role
    "default"
  }

  private[this] def processCreateUser(imEvent: IMEventModel): Unit = {
    val userID = imEvent.userID
    this._userID = userID

    val store = _configurator.storeProvider.userStateStore
    // try find user state. normally should ot exist
    val latestUserStateOpt = store.findLatestUserStateByUserID(userID)
    if(latestUserStateOpt.isDefined) {
      logger.error("Got %s(%s, %s) but user already exists. Ingoring".format(
        userID,
        shortClassNameOf(imEvent),
        imEvent.eventType))

      return
    }

    val initialAgreementName = _computeAgreementForNewUser(imEvent)
    val newUserState    = DefaultUserStateComputations.createInitialUserState(
      userID,
      imEvent.occurredMillis,
      imEvent.isActive,
      0.0,
      List(imEvent.role),
      initialAgreementName)

    this._userState = newUserState

    // FIXME: If this fails, then the actor must be shut down.
    store.insertUserState(newUserState)
  }

  private[this] def processModifyUser(event: IMEventModel): Unit = {
    val now = TimeHelpers.nowMillis()
    val newActive = ActiveStateSnapshot(event.isStateActive, now)

    DEBUG("New active status = %s".format(newActive))

    this._userState = this._userState.copy(activeStateSnapshot = newActive)
  }

  def onProcessIMEvent(event: ProcessIMEvent): Unit = {
    val imEvent = event.imEvent
    if(imEvent.isCreateUser) {
      processCreateUser(imEvent)
    } else if(imEvent.isModifyUser) {
      processModifyUser(imEvent)
    } else {
      throw new AquariumException("Cannot interpret %s".format(imEvent))
    }
  }

  def onRequestUserBalance(event: RequestUserBalance): Unit = {
    val userId = event.userID
    // FIXME: Implement threshold
    self reply UserResponseGetBalance(userId, _userState.creditsSnapshot.creditAmount)
  }

  def onUserRequestGetState(event: UserRequestGetState): Unit = {
    val userId = event.userID
   // FIXME: implement
    self reply UserResponseGetState(userId, this._userState)
  }

  def onProcessResourceEvent(event: ProcessResourceEvent): Unit = {
  }


  private[this] def D_userID = {
    this._userState match {
      case null ⇒
        "???"

      case userState ⇒
        userState.userID
    }
  }
  private[this] def DEBUG(fmt: String, args: Any*) =
    logger.debug("UserActor[%s]: %s".format(D_userID, fmt.format(args: _*)))

  private[this] def INFO(fmt: String, args: Any*) =
    logger.info("UserActor[%s]: %s".format(D_userID, fmt.format(args: _*)))

  private[this] def WARN(fmt: String, args: Any*) =
    logger.warn("UserActor[%s]: %s".format(D_userID, fmt.format(args: _*)))

  private[this] def ERROR(fmt: String, args: Any*) =
    logger.error("UserActor[%s]: %s".format(D_userID, fmt.format(args: _*)))

  private[this] def ERROR(t: Throwable, fmt: String, args: Any*) =
      logger.error("UserActor[%s]: %s".format(D_userID, fmt.format(args: _*)), t)
}
