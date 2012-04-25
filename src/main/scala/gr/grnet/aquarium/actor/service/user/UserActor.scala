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

import java.util.Date
import com.ckkloverdos.maybe.{Failed, NoVal, Just}

import gr.grnet.aquarium.actor._
import gr.grnet.aquarium.Configurator
import gr.grnet.aquarium.user._

import gr.grnet.aquarium.util.Loggable
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.logic.accounting.RoleAgreements
import gr.grnet.aquarium.actor.message.service.router._
import message.config.{ActorProviderConfigured, AquariumPropertiesLoaded}
import gr.grnet.aquarium.event.im.IMEventModel
import gr.grnet.aquarium.event.WalletEntry


/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class UserActor extends ReflectiveAquariumActor {
  @volatile
  private[this] var _userState: UserState = _
  @volatile
  private[this] var _timestampTheshold: Long = _

  def role = UserActorRole

  private[this] def _configurator: Configurator = Configurator.MasterConfigurator
  private[this] def _userId = _userState.userId

  /**
   * Replay the event log for all events that affect the user state.
   */
  def rebuildState(from: Long, to: Long): Unit = {
    val start = TimeHelpers.nowMillis()
    if(_userState == null)
      createBlankState

    //Rebuild state from user events
    val usersDB = _configurator.storeProvider.imEventStore
    val userEvents = usersDB.findIMEventsByUserId(_userId)
    val numUserEvents = userEvents.size
    _userState = replayIMEvents(_userState, userEvents, from, to)

    //Rebuild state from resource events
    val eventsDB = _configurator.storeProvider.resourceEventStore
    val resourceEvents = eventsDB.findResourceEventsByUserIdAfterTimestamp(_userId, from)
    val numResourceEvents = resourceEvents.size
    //    _userState = replayResourceEvents(_userState, resourceEvents, from, to)

    //Rebuild state from wallet entries
    val wallet = _configurator.storeProvider.walletEntryStore
    val walletEnties = wallet.findWalletEntriesAfter(_userId, new Date(from))
    val numWalletEntries = walletEnties.size
    _userState = replayWalletEntries(_userState, walletEnties, from, to)

    INFO(("Rebuilt state from %d events (%d user events, " +
      "%d resource events, %d wallet entries) in %d msec").format(
      numUserEvents + numResourceEvents + numWalletEntries,
      numUserEvents, numResourceEvents, numWalletEntries,
      TimeHelpers.nowMillis() - start))
  }

  /**
   * Create an empty state for a user
   */
  def createBlankState = {
    this._userState = DefaultUserStateComputations.createInitialUserState(this._userId, 0L, true, 0.0)
  }

  /**
   * Replay user events on the provided user state
   */
  def replayIMEvents(initState: UserState, events: List[IMEventModel],
                       from: Long, to: Long): UserState = {
    initState
  }


  /**
   * Replay wallet entries on the provided user state
   */
  def replayWalletEntries(initState: UserState, events: List[WalletEntry],
                          from: Long, to: Long): UserState = {
    initState
  }

  /**
   * Persist current user state
   */
  private[this] def saveUserState(): Unit = {
    _configurator.storeProvider.userStateStore.storeUserState(this._userState) match {
      case Just(record) => record
      case NoVal => ERROR("Unknown error saving state")
      case Failed(e) =>
        ERROR("Saving state failed: %s".format(e));
    }
  }

  def onAquariumPropertiesLoaded(event: AquariumPropertiesLoaded): Unit = {
    this._timestampTheshold = event.props.getLong(Configurator.Keys.user_state_timestamp_threshold).getOr(10000)
    INFO("Setup my timestampTheshold = %s", this._timestampTheshold)
  }

  def onProcessResourceEvent(event: ProcessResourceEvent): Unit = {
    val resourceEvent = event.rcEvent
    if(resourceEvent.userID != this._userId) {
      ERROR("Received %s but my userId = %s".format(event, this._userId))
    } else {
      //ensureUserState()
      //        calcWalletEntries()
      //processResourceEvent(resourceEvent, true)
    }
  }

  private[this] def processCreateUser(event: IMEventModel): Unit = {
    val userId = event.userID
    DEBUG("Creating user from state %s", event)
    val usersDB = _configurator.storeProvider.userStateStore
    usersDB.findUserStateByUserId(userId) match {
      case Just(userState) ⇒
        WARN("User already created, state = %s".format(userState))
      case failed@Failed(e) ⇒
        ERROR("[%s] %s", e.getClass.getName, e.getMessage)
      case NoVal ⇒
        val agreement = RoleAgreements.agreementForRole(event.role)
        DEBUG("User %s assigned agreement %s".format(userId, agreement.name))

        this._userState = DefaultUserStateComputations.createInitialUserState(
          userId,
          event.occurredMillis,
          event.isActive, 0.0, List(event.role), agreement.name)
        saveUserState
        DEBUG("Created and stored %s", this._userState)
    }
  }

  private[this] def processModifyUser(event: IMEventModel): Unit = {
    val now = TimeHelpers.nowMillis()
    val newActive = ActiveStateSnapshot(event.isStateActive, now)

    DEBUG("New active status = %s".format(newActive))

    this._userState = this._userState.copy(activeStateSnapshot = newActive)
  }

  def onProcessIMEvent(event: ProcessIMEvent): Unit = {
    val userEvent = event.imEvent
    if(userEvent.userID != this._userId) {
      ERROR("Received %s but my userId = %s".format(userEvent, this._userId))
    } else {
      if(userEvent.isCreateUser) {
        processCreateUser(userEvent)
      } else if(userEvent.isModifyUser) {
        processModifyUser(userEvent)
      }
    }
  }

  def onRequestUserBalance(event: RequestUserBalance): Unit = {
    val userId = event.userID
    val timestamp = event.timestamp

    if(TimeHelpers.nowMillis() - _userState.newestSnapshotTime > 60 * 1000) {
      //        calcWalletEntries()
    }
    self reply UserResponseGetBalance(userId, _userState.creditsSnapshot.creditAmount)
  }

  def onUserRequestGetState(event: UserRequestGetState): Unit = {
    val userId = event.userID
    if(this._userId != userId) {
      ERROR("Received %s but my userId = %s".format(event, this._userId))
      // TODO: throw an exception here
    } else {
      // FIXME: implement
      ERROR("FIXME: Should have properly computed the user state")
      //      ensureUserState()
      self reply UserResponseGetState(userId, this._userState)
    }
  }

  def onActorProviderConfigured(event: ActorProviderConfigured): Unit = {
  }

  override def postStop {
    DEBUG("Actor[%s] stopping, saving state", self.uuid)
    saveUserState
  }

  override def preRestart(reason: Throwable) {
    ERROR(reason, "preRestart: Actor[%s]", self.uuid)
  }

  override def postRestart(reason: Throwable) {
    ERROR(reason, "postRestart: Actor[%s]", self.uuid)
  }

  private[this] def DEBUG(fmt: String, args: Any*) =
    logger.debug("UserActor[%s]: %s".format(_userId, fmt.format(args: _*)))

  private[this] def INFO(fmt: String, args: Any*) =
    logger.info("UserActor[%s]: %s".format(_userId, fmt.format(args: _*)))

  private[this] def WARN(fmt: String, args: Any*) =
    logger.warn("UserActor[%s]: %s".format(_userId, fmt.format(args: _*)))

  private[this] def ERROR(fmt: String, args: Any*) =
    logger.error("UserActor[%s]: %s".format(_userId, fmt.format(args: _*)))

  private[this] def ERROR(t: Throwable, fmt: String, args: Any*) =
      logger.error("UserActor[%s]: %s".format(_userId, fmt.format(args: _*)), t)
}
