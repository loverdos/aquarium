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
import gr.grnet.aquarium.actor.message.event.{ProcessResourceEvent, ProcessIMEvent}
import gr.grnet.aquarium.actor.message.config.{InitializeUserState, ActorProviderConfigured, AquariumPropertiesLoaded}
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.event.model.im.IMEventModel
import gr.grnet.aquarium.actor.message.{GetUserStateResponse, GetUserBalanceResponseData, GetUserBalanceResponse, GetUserStateRequest, GetUserBalanceRequest}
import gr.grnet.aquarium.util.{LogHelpers, shortClassNameOf, shortNameOfClass, shortNameOfType}
import gr.grnet.aquarium.computation.reason.{RealtimeBillingCalculation, InitialUserActorSetup, UserStateChangeReason, IMEventArrival}
import gr.grnet.aquarium.AquariumInternalError
import gr.grnet.aquarium.computation.state.parts.IMStateSnapshot
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.computation.state.{UserStateBootstrap, UserState}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class UserActor extends ReflectiveRoleableActor {
  private[this] var _userID: String = "<?>"
  private[this] var _imState: IMStateSnapshot = _
  private[this] var _userState: UserState = _
  private[this] var _latestResourceEventOccurredMillis = TimeHelpers.nowMillis() // some valid datetime

  self.lifeCycle = Temporary

  private[this] def _shutmedown(): Unit = {
    if(haveUserState) {
      UserActorCache.invalidate(_userID)
    }

    self.stop()
  }

  override protected def onThrowable(t: Throwable, message: AnyRef) = {
    LogHelpers.logChainOfCauses(logger, t)
    ERROR(t, "Terminating due to: %s(%s)", shortClassNameOf(t), t.getMessage)

    _shutmedown()
  }

  def role = UserActorRole

  private[this] def userStateComputations = aquarium.userStateComputations

  private[this] def stdUserStateStoreFunc = (userState: UserState) ⇒ {
    aquarium.userStateStore.insertUserState(userState)
  }

  private[this] def _timestampTheshold = {
    aquarium.userStateTimestampThreshold
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

  private[this] def _updateIMStateRoleHistory(imEvent: IMEventModel, roleCheck: Option[String]) = {
    if(haveIMState) {
      val (newState,
           creationTimeChanged,
           activationTimeChanged,
           roleChanged) = this._imState.updatedWithEvent(imEvent, roleCheck)

      this._imState = newState
      (creationTimeChanged, activationTimeChanged, roleChanged)
    } else {
      this._imState = IMStateSnapshot.initial(imEvent)
      (
        imEvent.isCreateUser,
        true, // first activation status is a change by default??
        true  // first role is a change by default??
        )
    }
  }

  /**
   * Creates the IMStateSnapshot and returns the number of updates it made to it.
   */
  private[this] def createInitialIMState(): Unit = {
    val store = aquarium.imEventStore

    var _roleCheck = None: Option[String]

    // this._userID is already set up
    store.foreachIMEventInOccurrenceOrder(this._userID) { imEvent ⇒
      DEBUG("Replaying %s", imEvent)

      val (creationTimeChanged, activationTimeChanged, roleChanged) = _updateIMStateRoleHistory(imEvent, _roleCheck)
      _roleCheck = this._imState.roleHistory.lastRoleName

      DEBUG(
        "(creationTimeChanged, activationTimeChanged, roleChanged)=(%s, %s, %s) using %s",
        creationTimeChanged, activationTimeChanged, roleChanged,
        imEvent
      )
    }

    DEBUG("Initial %s = %s", shortNameOfType[IMStateSnapshot], this._imState.toJsonString)
    logSeparator()
  }

  /**
   * Resource events are processed only if the user has been activated.
   */
  private[this] def shouldProcessResourceEvents: Boolean = {
    haveIMState && this._imState.hasBeenCreated
  }

  private[this] def loadUserStateAndUpdateRoleHistory(): Unit = {
    val userCreationMillis = this._imState.userCreationMillis.get
    val initialRole = this._imState.roleHistory.firstRole.get.name

    val userStateBootstrap = UserStateBootstrap(
      this._userID,
      userCreationMillis,
      initialRole,
      aquarium.initialAgreementForRole(initialRole, userCreationMillis),
      aquarium.initialBalanceForRole(initialRole, userCreationMillis)
    )

    val now = TimeHelpers.nowMillis()
    val userState = userStateComputations.doMonthBillingUpTo(
      BillingMonthInfo.fromMillis(now),
      now,
      userStateBootstrap,
      aquarium.currentResourcesMap,
      InitialUserActorSetup(),
      stdUserStateStoreFunc,
      None
    )

    this._userState = userState

    // Final touch: Update role history
    if(haveIMState && haveUserState) {
      if(this._userState.roleHistory != this._imState.roleHistory) {
        this._userState = newUserStateWithUpdatedRoleHistory(InitialUserActorSetup())
      }
    }
  }

  private[this] def createInitialUserState(event: InitializeUserState): Unit = {
    if(!haveIMState) {
      // Should have been created from `createIMState()`
      DEBUG("Cannot create user state from %s, since %s = %s", event, shortNameOfClass(classOf[IMStateSnapshot]), this._imState)
      return
    }

    if(!this._imState.hasBeenCreated) {
      // Cannot set the initial state!
      DEBUG("Cannot create %s from %s, since user has not been created", shortNameOfType[UserState], event)
      return
    }

    // We will also need this functionality when receiving IMEvents,
    // so we place it in a method
    loadUserStateAndUpdateRoleHistory()

    if(haveUserState) {
      DEBUG("Initial %s = %s", shortNameOfType[UserState], this._userState.toJsonString)
      logSeparator()
    }
  }

  def onInitializeUserState(event: InitializeUserState): Unit = {
    this._userID = event.userID
    DEBUG("Got %s", event)

    createInitialIMState()
    createInitialUserState(event)
  }

  /**
   * Creates a new user state, taking into account the latest role history in IM state snapshot.
   * Having an IM state snapshot is a prerequisite, otherwise you get an exception; so check before you
   * call this.
   */
  private[this] def newUserStateWithUpdatedRoleHistory(stateChangeReason: UserStateChangeReason): UserState = {
    // FIXME: Also update agreement
    this._userState.newWithRoleHistory(this._imState.roleHistory, stateChangeReason)
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
      throw new AquariumInternalError("Got %s while uninitialized".format(processEvent))
    }

    if(this._imState.latestIMEvent.id == imEvent.id) {
      // This happens when the actor is brought to life, then immediately initialized, and then
      // sent the first IM event. But from the initialization procedure, this IM event will have
      // already been loaded from DB!
      INFO("Ignoring first %s just after %s birth", imEvent.toDebugString, shortClassNameOf(this))
      logSeparator()

      return
    }

    val (creationTimeChanged,
         activationTimeChanged,
         roleChanged) = _updateIMStateRoleHistory(imEvent, this._imState.roleHistory.lastRoleName)

    DEBUG(
      "(creationTimeChanged, activationTimeChanged, roleChanged)=(%s, %s, %s) using %s",
      creationTimeChanged, activationTimeChanged, roleChanged,
      imEvent
    )

    // Must also update user state if we know when in history the life of a user begins
    if(creationTimeChanged) {
      if(!haveUserState) {
        loadUserStateAndUpdateRoleHistory()
        INFO("Loaded %s due to %s", shortNameOfType[UserState], imEvent)
      } else {
        // Just update role history
        this._userState = newUserStateWithUpdatedRoleHistory(IMEventArrival(imEvent))
        INFO("Updated %s due to %s", shortNameOfType[UserState], imEvent)
      }
    }

    DEBUG("Updated %s = %s", shortNameOfType[IMStateSnapshot], this._imState.toJsonString)
    logSeparator()
  }

  def onProcessResourceEvent(event: ProcessResourceEvent): Unit = {
    val rcEvent = event.rcEvent

    if(!shouldProcessResourceEvents) {
      // This means the user has not been created (at least, as far as Aquarium is concerned).
      // So, we do not process any resource event
      DEBUG("Not processing %s", rcEvent.toJsonString)
      logSeparator()

      return
    }

    // Since the latest resource event per resource is recorded in the user state,
    // we do not need to query the store. Just query the in-memory state.
    // Note: This is a similar situation with the first IMEvent received right after the user
    //       actor is created.
    if(this._userState.isLatestResourceEventIDEqualTo(rcEvent.id)) {
      INFO("Ignoring first %s just after %s birth", rcEvent.toDebugString, shortClassNameOf(this))
      logSeparator()

      return
    }

    val now = TimeHelpers.nowMillis()
    val dt  = now - this._latestResourceEventOccurredMillis
    val belowThreshold = dt <= _timestampTheshold

    if(belowThreshold) {
      this._latestResourceEventOccurredMillis = event.rcEvent.occurredMillis

      DEBUG("Below threshold (%s ms). Not processing %s", this._timestampTheshold, rcEvent.toJsonString)
      logSeparator()

      return
    }

    val userID = this._userID
    val userCreationMillis = this._imState.userCreationMillis.get
    val initialRole = this._imState.roleHistory.firstRoleName.getOrElse(aquarium.defaultInitialUserRole)
    val initialAgreement = aquarium.initialAgreementForRole(initialRole, userCreationMillis)
    val initialCredits   = aquarium.initialBalanceForRole(initialRole, userCreationMillis)
    val userStateBootstrap = UserStateBootstrap(
      userID,
      userCreationMillis,
      initialRole,
      initialAgreement,
      initialCredits
    )
    val billingMonthInfo = BillingMonthInfo.fromMillis(now)
    val currentResourcesMap = aquarium.currentResourcesMap
    val calculationReason = RealtimeBillingCalculation(None, now)
    val eventOccurredMillis = rcEvent.occurredMillis

//    DEBUG("Using %s", currentResourcesMap.toJsonString)

    this._userState = aquarium.userStateComputations.doMonthBillingUpTo(
      billingMonthInfo,
      // Take into account that the event may be out-of-sync.
      // TODO: Should we use this._latestResourceEventOccurredMillis instead of now?
      now max eventOccurredMillis,
      userStateBootstrap,
      currentResourcesMap,
      calculationReason,
      stdUserStateStoreFunc
    )

    this._latestResourceEventOccurredMillis = event.rcEvent.occurredMillis

    DEBUG("Updated %s = %s", shortClassNameOf(this._userState), this._userState.toJsonString)
    logSeparator()
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
        // (no IMState, have UserState)
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
