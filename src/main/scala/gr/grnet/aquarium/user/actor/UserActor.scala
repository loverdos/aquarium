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
import com.ckkloverdos.maybe.{Failed, NoVal, Just, Maybe}
import gr.grnet.aquarium.logic.accounting.{AccountingException, Policy, Accounting}
import gr.grnet.aquarium.user._
import gr.grnet.aquarium.logic.events.{UserEvent, WalletEntry, ResourceEvent}
import gr.grnet.aquarium.logic.accounting.dsl.{DSLPolicy, DSLResource, DSLSimpleResource, DSLComplexResource}
import java.util.Date
import gr.grnet.aquarium.util.{DateUtils, TimeHelpers, Loggable}


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

  private[this] def processResourceEvent(resourceEvent: ResourceEvent, checkForOlderEvents: Boolean): Unit = {
    if(checkForOlderEvents) {
      DEBUG("Checking for events older than %s" format resourceEvent)
      processOlderResourceEvents(resourceEvent)
    }

    justProcessTheResourceEvent(resourceEvent, "ACTUAL")
  }


  /**
   * Given an "onoff" event, we try to locate all unprocessed resource events that precede this one.
   */
  def findOlderResourceEventsForOnOff(rcEvent: ResourceEvent, policy: DSLPolicy): List[ResourceEvent] = {
    Nil
  }

  def findOlderResourceEventsForOther(rcEvent: ResourceEvent, policy: DSLPolicy): List[ResourceEvent] = {
    Nil
  }

  /**
   * Find resource events that precede the given one and are unprocessed.
   */
  private[this] def findOlderResourceEvents(rcEvent: ResourceEvent, policy: DSLPolicy): List[ResourceEvent] = {
    if(rcEvent.isOnOffEvent(policy)) {
      findOlderResourceEventsForOnOff(rcEvent, policy)
    } else {
      findOlderResourceEventsForOther(rcEvent, policy)
    }
  }

  /**
   * Find and process older resource events.
   *
   * Older resource events are found based on the latest credit calculation, that is the latest
   * wallet entry. If there are resource events past that wallet entry, then we deduce that no wallet entries
   * have been calculated for these resource events and start from there.
   */
  private[this] def processOlderResourceEvents(resourceEvent: ResourceEvent): Unit = {
    assert(_userId == resourceEvent.userId)
    val rceId = resourceEvent.id
    val userId = resourceEvent.userId
    val resourceEventStore = _configurator.resourceEventStore
    val walletEntriesStore = _configurator.walletStore

    // 1. Find latest wallet entry
    val latestWalletEntriesM = walletEntriesStore.findLatestUserWalletEntries(userId)
    latestWalletEntriesM match {
      case Just(latestWalletEntries) ⇒
        // The time on which we base the selection of the older events
        val selectionTime = latestWalletEntries.head.occurredMillis

        // 2. Now find all resource events past the time of the latest wallet entry.
        //    These events have not been processed, except probably those ones
        //    that have the same `occurredMillis` with `selectionTime`
        val oldRCEvents = resourceEventStore.findResourceEventsByUserIdAfterTimestamp(userId, selectionTime)

        // 3. Filter out those resource events for which no wallet entry has actually been
        //    produced.
        val rcEventsToProcess = for {
          oldRCEvent        <- oldRCEvents
          oldRCEventId      = oldRCEvent.id
          latestWalletEntry <- latestWalletEntries if(!latestWalletEntry.fromResourceEvent(oldRCEventId) && rceId != oldRCEventId)
        } yield {
          oldRCEvent
        }

        DEBUG("Found %s events older than %s".format(rcEventsToProcess.size, resourceEvent))

        for {
          rcEventToProcess <- rcEventsToProcess
        } {
          justProcessTheResourceEvent(rcEventToProcess, "OLDER")
        }
      case NoVal ⇒
        DEBUG("No events to process older than %s".format(resourceEvent))
      case Failed(e, m) ⇒
        ERROR("[%s][%s] %s".format(e.getClass.getName, m, e.getMessage))
    }
  }

  private[this] def _storeWalletEntries(walletEntries: List[WalletEntry], allowNonFinalized: Boolean): Unit = {
    val walletEntriesStore = _configurator.walletStore
    for(walletEntry <- walletEntries) {
      val allow = walletEntry.finalized || allowNonFinalized
      if(allow) {
        walletEntriesStore.storeWalletEntry(walletEntry)
      }
    }
  }

  private[this] def _calcNewCreditSum(walletEntries: List[WalletEntry]): Double = {
    val newCredits = for {
      walletEntry <- walletEntries if(walletEntry.finalized)
    } yield walletEntry.value.toDouble

    newCredits.sum
  }

  /**
   * Process the resource event as if nothing else matters. Just do it.
   */
  private[this] def justProcessTheResourceEvent(ev: ResourceEvent, logLabel: String): Unit = {
    val start = System.currentTimeMillis
    DEBUG("Processing [%s] %s".format(logLabel, ev))

    // Initially, the user state (regarding resources) is empty.
    // So we have to compensate for both a totally empty resource state
    // and the update with new values.

    // 1. Find the resource definition
    val policy = Policy.policy
    policy.findResource(ev.resource) match {
      case Some(resource) ⇒
        // 2. Get the instance id and value for the resource
        val instanceIdM = resource match {
          // 2.1 If it is complex, from the details map, get the field which holds the instanceId
          case DSLComplexResource(name, unit, costPolicy, descriminatorField) ⇒
            ev.details.get(descriminatorField) match {
              case Some(instanceId) ⇒
                Just(instanceId)
              case None ⇒
                // We should have some value under this key here....
                Failed(throw new AccountingException("")) //TODO: See what to do here
            }
          // 2.2 If it is simple, ...
          case DSLSimpleResource(name, unit, costPolicy) ⇒
            // ... by convention, the instanceId of a simple resource is just "1"
            // @see [[gr.grnet.aquarium.user.OwnedResourcesSnapshot]]
            Just("1")
        }

        // 3. Did we get a valid instanceId?
        instanceIdM match {
          // 3.1 Yes, time to get/update the current state
          case Just(instanceId) ⇒
            val oldOwnedResources = _userState.ownedResources

            // A. First state diff: the modified resource value
            val StateChangeMillis = TimeHelpers.nowMillis
            val (newOwnedResources, oldRCInstanceOpt, newRCInstance) = oldOwnedResources.
              addOrUpdateResourceSnapshot(resource.name, instanceId, ev.value, ev.occurredMillis)
            val previousRCUpdateTime = oldRCInstanceOpt.map(_.snapshotTime).getOrElse(newRCInstance.snapshotTime)

            // Calculate the wallet entries generated from this resource event
            _userState.maybeDSLAgreement match {
              case Just(agreement) ⇒
                val walletEntriesM = chargeEvent(ev, agreement, ev.value,
                  new Date(previousRCUpdateTime),
                  findRelatedEntries(resource, ev.getInstanceId(policy)))
                walletEntriesM match {
                  case Just(walletEntries) ⇒
                    _storeWalletEntries(walletEntries, true)

                    // B. Second state diff: the new credits
                    val newCreditsSum = _calcNewCreditSum(walletEntries)
                    val oldCredits    = _userState.safeCredits.data
                    val newCredits = CreditSnapshot(oldCredits + newCreditsSum, StateChangeMillis)

                    // Finally, the userState change
                    DEBUG("Credits   = %s".format(this._userId, newCredits))
                    DEBUG("Resources = %s".format(this._userId, newOwnedResources))
                    this._userState = this._userState.copy(
                      credits = newCredits,
                      ownedResources = newOwnedResources
                    )
                  case NoVal ⇒
                    DEBUG("No wallet entries generated for %s".format(ev))
                  case failed @ Failed(e, m) ⇒
                    failed
                }
                
              case NoVal ⇒
                Failed(new UserDataSnapshotException("No agreement snapshot found for user %s".format(this._userId)))
              case failed @ Failed(e, m) ⇒
                failed
            }
          // 3.2 No, no luck, this is an error
          case NoVal ⇒
            Failed(new UserDataSnapshotException("No instanceId for resource %s of user %s".format(resource, this._userId)))
          case failed @ Failed(e, m) ⇒
            failed
        }
      // No resource definition found, this is an error
      case None ⇒ // Policy.policy.findResource(ev.resource)
        Failed(new UserDataSnapshotException("No resource %s found for user %s".format(ev.resource, this._userId)))
    }

    DEBUG("Finished %s time: %d ms".format(ev.id, System.currentTimeMillis - start))
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
        val agreementOpt = Policy.policy.findAgreement("default")
        
        if(agreementOpt.isEmpty) {
          ERROR("No default agreement found. Cannot initialize user state")
        } else {
          this._userState = UserState(
            userId,
            0,
            ActiveSuspendedSnapshot(event.isStateActive, now),
            CreditSnapshot(0, now),
            AgreementSnapshot(agreementOpt.get.name, now),
            RolesSnapshot(event.roles, now),
            PaymentOrdersSnapshot(Nil, now),
            OwnedGroupsSnapshot(Nil, now),
            GroupMembershipsSnapshot(Nil, now),
            OwnedResourcesSnapshot(List(), now)
          )

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
   * Try to load from the DB the latest known info (snapshot data) for the given user.
   */
  private[this] def findUserState(userId: String): Maybe[UserState] = {
    val usersDB = _configurator.storeProvider.userStateStore
    usersDB.findUserStateByUserId(userId)
  }

  /**
   * Tries to makes sure that the internal user state exists.
   *
   * May contact the [[gr.grnet.aquarium.store.UserStateStore]] for that.
   *
   */
  private[this] def ensureUserState(): Unit = {
    /*if(null eq this._userState) {
      findUserState(this._userId) match {
        case Just(userState) ⇒
          DEBUG("Loaded user state %s from DB", userState)
          //TODO: May be out of sync with the event store, rebuild it here
          this._userState = userState
          rebuildState(this._userState.oldestSnapshotTime)
        case Failed(e, m) ⇒
          ERROR("While loading user state from DB: [%s][%s] %s", e.getClass.getName, e.getMessage, m)
        case NoVal ⇒
          //TODO: Rebuild actor state here.
          rebuildState(0)
          WARN("Request for unknown (to Aquarium) user")
      }
    }*/

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
    val now = System.currentTimeMillis()
    val agreement = Policy.policy.findAgreement("default")

    this._userState = UserState(
      _userId,
      0,
      ActiveSuspendedSnapshot(false, now),
      CreditSnapshot(0, now),
      AgreementSnapshot(agreement.get.name, now),
      RolesSnapshot(List(), now),
      PaymentOrdersSnapshot(Nil, now),
      OwnedGroupsSnapshot(Nil, now),
      GroupMembershipsSnapshot(Nil, now),
      OwnedResourcesSnapshot(List(), now)
    )
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

        var curValue = 0F
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
      /*if(this._userId != userId) {
        ERROR("Received %s but my userId = %s".format(m, this._userId))
        // TODO: throw an exception here
      } else {
        // This is the big party.
        // Get the user state, if it exists and make sure it is not stale.
        ensureUserState()

        // Do we have a user state?
        if(_userState ne null) {
          // Yep, we do. See what there is inside it.
          val credits = _userState.credits
          val creditsTimestamp = credits.snapshotTime

          // Check if data is stale
          if(creditsTimestamp + _timestampTheshold > timestamp) {
            // No, it's OK
            self reply UserResponseGetBalance(userId, credits.data)
          } else {
            // Yep, data is stale and must recompute balance
            // FIXME: implement
            ERROR("FIXME: Should have computed a new value for %s".format(credits))
            self reply UserResponseGetBalance(userId, credits.data)
          }
        } else {
          // No user state exists. This is an error.
          val errMsg = "Could not load user state for %s".format(m)
          ERROR(errMsg)
          self reply ResponseUserBalance(userId, 0, Some(errMsg))
        }*/
        if (System.currentTimeMillis() - _userState.newestSnapshotTime > 60 * 1000)
          calcWalletEntries()
        self reply UserResponseGetBalance(userId, _userState.credits.data)
      //}

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