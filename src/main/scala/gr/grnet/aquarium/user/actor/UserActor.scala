/*
 * Copyright 2011 GRNET S.A. All rights reserved.
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

package gr.grnet.aquarium.user.actor

import gr.grnet.aquarium.actor._
import gr.grnet.aquarium.Configurator
import gr.grnet.aquarium.processor.actor._
import gr.grnet.aquarium.logic.accounting.{AccountingException, Policy, Accounting}
import gr.grnet.aquarium.user._
import gr.grnet.aquarium.logic.events.{UserEvent, WalletEntry, ResourceEvent}
import java.util.Date
import gr.grnet.aquarium.util.{DateUtils, Loggable}
import gr.grnet.aquarium.logic.accounting.dsl.{DSLAgreement, DSLResource, DSLComplexResource}
import gr.grnet.aquarium.util.date.TimeHelpers
import com.ckkloverdos.maybe.{Maybe, Failed, NoVal, Just}


/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class UserActor extends AquariumActor
                   with ReflectiveAquariumActor
                   with Loggable {
  @volatile
  private[this] var _userId: String = _
  @volatile
  private[this] var _userState: UserState = _
  @volatile
  private[this] var _timestampTheshold: Long = _

  def role = UserActorRole

  private[this] def _configurator: Configurator = Configurator.MasterConfigurator

  /**
   * Replay the event log for all events that affect the user state.
   */
  def rebuildState(from: Long, to: Long): Unit = {
    val start = System.currentTimeMillis()
    if (_userState == null)
      createBlankState

    //Rebuild state from user events
    val usersDB = _configurator.storeProvider.userEventStore
    val userEvents = usersDB.findUserEventsByUserId(_userId)
    val numUserEvents = userEvents.size
    _userState = replayUserEvents(_userState, userEvents, from, to)

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
      System.currentTimeMillis() - start))
  }

  /**
   * Create an empty state for a user
   */
  def createBlankState = {
    this._userState = DefaultUserStateComputations.createFirstUserState(this._userId)
  }

  /**
   * Replay user events on the provided user state
   */
  def replayUserEvents(initState: UserState, events: List[UserEvent],
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
    _configurator.storeProvider.userStateStore.deleteUserState(this._userId)
    _configurator.storeProvider.userStateStore.storeUserState(this._userState) match {
      case Just(record) => record
      case NoVal => ERROR("Unknown error saving state")
      case Failed(e, a) =>
        ERROR("Saving state failed: %s error was: %s".format(a,e));
    }
  }

  def onAquariumPropertiesLoaded(event: AquariumPropertiesLoaded): Unit = {
    this._timestampTheshold = event.props.getLong(Configurator.Keys.user_state_timestamp_threshold).getOr(10000)
    INFO("Setup my timestampTheshold = %s", this._timestampTheshold)
  }

  def onUserActorInitWithUserId(event: UserActorInitWithUserId): Unit = {
    this._userId = event.userId
    DEBUG("Actor starting, loading state")
  }

  def onProcessResourceEvent(event: ProcessResourceEvent): Unit = {
    val resourceEvent = event.rce
    if(resourceEvent.userId != this._userId) {
      ERROR("Received %s but my userId = %s".format(event, this._userId))
    } else {
      //ensureUserState()
//        calcWalletEntries()
      //processResourceEvent(resourceEvent, true)
    }
  }

  private[this] def processCreateUser(event: UserEvent): Unit = {
    val userId = event.userId
    DEBUG("Creating user from state %s", event)
    val usersDB = _configurator.storeProvider.userStateStore
    usersDB.findUserStateByUserId(userId) match {
      case Just(userState) ⇒
        WARN("User already created, state = %s".format(userState))
      case failed @ Failed(e, m) ⇒
        ERROR("[%s][%s] %s", e.getClass.getName, e.getMessage, m)
      case NoVal ⇒
        // OK. Create a default UserState and store it
        val now = TimeHelpers.nowMillis
        val agreementOpt = Policy.policy.findAgreement(DSLAgreement.DefaultAgreementName)

        if(agreementOpt.isEmpty) {
          ERROR("No default agreement found. Cannot initialize user state")
        } else {
          this._userState = DefaultUserStateComputations.createFirstUserState(
            userId,
            event.occurredMillis,
            DSLAgreement.DefaultAgreementName)
          saveUserState
          DEBUG("Created and stored %s", this._userState)
        }
    }
  }

  private[this] def processModifyUser(event: UserEvent): Unit = {
    val now = TimeHelpers.nowMillis
    val newActive = ActiveStateSnapshot(event.isStateActive, now)

    DEBUG("New active status = %s".format(newActive))

    this._userState = this._userState.copy( activeStateSnapshot = newActive )
  }

  def onProcessUserEvent(event: ProcessUserEvent): Unit = {
    val userEvent = event.ue
    if(userEvent.userId != this._userId) {
      ERROR("Received %s but my userId = %s".format(userEvent, this._userId))
    } else {
//      ensureUserState()
      if(userEvent.isCreateUser) {
        processCreateUser(userEvent)
      } else if(userEvent.isModifyUser) {
        processModifyUser(userEvent)
      }
    }
  }

  def onRequestUserBalance(event: RequestUserBalance): Unit = {
    val userId = event.userId
    val timestamp = event.timestamp

    if (System.currentTimeMillis() - _userState.newestSnapshotTime > 60 * 1000)
    {
//        calcWalletEntries()
    }
    self reply UserResponseGetBalance(userId, _userState.creditsSnapshot.creditAmount)
  }

  def onUserRequestGetState(event: UserRequestGetState): Unit = {
    val userId = event.userId
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

  override def postStop {
    DEBUG("Stopping, saving state")
    //saveUserState
  }

  def knownMessageTypes = UserActor.KnownMessageTypes

  private[this] def DEBUG(fmt: String, args: Any*) =
    logger.debug("UserActor[%s]: %s".format(_userId, fmt.format(args:_*)))

  private[this] def INFO(fmt: String, args: Any*) =
      logger.info("UserActor[%s]: %s".format(_userId, fmt.format(args:_*)))

  private[this] def WARN(fmt: String, args: Any*) =
    logger.warn("UserActor[%s]: %s".format(_userId, fmt.format(args:_*)))

  private[this] def ERROR(fmt: String, args: Any*) =
    logger.error("UserActor[%s]: %s".format(_userId, fmt.format(args:_*)))
}

object UserActor {
  final val KnownMessageTypes = List(
    classOf[AquariumPropertiesLoaded],
    classOf[UserActorInitWithUserId],
    classOf[ProcessResourceEvent],
    classOf[ProcessUserEvent],
    classOf[RequestUserBalance],
    classOf[UserRequestGetState]
  )
}