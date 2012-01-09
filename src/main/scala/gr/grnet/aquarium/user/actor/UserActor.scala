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
import java.util.Date
import gr.grnet.aquarium.processor.actor._
import com.ckkloverdos.maybe.{Failed, NoVal, Just, Maybe}
import gr.grnet.aquarium.logic.accounting.{AccountingException, Policy, Accounting}
import gr.grnet.aquarium.logic.accounting.dsl.{DSLSimpleResource, DSLComplexResource}
import gr.grnet.aquarium.util.{TimeHelpers, Loggable}
import gr.grnet.aquarium.logic.events.{UserEvent, WalletEntry, ResourceEvent}
import gr.grnet.aquarium.user._


/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class UserActor extends AquariumActor with Loggable with Accounting {
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
      logger.debug("Checking for events older than %s" format resourceEvent)
      processOlderResourceEvents(resourceEvent)
    }

    justProcessTheResourceEvent(resourceEvent, "ACTUAL")
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

  private[this] def _storeWalletEntries(walletEntries: List[WalletEntry]): Unit = {
    val walletEntriesStore = _configurator.walletStore
    for(walletEntry <- walletEntries) {
      walletEntriesStore.storeWalletEntry(walletEntry)
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
    Policy.policy.findResource(ev.resource) match {
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
            // Find or create the new resource instance map
            val oldOwnedResourcesData = oldOwnedResources.data
            val oldRCInstanceMap = oldOwnedResourcesData.get(resource) match {
              case Some(resourceMap) ⇒ resourceMap
              case None              ⇒ Map[String, Float]()
            }
            // Update the new value in the resource instance map
            val newRCInstanceMap: Map[String, Float] = oldRCInstanceMap.updated(instanceId, ev.value)

            val newOwnedResourcesData = oldOwnedResourcesData.updated(resource, newRCInstanceMap)

            // A. First state diff: the modified resource value
            val StateChangeMillis = TimeHelpers.nowMillis
            val newOwnedResources = oldOwnedResources.copy(
              data = newOwnedResourcesData,
              snapshotTime = StateChangeMillis
            )

            // Calculate the wallet entries generated from this resource event
            _userState.maybeDSLAgreement match {
              case Just(agreement) ⇒
                // TODO: the snapshot time should be per instanceId?
                // TODO: Related events
                val walletEntriesM = chargeEvent(ev, agreement, ev.value,
                  new Date(oldOwnedResources.snapshotTime), List())
                walletEntriesM match {
                  case Just(walletEntries) ⇒
                    _storeWalletEntries(walletEntries)

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

    DEBUG("Finished %s time: %d ms".format(logLabel, ev, System.currentTimeMillis - start))
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
            ActiveSuspendedSnapshot(event.isStateActive, now),
            CreditSnapshot(0, now),
            AgreementSnapshot(agreementOpt.get, now),
            RolesSnapshot(event.roles, now),
            PaymentOrdersSnapshot(Nil, now),
            OwnedGroupsSnapshot(Nil, now),
            GroupMembershipsSnapshot(Nil, now),
            OwnedResourcesSnapshot(Map(), now)
          )

          usersDB.storeUserState(this._userState)
          DEBUG("Created and stored %s", this._userState)
        }
    }
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
      usersDB.findUserStateByUserId(userId) match {
        case userStateJ @ Just(userState) ⇒
          userStateJ
        case e ⇒ e
      }
  }

  protected def receive: Receive = {
    case UserActorStop ⇒
      self.stop()

    case m @ AquariumPropertiesLoaded(props) ⇒
      this._timestampTheshold = props.getLong(Configurator.Keys.user_state_timestamp_threshold).getOr(10000)
      logger.info("Setup my timestampTheshold = %s".format(this._timestampTheshold))

    case m @ UserActorInitWithUserId(userId) ⇒
      this._userId = userId
      findUserState(userId) match {
        case Just(userState) ⇒
          DEBUG("Loaded user state %s from DB", userState)
          this._userState = userState
        case Failed(e, m) ⇒
          ERROR("While loading user state from DB: [%s][%s] %s", e.getClass.getName, e.getMessage, m)
        case NoVal ⇒
          WARN("Request for unknown (to Aquarium) user")
      }

    case m @ ProcessResourceEvent(resourceEvent) ⇒
      processResourceEvent(resourceEvent, true)

    case m @ ProcessUserEvent(userEvent) ⇒
      processUserEvent(userEvent)

    case m @ UserRequestGetBalance(userId, timestamp) ⇒
      if(this._userId != userId) {
        ERROR("Received %s but my userId = %s".format(m, this._userId))
        // TODO: throw an exception here
      } else {
        // This is the big party.
        // Get the user state, if it exists and make sure it is not stale.

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
          // Nope. No user state exists. Must reproduce one
          // FIXME: implement
          ERROR("FIXME: Should have computed the user state for userId = %s".format(userId))
          self reply UserResponseGetBalance(userId, 0)
        }
      }

    case m @ UserRequestGetState(userId, timestamp) ⇒
      if(this._userId != userId) {
        ERROR("Received %s but my userId = %s".format(m, this._userId))
        // TODO: throw an exception here
      } else {
        // FIXME: implement
        ERROR("FIXME: Should have properly computed the user state")
        self reply UserResponseGetState(userId, this._userState)
      }
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