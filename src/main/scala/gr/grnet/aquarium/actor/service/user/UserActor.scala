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

import akka.config.Supervision.Temporary
import gr.grnet.aquarium.Aquarium
import gr.grnet.aquarium.actor.message.event.{ProcessResourceEvent, ProcessIMEvent}
import gr.grnet.aquarium.actor.message.{GetUserStateRequest, GetUserBalanceRequest}
import gr.grnet.aquarium.computation.data.IMStateSnapshot
import gr.grnet.aquarium.event.model.im.IMEventModel
import gr.grnet.aquarium.actor.message.config.{InitializeUserState, ActorProviderConfigured, AquariumPropertiesLoaded}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class UserActor extends ReflectiveRoleableActor {
  private[this] var _imState: IMStateSnapshot = _
//  private[this] var _userState: UserState = _
//  private[this] var _newUserState: NewUserState = _

  self.lifeCycle = Temporary

//  private[this] def _userID = this._newUserState.userID
  private[this] def _shutmedown(): Unit = {
//    if(_haveUserState) {
//      UserActorCache.invalidate(_userID)
//    }

    self.stop()
  }

  override protected def onThrowable(t: Throwable, message: AnyRef) = {
    logChainOfCauses(t)
//    ERROR(t, "Terminating due to: %s(%s)", shortClassNameOf(t), t.getMessage)

    _shutmedown()
  }

  def role = UserActorRole

  private[this] def aquarium: Aquarium = Aquarium.Instance

  private[this] def _timestampTheshold =
    aquarium.props.getLong(Aquarium.Keys.user_state_timestamp_threshold).getOr(10000)


//  private[this] def _haveUserState = {
//    this._newUserState ne null
//  }

  private[this] def _haveIMState = {
    this._imState ne null
  }

  def onAquariumPropertiesLoaded(event: AquariumPropertiesLoaded): Unit = {
  }

  def onActorProviderConfigured(event: ActorProviderConfigured): Unit = {
  }

  private[this] def createIMState(userID: String): Unit = {
    val store = aquarium.imEventStore
    // TODO: Optimization: Since IMState only records roles, we should incrementally
    // TODO:               built it only for those IMEvents that changed the role.
    store.replayIMEventsInOccurrenceOrder(userID) { imEvent ⇒
      logger.debug("Replaying %s".format(imEvent))

      val newState = this._imState match {
        case null ⇒
          IMStateSnapshot.initial(imEvent)

        case currentState ⇒
          currentState.copyWithEvent(imEvent)
      }

      this._imState = newState
    }

    logger.debug("Recomputed %s".format(this._imState))
  }

  def onInitializeUserState(event: InitializeUserState): Unit = {
    logger.debug("Got %s".format(event))
    createIMState(event.userID)
  }

  private[this] def _getAgreementNameForNewUser(imEvent: IMEventModel): String = {
    // FIXME: Implement based on the role
    "default"
  }

  /**
   * Process [[gr.grnet.aquarium.event.model.im.IMEventModel]]s.
   * When this method is called, we assume that all proper checks have been made and it
   * is OK to proceed with the event processing.
   */
  def onProcessIMEvent(processEvent: ProcessIMEvent): Unit = {
    val imEvent = processEvent.imEvent
    val hadIMState = _haveIMState

    if(hadIMState) {
      if(this._imState.latestIMEvent.id == imEvent.id) {
        // This happens when the actor is brought to life, then immediately initialized, and then
        // sent the first IM event. But from the initialization procedure, this IM event will have
        // already been loaded from DB!
        logger.debug("Ignoring first %s after birth".format(imEvent.toDebugString))
        return
      }

      this._imState = this._imState.copyWithEvent(imEvent)
    } else {
      this._imState = IMStateSnapshot.initial(imEvent)
    }

//    DEBUG("%s %s = %s", if(hadIMState) "Update" else "Set", shortClassNameOf(this._imState), this._imState)
  }

  def onGetUserBalanceRequest(event: GetUserBalanceRequest): Unit = {
    val userId = event.userID
    // FIXME: Implement
//    self reply GetUserBalanceResponse(userId, Right(_userState.creditsSnapshot.creditAmount))
  }

  def onGetUserStateRequest(event: GetUserStateRequest): Unit = {
    val userId = event.userID
   // FIXME: Implement
//    self reply GetUserStateResponse(userId, Right(this._userState))
  }

  def onProcessResourceEvent(event: ProcessResourceEvent): Unit = {
    val rcEvent = event.rcEvent

    logger.info("Got\n{}", rcEvent.toJsonString)
  }


//  private[this] def D_userID = {
//    if(this._newUserState eq null)
//      if(this._imState eq null)
//        "<NOT INITIALIZED>"
//      else
//        this._imState.latestIMEvent.userID
//    else
//      this._newUserState.userID
//  }
//
//  private[this] def DEBUG(fmt: String, args: Any*) =
//    logger.debug("UserActor[%s]: %s".format(D_userID, fmt.format(args: _*)))
//
//  private[this] def INFO(fmt: String, args: Any*) =
//    logger.info("UserActor[%s]: %s".format(D_userID, fmt.format(args: _*)))
//
//  private[this] def WARN(fmt: String, args: Any*) =
//    logger.warn("UserActor[%s]: %s".format(D_userID, fmt.format(args: _*)))
//
//  private[this] def ERROR(fmt: String, args: Any*) =
//    logger.error("UserActor[%s]: %s".format(D_userID, fmt.format(args: _*)))
//
//  private[this] def ERROR(t: Throwable, fmt: String, args: Any*) =
//    logger.error("UserActor[%s]: %s".format(D_userID, fmt.format(args: _*)), t)
}
