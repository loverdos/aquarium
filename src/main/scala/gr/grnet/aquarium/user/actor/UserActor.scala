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

import gr.grnet.aquarium.util.Loggable
import gr.grnet.aquarium.actor._
import gr.grnet.aquarium.Configurator
import gr.grnet.aquarium.processor.actor.{UserResponseGetState, UserRequestGetState, UserResponseGetBalance, UserRequestGetBalance}
import gr.grnet.aquarium.logic.accounting.dsl.DSLResource
import java.util.Date
import gr.grnet.aquarium.processor.actor._
import gr.grnet.aquarium.logic.accounting.Accounting
import com.ckkloverdos.maybe.{Failed, NoVal, Just, Maybe}
import gr.grnet.aquarium.logic.events.{WalletEntry, ResourceEvent}
import gr.grnet.aquarium.user.{OwnedResourcesSnapshot, UserState}


/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class UserActor extends AquariumActor with Loggable {
  @volatile
  private[this] var _userId: String = _
  @volatile
  private[this] var _isInitialized: Boolean = false
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

        logger.debug("Found %s events older than %s".format(rcEventsToProcess.size, resourceEvent))

        for {
          rcEventToProcess <- rcEventsToProcess
        } {
          justProcessTheResourceEvent(rcEventToProcess, "OLDER")
        }
      case NoVal ⇒
        logger.debug("No events to process older than %s".format(resourceEvent))
      case Failed(e, m) ⇒
        logger.error("[%s][%s] %s".format(e.getClass.getName, m, e.getMessage))
    }
  }

  private[this] def _calcNewUserState(resourceEvent: ResourceEvent, walletEntries: List[WalletEntry]): Unit = {
    val walletEntriesStore = _configurator.walletStore
    // 1. Store the new wallet entries
    for(walletEntry <- walletEntries) {
      walletEntriesStore.storeWalletEntry(walletEntry)
    }
    // 2. Update user state
    val newUserState = resourceEvent.calcStateChange(walletEntries, _userState)
    if(_userState == newUserState) {
      logger.debug("No state change for %s".format(_userState))
    } else {
      logger.debug("State change from %s".format(_userState))
      logger.debug("State change   to %s".format(newUserState))
      _userState = newUserState
    }
  }

  /**
   * Process the resource event as if nothing else matters. Just do it.
   */
  private[this] def justProcessTheResourceEvent(resourceEvent: ResourceEvent, logLabel: String): Unit = {
    logger.debug("Processing [%s] %s".format(logLabel, resourceEvent))

    if(resourceEvent.resourceType.isIndependentType) {
      // There is a one-to-one correspondence from an event to credit diff generation (wallet entry)

      // TODO: find some other way to use the services of Accounting.
      val accounting = new Accounting {}
      val walletEntriesM = accounting chargeEvent resourceEvent

      walletEntriesM match {
        case Just(walletEntries) ⇒
          _calcNewUserState(resourceEvent, walletEntries)
        case NoVal ⇒
          logger.debug("No wallet entries generated for %s".format(resourceEvent))
          _calcNewUserState(resourceEvent, Nil)
        case f @ Failed(e, m) ⇒
          logger.error("[%s][%s] %s".format(e.getClass.getName, m, e.getMessage))
          // TODO: What else to do on error?
      }
    } else {
      // We need more than one resource event to calculate credit diffs.
      // FIXME: implement
      logger.error("Not processing %s".format(resourceEvent))
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
      this._isInitialized = true
      // TODO: query DB etc to get internal state
      logger.info("Setup my userId = %s".format(userId))

//    case m @ ActorProviderConfigured(actorProvider) ⇒
//      this._actorProvider = actorProvider
//      logger.info("Configured %s with %s".format(this, m))

    case m @ ProcessResourceEvent(resourceEvent) ⇒
      processResourceEvent(resourceEvent, true)

    case m @ UserRequestGetBalance(userId, timestamp) ⇒
      if(this._userId != userId) {
        logger.error("Received %s but my userId = %s".format(m, this._userId))
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
            logger.error("FIXME: Should have computed a new value for %s".format(credits))
            self reply UserResponseGetBalance(userId, credits.data)
          }
        } else {
          // Nope. No user state exists. Must reproduce one
          // FIXME: implement
          logger.error("FIXME: Should have computed the user state for userId = %s".format(userId))
          self reply UserResponseGetBalance(userId, Maybe(userId.toDouble).getOr(10.5))
        }
      }

    case m @ UserRequestGetState(userId, timestamp) ⇒
      if(this._userId != userId) {
        logger.error("Received %s but my userId = %s".format(m, this._userId))
        // TODO: throw an exception here
      } else {
        // FIXME: implement
        logger.error("FIXME: Should have properly computed the user state")
        self reply UserResponseGetState(userId, this._userState)
      }

    /* Resource API */
    case m @ UserActorResourceStateRequest(resource, instance) ⇒
      resourceState(resource, instance)

    case m @ UserActorResourceStateUpdate(resource, instanceid, value) ⇒
      resourceStateUpdate(resource, instanceid, value)

    case m @ UserActorResourceCreateInstance(resource, instanceid) ⇒
      createResourceInstance(resource, instanceid)

    case m @ UserActorResourceDeleteInstance(resource, instanceid) ⇒
      deleteResourceInstance(resource, instanceid)
  }

  /**
   * Logic to retrieve the state for a resource on request. For complex
   * resources, it expects an instance id to be registered with the
   * resource state. For non complex resources, if the resource has not
   * been used yet, it will create an entry in the state store.
   */
  private def resourceState(resource: DSLResource, instanceId: Option[String]):
  Option[UserActorResourceStateResponse] =
    resource.isComplex match {
      case true ⇒ instanceId match {
        case Some(x) ⇒ _userState.ownedResources.data.get(resource) match {
          case Some(y) if (y.isInstanceOf[Map[String, Any]]) ⇒
            val measurement = y.asInstanceOf[Map[String, Any]].get(x)
            Some(UserActorResourceStateResponse(resource, new Date(_userState.ownedResources.snapshotTime), measurement))
          case Some(y) ⇒
            warn("Should never reach this line")
            Some(UserActorResourceStateResponse(resource, new Date(_userState.ownedResources.snapshotTime), y))
          case None ⇒
            err("No instance %s for resource".format(instanceId.get, resource.name))
            None
        }
        case None =>
          err("No instance id for complex resource %s".format(resource.name))
          None
      }
      case false ⇒ _userState.ownedResources.data.get(resource) match {
        case Some(y) ⇒
          Some(UserActorResourceStateResponse(resource, new Date(_userState.ownedResources.snapshotTime), y))
        case None ⇒
          debug("First request for resource %s, creating...".format(resource.name))
          val _ownedResources = _userState.ownedResources
          val _ownedResourcesData = _ownedResources.data
          _userState = _userState.copy(ownedResources = _ownedResources.copy(data = _ownedResourcesData.updated(resource, "")))
          resourceState(resource, None)
      }
    }

  /**
   * Update the current state for a resource
   */
  private def resourceStateUpdate(resource: DSLResource,
                                  instanceid: Option[String],
                                  value: Any) = None

  /**
   * Create a new instance for a complex resource
   */
  private def createResourceInstance(resource: DSLResource,
                                    instanceid: String) = None

  /**
   * Delete a resource instance for a complex resource
   */
  private def deleteResourceInstance(resource: DSLResource,
                                     instanceid: String) = None

  private def debug(msg: String) =
    logger.debug("UserActor[%s] %s".format(_userId, msg))

  private def warn(msg: String) =
    logger.warn("UserActor[%s] %s".format(_userId, msg))

  private def err(msg: String) =
    logger.error("UserActor[%s] %s".format(_userId, msg))
}