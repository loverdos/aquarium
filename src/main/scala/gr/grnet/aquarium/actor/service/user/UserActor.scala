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
import gr.grnet.aquarium.util.{shortClassNameOf, shortNameOfClass}
import gr.grnet.aquarium.actor.message.event.{ProcessResourceEvent, ProcessIMEvent}
import gr.grnet.aquarium.computation.data.IMStateSnapshot
import gr.grnet.aquarium.event.model.im.IMEventModel
import gr.grnet.aquarium.actor.message.config.{InitializeUserState, ActorProviderConfigured, AquariumPropertiesLoaded}
import gr.grnet.aquarium.actor.message.{GetUserBalanceResponseData, GetUserBalanceResponse, GetUserStateRequest, GetUserBalanceRequest}
import gr.grnet.aquarium.computation.{BillingMonthInfo, UserStateBootstrappingData, UserState}
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.logic.accounting.Policy
import gr.grnet.aquarium.computation.reason.InitialUserStateSetup

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class UserActor extends ReflectiveRoleableActor {
  private[this] var _userID: String = "<?>"
  private[this] var _imState: IMStateSnapshot = _
  private[this] var _userState: UserState = _

  self.lifeCycle = Temporary

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

  private[this] def aquarium: Aquarium = Aquarium.Instance
  private[this] def userStateComputations = aquarium.userStateComputations

  private[this] def _timestampTheshold = {
    aquarium.props.getLong(Aquarium.Keys.user_state_timestamp_threshold).getOr(10000)
  }

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

  private[this] def createIMState(event: InitializeUserState): Unit = {
    val userID = event.userID
    val store = aquarium.imEventStore
    // TODO: Optimization: Since IMState only records roles, we should incrementally
    // TODO:               built it only for those IMEvents that changed the role.
    store.replayIMEventsInOccurrenceOrder(userID) { imEvent ⇒
      logger.debug("Replaying %s".format(imEvent))

      val newState = this._imState match {
        case null ⇒
          IMStateSnapshot.initial(imEvent)

        case currentState ⇒
          currentState.updateHistoryWithEvent(imEvent)
      }

      this._imState = newState
    }

    DEBUG("Recomputed %s = %s", shortNameOfClass(classOf[IMStateSnapshot]), this._imState)
  }

  /**
   * Resource events are processed only if the user has been activated.
   */
  private[this] def shouldProcessResourceEvents: Boolean = {
    _haveIMState && this._imState.hasBeenActivated
  }

  private[this] def createUserState(event: InitializeUserState): Unit = {
    val userID = event.userID
    val referenceTime = event.referenceTimeMillis

    if(!_haveIMState) {
      // Should have been created from `createIMState()`
      DEBUG("Cannot create user state from %s, since %s = %s", event, shortNameOfClass(classOf[IMStateSnapshot]), this._imState)
      return
    }

    if(!this._imState.hasBeenActivated) {
      // Cannot set the initial state!
      DEBUG("Cannot create user state from %s, since user is inactive", event)
      return
    }

    val userActivationMillis = this._imState.userActivationMillis.get
    val initialRole = this._imState.roleHistory.firstRole.get.name

    val userStateBootstrap = UserStateBootstrappingData(
      this._userID,
      userActivationMillis,
      initialRole,
      aquarium.initialAgreementForRole(initialRole, userActivationMillis),
      aquarium.initialBalanceForRole(initialRole, userActivationMillis)
    )

    userStateComputations.doFullMonthlyBilling(
      userStateBootstrap,
      BillingMonthInfo.fromMillis(TimeHelpers.nowMillis()),
      aquarium.currentResourcesMap,
      InitialUserStateSetup,
      None
    )
  }

  def onInitializeUserState(event: InitializeUserState): Unit = {
    val userID = event.userID
    this._userID = userID
    DEBUG("Got %s", event)

    createIMState(event)
    createUserState(event)
  }

  /**
   * Process [[gr.grnet.aquarium.event.model.im.IMEventModel]]s.
   * When this method is called, we assume that all proper checks have been made and it
   * is OK to proceed with the event processing.
   */
  def onProcessIMEvent(processEvent: ProcessIMEvent): Unit = {
    val imEvent = processEvent.imEvent

    if(!_haveIMState) {
      // This is an error. Should have been initialized from somewhere ...
      throw new Exception("Got %s while being uninitialized".format(processEvent))
    }

    if(this._imState.latestIMEvent.id == imEvent.id) {
      // This happens when the actor is brought to life, then immediately initialized, and then
      // sent the first IM event. But from the initialization procedure, this IM event will have
      // already been loaded from DB!
      INFO("Ignoring first %s after birth", imEvent.toDebugString)
      return
    }

    this._imState = this._imState.updateHistoryWithEvent(imEvent)

    INFO("Update %s = %s", shortClassNameOf(this._imState), this._imState)
  }

  def onProcessResourceEvent(event: ProcessResourceEvent): Unit = {
    val rcEvent = event.rcEvent

    if(!shouldProcessResourceEvents) {
      // This means the user has not been activated. So, we do not process any resource event
      DEBUG("Not processing %s", rcEvent.toJsonString)
      return
    }
  }


  def onGetUserBalanceRequest(event: GetUserBalanceRequest): Unit = {
    val userID = event.userID

    if(!_haveIMState) {
      // No IMEvent has arrived, so this user is virtually unknown
      self reply GetUserBalanceResponse(Left("User not found"), 404/*Not found*/)
    }
    else if(!_haveUserState) {
      // The user is known but we have no state.
      // Ridiculous. Should have been created at least during initialization.
    }

    if(!_haveUserState) {
      self reply GetUserBalanceResponse(Left("Not found"), 404/*Not found*/)
    } else {
      self reply GetUserBalanceResponse(Right(GetUserBalanceResponseData(userID, this._userState.totalCredits)))
    }
  }

  def onGetUserStateRequest(event: GetUserStateRequest): Unit = {
    val userId = event.userID
   // FIXME: Implement
//    self reply GetUserStateResponse(userId, Right(this._userState))
  }

  private[this] def D_userID = {
    this._userID
  }

  private[this] def DEBUG(fmt: String, args: Any*) =
    logger.debug("User[%s]: %s".format(D_userID, fmt.format(args: _*)))

  private[this] def INFO(fmt: String, args: Any*) =
    logger.info("User[%s]: %s".format(D_userID, fmt.format(args: _*)))

  private[this] def WARN(fmt: String, args: Any*) =
    logger.warn("User[%s]: %s".format(D_userID, fmt.format(args: _*)))

  private[this] def ERROR(fmt: String, args: Any*) =
    logger.error("User[%s]: %s".format(D_userID, fmt.format(args: _*)))

  private[this] def ERROR(t: Throwable, fmt: String, args: Any*) =
    logger.error("User[%s]: %s".format(D_userID, fmt.format(args: _*)), t)
}
