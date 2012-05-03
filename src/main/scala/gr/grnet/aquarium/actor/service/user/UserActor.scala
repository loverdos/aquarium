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
import gr.grnet.aquarium.{AquariumInternalError, AquariumException, Configurator}


/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class UserActor extends ReflectiveRoleableActor {
  private[this] var _userID: String = _
  private[this] var _userState: UserState = _

  self.lifeCycle = Temporary

  private[this] def _shutmedown(): Unit = {
    if(_haveFullState) {
      UserActorCache.invalidate(this._userID)
    }

    self.stop()
  }

  override protected def onThrowable(t: Throwable, message: AnyRef) = {
    logChainOfCauses(t)
    ERROR(t, "Terminating due to: %s(%s)", shortClassNameOf(t), t.getMessage)

    _shutmedown()
  }

  def role = UserActorRole

  private[this] def _configurator: Configurator = Configurator.MasterConfigurator
//  private[this] def _userId = _userState.userId

  private[this] def _timestampTheshold =
    _configurator.props.getLong(Configurator.Keys.user_state_timestamp_threshold).getOr(10000)


  private[this] def _haveFullState = {
    (this._userID ne null) && (this._userState ne null)
  }

  private[this] def _havePartialState = {
    (this._userID ne null) && (this._userState eq null)
  }


  def onAquariumPropertiesLoaded(event: AquariumPropertiesLoaded): Unit = {
  }

  def onActorProviderConfigured(event: ActorProviderConfigured): Unit = {
  }

  private[this] def _getAgreementNameForNewUser(imEvent: IMEventModel): String = {
    // FIXME: Implement based on the role
    "default"
  }

  private[this] def processCreateUser(imEvent: IMEventModel): Unit = {
    this._userID = imEvent.userID

    val store = _configurator.storeProvider.userStateStore
    // try find user state. normally should ot exist
    val latestUserStateOpt = store.findLatestUserStateByUserID(this._userID)
    if(latestUserStateOpt.isDefined) {
      logger.error("Got %s(%s, %s) but user already exists. Ingoring".format(
        this._userID,
        shortClassNameOf(imEvent),
        imEvent.eventType))

      return
    }

    val initialAgreementName = _getAgreementNameForNewUser(imEvent)
    val newUserState    = DefaultUserStateComputations.createInitialUserState(
      this._userID,
      imEvent.occurredMillis,
      imEvent.isActive,
      0.0,
      List(imEvent.role),
      initialAgreementName)

    this._userState = newUserState

    // FIXME: If this fails, then the actor must be shut down.
    store.insertUserState(newUserState)
  }

  private[this] def processModifyUser(imEvent: IMEventModel): Unit = {
    val now = TimeHelpers.nowMillis()

    if(!_haveFullState) {
      ERROR("Got %s(%s) but have no state. Shutting down", shortClassNameOf(imEvent), imEvent.eventType)
      _shutmedown()
      return
    }

    this._userState = this._userState.modifyFromIMEvent(imEvent, now)
  }

  def onProcessSetUserID(event: ProcessSetUserID): Unit = {
    this._userID = event.userID
  }

  def onProcessIMEvent(event: ProcessIMEvent): Unit = {
    val now = TimeHelpers.nowMillis()

    val imEvent = event.imEvent
    // If we already have a userID but it does not match the incoming userID, then this is an internal error
    if(_havePartialState && (this._userID != imEvent.userID)) {
      throw new AquariumInternalError(
        "Got userID = %s but already have userID = %s".format(imEvent.userID, this._userID))
    }

    // If we get an IMEvent without having a user state, then we query for the latest user state.
    if(!_haveFullState) {
      val userStateOpt = _configurator.userStateStore.findLatestUserStateByUserID(this._userID)
      this._userState = userStateOpt match {
        case Some(userState) ⇒
          userState

        case None ⇒
          val initialAgreementName = _getAgreementNameForNewUser(imEvent)
          val initialUserState = DefaultUserStateComputations.createInitialUserState(
            this._userID,
            imEvent.occurredMillis,
            imEvent.isActive,
            0.0,
            List(imEvent.role),
            initialAgreementName)

          DEBUG("Got initial state")
          initialUserState
      }
    }

    if(imEvent.isModifyUser && this._userState.isInitial) {
      INFO("Got a '%s' but have not received '%s' yet", imEvent.eventType, IMEventModel.EventTypeNames.create)
      return
    }

    if(imEvent.isCreateUser && !this._userState.isInitial) {
      INFO("Got a '%s' but my state is not initial", imEvent.eventType)
      return
    }

    this._userState = this._userState.modifyFromIMEvent(imEvent, now)

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
    if(this._userID eq null)
      "<NOT INITIALIZED>" // We always get a userID first
    else
      if(this._userState eq null)
        "%s, NO STATE".format(this._userID)
      else
        "%s".format(this._userID)
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
