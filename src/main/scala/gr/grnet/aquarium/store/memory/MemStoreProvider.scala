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

package gr.grnet.aquarium.store.memory

import com.ckkloverdos.props.Props
import com.ckkloverdos.maybe.Just
import gr.grnet.aquarium.store._
import scala.collection.JavaConversions._
import collection.mutable.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap
import gr.grnet.aquarium.Configurable
import gr.grnet.aquarium.event.model.im.{StdIMEvent, IMEventModel}
import org.bson.types.ObjectId
import gr.grnet.aquarium.event.model.resource.{StdResourceEvent, ResourceEventModel}
import gr.grnet.aquarium.computation.state.UserState
import gr.grnet.aquarium.util.Tags
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.policy.{PolicyModel, StdPolicy}
import collection.immutable.SortedMap
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot

/**
 * An implementation of various stores that persists parts in memory.
 *
 * This is just for testing purposes.
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Prodromos Gerakios <grnet.gr>
 */

class MemStoreProvider
extends StoreProvider
   with UserStateStore
   with Configurable
   with PolicyStore
   with ResourceEventStore
   with IMEventStore {

  override type IMEvent = MemIMEvent
  override type ResourceEvent = MemResourceEvent
  override type Policy = StdPolicy

  private[this] var _userStates = List[UserState]()
  private[this] var _policies  = List[Policy]()
  private[this] var _resourceEvents = List[ResourceEvent]()

  private[this] val imEventById: ConcurrentMap[String, MemIMEvent] = new ConcurrentHashMap[String, MemIMEvent]()


  def propertyPrefix = None

  def configure(props: Props) = {
  }

  override def toString = {
    val map = Map(
      Tags.UserStateTag     -> _userStates.size,
      Tags.ResourceEventTag -> _resourceEvents.size,
      Tags.IMEventTag       -> imEventById.size,
      "PolicyEntry"         -> _policies.size
    )

    "MemStoreProvider(%s)" format map
  }

  //+ StoreProvider
  def userStateStore = this

  def resourceEventStore = this

  def imEventStore = this

  def policyStore = this
  //- StoreProvider


  //+ UserStateStore
  def insertUserState(userState: UserState): UserState = {
    _userStates = userState.copy(_id = new ObjectId().toString) :: _userStates
    userState
  }

  def findUserStateByUserID(userID: String) = {
    _userStates.find(_.userID == userID)
  }

  def findLatestUserStateForFullMonthBilling(userID: String, bmi: BillingMonthInfo): Option[UserState] = {
    val goodOnes = _userStates.filter(_.theFullBillingMonth.isDefined).filter { userState ⇒
        val f1 = userState.userID == userID
        val f2 = userState.isFullBillingMonthState
        val bm = userState.theFullBillingMonth.get
        val f3 = bm == bmi

        f1 && f2 && f3
    }
    
    goodOnes.sortWith {
      case (us1, us2) ⇒
        us1.occurredMillis > us2.occurredMillis
    } match {
      case head :: _ ⇒
        Some(head)
      case _ ⇒
        None
    }
  }
  //- UserStateStore

  //+ ResourceEventStore
  def createResourceEventFromOther(event: ResourceEventModel): ResourceEvent = {
    if(event.isInstanceOf[MemResourceEvent]) event.asInstanceOf[MemResourceEvent]
    else {
      import event._
      new StdResourceEvent(
        id,
        occurredMillis,
        receivedMillis,
        userID,
        clientID,
        resource,
        instanceID,
        value,
        eventVersion,
        details
      ): MemResourceEvent
    }
  }

  override def clearResourceEvents() = {
    _resourceEvents = Nil
  }

  def pingResourceEventStore(): Unit = {
    // We are always live and kicking...
  }

  def insertResourceEvent(event: ResourceEventModel) = {
    val localEvent = createResourceEventFromOther(event)
    _resourceEvents ::= localEvent
    localEvent
  }

  def findResourceEventByID(id: String) = {
    _resourceEvents.find(ev ⇒ ev.id == id)
  }

  def findResourceEventsByUserID(userId: String)
                                (sortWith: Option[(ResourceEvent, ResourceEvent) => Boolean]): List[ResourceEvent] = {
    val byUserId = _resourceEvents.filter(_.userID == userId).toArray
    val sorted = sortWith match {
      case Some(sorter) ⇒
        byUserId.sortWith(sorter)
      case None ⇒
        byUserId
    }

    sorted.toList
  }

  def countOutOfSyncResourceEventsForBillingPeriod(userID: String, startMillis: Long, stopMillis: Long): Long = {
    _resourceEvents.filter { case ev ⇒
      ev.userID == userID &&
      // out of sync events are those that were received in the billing month but occurred in previous (or next?)
      // months
      ev.isOutOfSyncForBillingPeriod(startMillis, stopMillis)
    }.size.toLong
  }
  //- ResourceEventStore

  def foreachResourceEventOccurredInPeriod(
      userID: String,
      startMillis: Long,
      stopMillis: Long
  )(f: ResourceEvent ⇒ Unit): Unit = {
    _resourceEvents.filter { case ev ⇒
      ev.userID == userID &&
      ev.isOccurredWithinMillis(startMillis, stopMillis)
    }.foreach(f)
  }

  //+ IMEventStore
  def createIMEventFromJson(json: String) = {
    StdIMEvent.fromJsonString(json)
  }

  def createIMEventFromOther(event: IMEventModel) = {
    StdIMEvent.fromOther(event)
  }

  def pingIMEventStore(): Unit = {
  }


  def insertIMEvent(event: IMEventModel) = {
    val localEvent = createIMEventFromOther(event)
    imEventById += (event.id -> localEvent)
    localEvent
  }

  def findIMEventByID(id: String) = imEventById.get(id)


  /**
   * Find the `CREATE` even for the given user. Note that there must be only one such event.
   */
  def findCreateIMEventByUserID(userID: String): Option[IMEvent] = {
    imEventById.valuesIterator.filter { e ⇒
      e.userID == userID && e.isCreateUser
    }.toList.sortWith { case (e1, e2) ⇒
      e1.occurredMillis < e2.occurredMillis
    } headOption
  }

  def findLatestIMEventByUserID(userID: String): Option[IMEvent] = {
    imEventById.valuesIterator.filter(_.userID == userID).toList.sortWith {
      case (us1, us2) ⇒
        us1.occurredMillis > us2.occurredMillis
    } headOption
  }

  /**
   * Scans events for the given user, sorted by `occurredMillis` in ascending order and runs them through
   * the given function `f`.
   *
   * Any exception is propagated to the caller. The underlying DB resources are properly disposed in any case.
   */
  def foreachIMEventInOccurrenceOrder(userID: String)(f: (IMEvent) => Unit) = {
    imEventById.valuesIterator.filter(_.userID == userID).toSeq.sortWith {
      case (ev1, ev2) ⇒ ev1.occurredMillis <= ev2.occurredMillis
    } foreach(f)
  }
  //- IMEventStore

  /**
   * Store an accounting policy.
   */
  def insertPolicy(policy: PolicyModel): Policy = {
    val localPolicy = StdPolicy(
      id = policy.id,
      parentID = policy.parentID,
      validityTimespan = policy.validityTimespan,
      resourceTypes = policy.resourceTypes,
      chargingBehaviors = policy.chargingBehaviors,
      roleMapping = policy.roleMapping
    )
    _policies = localPolicy :: _policies

    localPolicy
  }

  def loadValidPolicyAt(atMillis: Long): Option[Policy] = {
    throw new UnsupportedOperationException
  }

  def loadAndSortPoliciesWithin(fromMillis: Long, toMillis: Long): SortedMap[Timeslot, Policy] = {
    throw new UnsupportedOperationException
  }
}

object MemStoreProvider {
  final def isLocalIMEvent(event: IMEventModel) = event match {
    case _: MemIMEvent ⇒ true
    case _ ⇒ false
  }
}