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

import gr.grnet.aquarium.util.shortClassNameOf
import message.config.{ActorProviderConfigured, AquariumPropertiesLoaded}
import akka.config.Supervision.Temporary
import gr.grnet.aquarium.Configurator
import gr.grnet.aquarium.util.date.{TimeHelpers, MutableDateCalc}
import gr.grnet.aquarium.actor.message.event.{ProcessResourceEvent, ProcessIMEvent}
import gr.grnet.aquarium.actor.message.{GetUserStateResponse, GetUserBalanceResponse, GetUserStateRequest, GetUserBalanceRequest}
import gr.grnet.aquarium.computation.data.IMStateSnapshot
import gr.grnet.aquarium.computation.UserState
import gr.grnet.aquarium.event.model.im.IMEventModel

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class UserActor extends ReflectiveRoleableActor {
  private[this] var _imState: IMStateSnapshot = _
  private[this] var _userState: UserState = _

  self.lifeCycle = Temporary

  private[this] def _userID = this._userState.userID
  private[this] def _shutmedown(): Unit = {
    if(_haveUserState) {
      UserActorCache.invalidate(_userID)
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

  private[this] def _timestampTheshold =
    _configurator.props.getLong(Configurator.Keys.user_state_timestamp_threshold).getOr(10000)


  private[this] def _haveUserState = {
    this._userState ne null
  }

  private[this] def _haveIMState = {
    this._imState ne null
  }

  def onAquariumPropertiesLoaded(event: AquariumPropertiesLoaded): Unit = {
  }

  def onActorProviderConfigured(event: ActorProviderConfigured): Unit = {
  }

  private[this] def _getAgreementNameForNewUser(imEvent: IMEventModel): String = {
    // FIXME: Implement based on the role
    "default"
  }

  def onProcessIMEvent(event: ProcessIMEvent): Unit = {
    val now = TimeHelpers.nowMillis()

    val imEvent = event.imEvent
    val hadIMState = _haveIMState

    if(hadIMState) {
      val newOccurredMillis = imEvent.occurredMillis
      val currentOccurredMillis = this._imState.imEvent.occurredMillis

      if(newOccurredMillis < currentOccurredMillis) {
        INFO(
          "Ignoring older IMEvent: [%s] < [%s]",
          new MutableDateCalc(newOccurredMillis).toYYYYMMDDHHMMSSSSS,
          new MutableDateCalc(currentOccurredMillis).toYYYYMMDDHHMMSSSSS)

        return
      }
    }

    this._imState = IMStateSnapshot(imEvent)
    DEBUG("%s %s", if(hadIMState) "Update" else "Set", shortClassNameOf(this._imState))
  }

  def onGetUserBalanceRequest(event: GetUserBalanceRequest): Unit = {
    val userId = event.userID
    // FIXME: Implement
    self reply GetUserBalanceResponse(userId, Right(_userState.creditsSnapshot.creditAmount))
  }

  def onGetUserStateRequest(event: GetUserStateRequest): Unit = {
    val userId = event.userID
   // FIXME: Implement
    self reply GetUserStateResponse(userId, Right(this._userState))
  }

  def onProcessResourceEvent(event: ProcessResourceEvent): Unit = {
    val rcEvent = event.rcEvent

    logger.info("Got\n{}", rcEvent.toJsonString)
  }


  private[this] def D_userID = {
    if(this._userState eq null)
      if(this._imState eq null)
        "<NOT INITIALIZED>"
      else
        this._imState.imEvent.userID
    else
      this._userState.userID
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
