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
import gr.grnet.aquarium.util.{shortClassNameOf, shortNameOfClass, shortNameOfType}
import gr.grnet.aquarium.actor.message.event.{ProcessResourceEvent, ProcessIMEvent}
import gr.grnet.aquarium.computation.data.IMStateSnapshot
import gr.grnet.aquarium.actor.message.config.{InitializeUserState, ActorProviderConfigured, AquariumPropertiesLoaded}
import gr.grnet.aquarium.computation.{BillingMonthInfo, UserStateBootstrappingData, UserState}
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.event.model.im.IMEventModel
import gr.grnet.aquarium.{AquariumException, Aquarium}
import gr.grnet.aquarium.actor.message.{GetUserStateResponse, GetUserBalanceResponseData, GetUserBalanceResponse, GetUserStateRequest, GetUserBalanceRequest}
import gr.grnet.aquarium.computation.reason.{InitialUserActorSetup, UserStateChangeReason, IMEventArrival, InitialUserStateSetup}

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
    if(haveUserState) {
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

  private[this] def haveUserState = {
    this._userState ne null
  }

  private[this] def haveIMState = {
    this._imState ne null
  }

  def onAquariumPropertiesLoaded(event: AquariumPropertiesLoaded): Unit = {
  }

  def onActorProviderConfigured(event: ActorProviderConfigured): Unit = {
  }

  private[this] def _updateIMStateRoleHistory(imEvent: IMEventModel): (Boolean, Boolean, String) = {
    if(haveIMState) {
      val currentRole = this._imState.roleHistory.lastRole.map(_.name).getOrElse(null)
//      logger.debug("Current role = %s".format(currentRole))

      if(imEvent.role != currentRole) {
//        logger.debug("New role = %s".format(imEvent.role))
        this._imState = this._imState.updateRoleHistoryWithEvent(imEvent)
        (true, false, "")
      } else {
        val noUpdateReason = "Same role '%s'".format(currentRole)
//        logger.debug(noUpdateReason)
        (false, false, noUpdateReason)
      }
    } else {
      this._imState = IMStateSnapshot.initial(imEvent)
      (true, true, "")
    }
  }

  /**
   * Creates the IMStateSnapshot and returns the number of updates it made to it.
   */
  private[this] def createIMState(event: InitializeUserState): Int = {
    val userID = event.userID
    val store = aquarium.imEventStore

    var _updateCount = 0

    store.replayIMEventsInOccurrenceOrder(userID) { imEvent ⇒
      DEBUG("Replaying %s", imEvent)

      val (updated, firstUpdate, noUpdateReason) = _updateIMStateRoleHistory(imEvent)
      if(updated) {
        _updateCount = _updateCount + 1
        DEBUG("Updated %s for role '%s'", shortNameOfType[IMStateSnapshot], imEvent.role)
      } else {
        DEBUG("Not updated %s due to: %s", shortNameOfType[IMStateSnapshot], noUpdateReason)
      }
    }

    if(_updateCount > 0)
      DEBUG("Computed %s = %s", shortNameOfType[IMStateSnapshot], this._imState)
    else
      DEBUG("Not computed %s", shortNameOfType[IMStateSnapshot])

    _updateCount
  }

  /**
   * Resource events are processed only if the user has been activated.
   */
  private[this] def shouldProcessResourceEvents: Boolean = {
    haveIMState && this._imState.hasBeenActivated
  }

  private[this] def createUserState(event: InitializeUserState): Unit = {
    if(!haveIMState) {
      // Should have been created from `createIMState()`
      DEBUG("Cannot create user state from %s, since %s = %s", event, shortNameOfClass(classOf[IMStateSnapshot]), this._imState)
      return
    }

    if(!this._imState.hasBeenActivated) {
      // Cannot set the initial state!
      DEBUG("Cannot create %s from %s, since user is inactive", shortNameOfType[UserState], event)
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

    val userState = userStateComputations.doFullMonthlyBilling(
      userStateBootstrap,
      BillingMonthInfo.fromMillis(TimeHelpers.nowMillis()),
      aquarium.currentResourcesMap,
      InitialUserStateSetup,
      None
    )

    this._userState = userState

    // Final touch: Update role history
    if(haveIMState && haveUserState) {
      // FIXME: Not satisfied with this redundant info
      if(this._userState.roleHistory != this._imState.roleHistory) {
        this._userState = newUserStateWithUpdatedRoleHistory(InitialUserActorSetup)
      }
    }

    if(haveUserState) {
      DEBUG("%s = %s", shortNameOfType[UserState], this._userState)
    }
  }

  def onInitializeUserState(event: InitializeUserState): Unit = {
    val userID = event.userID
    this._userID = userID
    DEBUG("Got %s", event)

    createIMState(event)
    createUserState(event)
  }

  /**
   * Creates a new user state, taking into account the latest role history in IM state snapshot.
   * Having an IM state snapshot is a prerequisite, otherwise you get an exception; so check before you
   * call this.
   */
  private[this] def newUserStateWithUpdatedRoleHistory(stateChangeReason: UserStateChangeReason): UserState = {
    this._userState.copy(
      roleHistory = this._imState.roleHistory,
      // FIXME: Also update agreement
      stateChangeCounter = this._userState.stateChangeCounter + 1,
      lastChangeReason = stateChangeReason
    )
  }

  /**
   * Process [[gr.grnet.aquarium.event.model.im.IMEventModel]]s.
   * When this method is called, we assume that all proper checks have been made and it
   * is OK to proceed with the event processing.
   */
  def onProcessIMEvent(processEvent: ProcessIMEvent): Unit = {
    val imEvent = processEvent.imEvent

    if(!haveIMState) {
      // This is an error. Should have been initialized from somewhere ...
      throw new AquariumException("Got %s while being uninitialized".format(processEvent))
    }

    if(this._imState.latestIMEvent.id == imEvent.id) {
      // This happens when the actor is brought to life, then immediately initialized, and then
      // sent the first IM event. But from the initialization procedure, this IM event will have
      // already been loaded from DB!
      INFO("Ignoring first %s just after %s birth", imEvent.toDebugString, shortClassNameOf(this))
      logSeparator()
      return
    }

    val (updated, firstUpdate, noUpdateReason) = _updateIMStateRoleHistory(imEvent)

    if(updated) {
      DEBUG("Updated %s = %s", shortClassNameOf(this._imState), this._imState)

      // Must also update user state
      if(shouldProcessResourceEvents) {
        if(haveUserState) {
          DEBUG("Also updating %s with new %s",
            shortClassNameOf(this._userState),
            shortClassNameOf(this._imState.roleHistory)
          )

          this._userState = newUserStateWithUpdatedRoleHistory(IMEventArrival(imEvent))
        }
      }
    } else {
      DEBUG("Not updating %s from %s due to: %s", shortNameOfType[IMStateSnapshot], imEvent, noUpdateReason)
    }

    logSeparator()
  }

  def onProcessResourceEvent(event: ProcessResourceEvent): Unit = {
    val rcEvent = event.rcEvent

    if(!shouldProcessResourceEvents) {
      // This means the user has not been activated. So, we do not process any resource event
      DEBUG("Not processing %s", rcEvent.toJsonString)
      logSeparator()
      return
    }
  }


  def onGetUserBalanceRequest(event: GetUserBalanceRequest): Unit = {
    val userID = event.userID

    (haveIMState, haveUserState) match {
      case (true, true) ⇒
        // (have IMState, have UserState)
        this._imState.hasBeenActivated match {
          case true ⇒
            // (have IMState, activated, have UserState)
            self reply GetUserBalanceResponse(Right(GetUserBalanceResponseData(userID, this._userState.totalCredits)))

          case false ⇒
            // (have IMState, not activated, have UserState)
            // Since we have user state, we should have been activated
            self reply GetUserBalanceResponse(Left("Internal Server Error [AQU-BAL-0001]"), 500)
        }

      case (true, false) ⇒
        // (have IMState, no UserState)
        this._imState.hasBeenActivated match {
          case true  ⇒
            // (have IMState, activated, no UserState)
            // Since we are activated, we should have some state.
            self reply GetUserBalanceResponse(Left("Internal Server Error [AQU-BAL-0002]"), 500)
          case false ⇒
            // (have IMState, not activated, no UserState)
            // The user is virtually unknown
            self reply GetUserBalanceResponse(Left("User %s not activated [AQU-BAL-0003]".format(userID)), 404 /*Not found*/)
        }

      case (false, true) ⇒
        // (no IMState, have UserState
        // A bit ridiculous situation
        self reply GetUserBalanceResponse(Left("Unknown user %s [AQU-BAL-0004]".format(userID)), 404/*Not found*/)

      case (false, false) ⇒
        // (no IMState, no UserState)
        self reply GetUserBalanceResponse(Left("Unknown user %s [AQU-BAL-0005]".format(userID)), 404/*Not found*/)
    }
  }

  def onGetUserStateRequest(event: GetUserStateRequest): Unit = {
    haveUserState match {
      case true ⇒
        self reply GetUserStateResponse(Right(this._userState))

      case false ⇒
        self reply GetUserStateResponse(Left("No state for user %s [AQU-STA-0006]".format(event.userID)))
    }
  }

  private[this] def D_userID = {
    this._userID
  }

  private[this] def DEBUG(fmt: String, args: Any*) =
    logger.debug("[%s] - %s".format(D_userID, fmt.format(args: _*)))

  private[this] def INFO(fmt: String, args: Any*) =
    logger.info("[%s] - %s".format(D_userID, fmt.format(args: _*)))

  private[this] def WARN(fmt: String, args: Any*) =
    logger.warn("[%s] - %s".format(D_userID, fmt.format(args: _*)))

  private[this] def ERROR(fmt: String, args: Any*) =
    logger.error("[%s] - %s".format(D_userID, fmt.format(args: _*)))

  private[this] def ERROR(t: Throwable, fmt: String, args: Any*) =
    logger.error("[%s] - %s".format(D_userID, fmt.format(args: _*)), t)
}
