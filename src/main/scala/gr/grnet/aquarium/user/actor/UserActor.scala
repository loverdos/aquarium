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
import com.ckkloverdos.maybe.{Failed, NoVal, Just}
import gr.grnet.aquarium.logic.accounting.{AccountingException, Policy, Accounting}
import gr.grnet.aquarium.user._
import gr.grnet.aquarium.logic.events.{UserEvent, WalletEntry, ResourceEvent}
import java.util.Date
import gr.grnet.aquarium.util.{DateUtils, Loggable}
import gr.grnet.aquarium.logic.accounting.dsl.{DSLAgreement, DSLResource, DSLComplexResource}
import gr.grnet.aquarium.util.date.TimeHelpers


/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class UserActor extends AquariumActor
  with Loggable with Accounting with DateUtils {
  @volatile
  private[this] var _userId: String = _
  @volatile
  private[this] var _userState: UserState = _
  @volatile
  private[this] var _timestampTheshold: Long = _

  def role = UserActorRole

  private[this] def _configurator: Configurator = Configurator.MasterConfigurator

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
          this._userState = UserStateComputations.createFirstUserState(userId, DSLAgreement.DefaultAgreementName)
          saveUserState
          DEBUG("Created and stored %s", this._userState)
        }
    }
  }

  private[this] def findRelatedEntries(res: DSLResource, instid: String): List[WalletEntry] = {
    val walletDB = _configurator.storeProvider.walletEntryStore
    walletDB.findPreviousEntry(_userId, res.name, instid, Some(false))
  }


  private[this] def processModifyUser(event: UserEvent): Unit = {
    val now = TimeHelpers.nowMillis
    val newActive = ActiveSuspendedSnapshot(event.isStateActive, now)

    DEBUG("New active status = %s".format(newActive))

    this._userState = this._userState.copy( active = newActive )
  }
  /**
   * Use the provided [[gr.grnet.aquarium.logic.events.UserEvent]] to change any user state.
   */
  private[this] def processUserEvent(event: UserEvent): Unit = {
    if(event.isCreateUser) {
      processCreateUser(event)
    } else if(event.isModifyUser) {
      processModifyUser(event)
    }
  }

  /**
   * Tries to makes sure that the internal user state exists.
   *
   * May contact the [[gr.grnet.aquarium.store.UserStateStore]] for that.
   *
   */
  private[this] def ensureUserState(): Unit = {
    if (_userState == null)
      rebuildState(0)
    else
      rebuildState(_userState.oldestSnapshotTime, System.currentTimeMillis())
  }

  /**
   * Replay the event log for all events that affect the user state, starting
   * from the provided time instant.
   */
  def rebuildState(from: Long): Unit =
    rebuildState(from, oneYearAhead(new Date(), new Date(Long.MaxValue)).getTime)

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
    _userState = replayResourceEvents(_userState, resourceEvents, from, to)

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
    this._userState = UserStateComputations.createFirstUserState(this._userId, DSLAgreement.DefaultAgreementName)
  }

  /**
   * Replay user events on the provided user state
   */
  def replayUserEvents(initState: UserState, events: List[UserEvent],
                       from: Long, to: Long): UserState = {
    var act = initState.active
    var rol = initState.roles
    events
      .filter(e => e.occurredMillis >= from && e.occurredMillis < to)
      .foreach {
        e =>
          act = act.copy(
            data = e.isStateActive, snapshotTime = e.occurredMillis)
          // TODO: Implement the following
          //_userState.agreement = _userState.agreement.copy(
          //  data = e.newAgreement, e.occurredMillis)

          rol = rol.copy(data = e.roles,
            snapshotTime = e.occurredMillis)
    }
    initState.copy(active = act, roles = rol)
  }

  /**
   * Replay resource events on the provided user state
   */
  def replayResourceEvents(initState: UserState, events: List[ResourceEvent],
                           from: Long, to: Long): UserState = {
    var res = initState.ownedResources
    events
      .filter(e => e.occurredMillis >= from && e.occurredMillis < to)
      .foreach {
        e =>
          val name = Policy.policy.findResource(e.resource) match {
            case Some(x) => x.name
            case None => ""
          }

          val instanceId = e.getInstanceId(Policy.policy)
          res = res.addOrUpdateResourceSnapshot(name,
            instanceId, e.value, e.occurredMillis)._1
    }
    if (!events.isEmpty) {
      val snapTime = events.map{e => e.occurredMillis}.max
      res = res.copy(snapshotTime = snapTime)
    }
    initState.copy(ownedResources = res)
  }

  /**
   * Replay wallet entries on the provided user state
   */
  def replayWalletEntries(initState: UserState, events: List[WalletEntry],
                          from: Long, to: Long): UserState = {
    var cred = initState.credits
    events
      .filter(e => e.occurredMillis >= from && e.occurredMillis < to)
      .foreach {
        w =>
          val newVal = cred.data + w.value
          cred = cred.copy(data = newVal)
    }
    if (!events.isEmpty) {
      val snapTime = events.map{e => e.occurredMillis}.max
      cred = cred.copy(snapshotTime = snapTime)
    }
    initState.copy(credits = cred)
  }

  /**
   * Update wallet entries for all unprocessed events
   */
  def calcWalletEntries(): Unit = {
    ensureUserState

    if (_userState.ownedResources.snapshotTime < _userState.credits.snapshotTime) return
    val eventsDB = _configurator.storeProvider.resourceEventStore
    val resourceEvents = eventsDB.findResourceEventsByUserIdAfterTimestamp(_userId, _userState.credits.snapshotTime)
    val policy = Policy.policy

    val walletEntries = resourceEvents.map {
      ev =>
        // TODO: Check that agreement exists
        val agr = policy.findAgreement(_userState.agreement.data).get

        val resource = policy.findResource(ev.resource) match {
          case Some(x) => x
          case None =>
            val errMsg = "Cannot find resource: %s".format(ev.resource)
            ERROR(errMsg)
            throw new AccountingException(errMsg) // FIXME: to throw or not to throw?
        }

        val instid = resource.isComplex match {
          case true => ev.details.get(resource.asInstanceOf[DSLComplexResource].descriminatorField)
          case false => None
        }

        var curValue: Double = 0.0
        var lastUpdate = _userState.ownedResources.findResourceSnapshot(ev.resource, ev.getInstanceId(policy)) match {
          case Some(x) => x.snapshotTime
          case None => Long.MaxValue //To trigger recalculation
        }

        if (lastUpdate > ev.occurredMillis) {
          //Event is older that current state. Rebuild state up to event timestamp
          val resHistory =
            ResourceEvent("", 0, 0, _userId, "1", ev.resource, ev.eventVersion, 0, ev.details) ::
            eventsDB.findResourceEventHistory(_userId, ev.resource, instid, ev.occurredMillis)
          INFO("%d older entries for resource %s, user %s up to %d".format(resHistory.size, ev.resource, _userId, ev.occurredMillis));
          var res = OwnedResourcesSnapshot(List(), 0)
          resHistory.foreach {
            e =>
              res = res.addOrUpdateResourceSnapshot(e.resource, e.getInstanceId(policy), e.value, e.occurredMillis)._1
          }
          lastUpdate = res.findResourceSnapshot(ev.resource, ev.getInstanceId(policy)).get.snapshotTime
          curValue = res.findResourceSnapshot(ev.resource, ev.getInstanceId(policy)).get.data
        } else {
          curValue = _userState.ownedResources.findResourceSnapshot(ev.resource, ev.getInstanceId(policy)).get.data
        }

        val entries = chargeEvent(ev, agr, curValue, new Date(lastUpdate),
          findRelatedEntries(resource, ev.getInstanceId(policy)))
        INFO("PERF: CHARGE %s %d".format(ev.id, System.currentTimeMillis))
        entries match {
          case Just(x) => x
          case Failed(e, r) => List()
          case NoVal => List()
        }
    }.flatten

    val walletDB = _configurator.storeProvider.walletEntryStore
    walletEntries.foreach(w => walletDB.storeWalletEntry(w))

    ensureUserState
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

  protected def receive: Receive = {
    case m @ AquariumPropertiesLoaded(props) ⇒
      this._timestampTheshold = props.getLong(Configurator.Keys.user_state_timestamp_threshold).getOr(10000)
      INFO("Setup my timestampTheshold = %s", this._timestampTheshold)

    case m @ UserActorInitWithUserId(userId) ⇒
      this._userId = userId
      DEBUG("Actor starting, loading state")
      ensureUserState()

    case m @ ProcessResourceEvent(resourceEvent) ⇒
      if(resourceEvent.userId != this._userId) {
        ERROR("Received %s but my userId = %s".format(m, this._userId))
      } else {
        //ensureUserState()
        calcWalletEntries()
        //processResourceEvent(resourceEvent, true)
      }

    case m @ ProcessUserEvent(userEvent) ⇒
      if(userEvent.userId != this._userId) {
        ERROR("Received %s but my userId = %s".format(m, this._userId))
      } else {
        ensureUserState()
        processUserEvent(userEvent)
      }

    case m @ RequestUserBalance(userId, timestamp) ⇒
      if (System.currentTimeMillis() - _userState.newestSnapshotTime > 60 * 1000)
        calcWalletEntries()
      self reply UserResponseGetBalance(userId, _userState.credits.data)

    case m @ UserRequestGetState(userId, timestamp) ⇒
      if(this._userId != userId) {
        ERROR("Received %s but my userId = %s".format(m, this._userId))
        // TODO: throw an exception here
      } else {
        // FIXME: implement
        ERROR("FIXME: Should have properly computed the user state")
        ensureUserState()
        self reply UserResponseGetState(userId, this._userState)
      }
  }

  override def postStop {
    DEBUG("Stopping, saving state")
    //saveUserState
  }

  private[this] def DEBUG(fmt: String, args: Any*) =
    logger.debug("UserActor[%s]: %s".format(_userId, fmt.format(args:_*)))

  private[this] def INFO(fmt: String, args: Any*) =
      logger.info("UserActor[%s]: %s".format(_userId, fmt.format(args:_*)))

  private[this] def WARN(fmt: String, args: Any*) =
    logger.warn("UserActor[%s]: %s".format(_userId, fmt.format(args:_*)))

  private[this] def ERROR(fmt: String, args: Any*) =
    logger.error("UserActor[%s]: %s".format(_userId, fmt.format(args:_*)))
}