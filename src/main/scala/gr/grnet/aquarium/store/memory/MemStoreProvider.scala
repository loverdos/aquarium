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

import collection.immutable
import collection.immutable.SortedMap
import com.ckkloverdos.props.Props
import gr.grnet.aquarium.Configurable
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.message.avro.gen.{UserAgreementHistoryMsg, UserStateMsg, IMEventMsg, ResourceEventMsg, PolicyMsg}
import gr.grnet.aquarium.message.avro.{MessageFactory, MessageHelpers, OrderingHelpers}
import gr.grnet.aquarium.store._
import gr.grnet.aquarium.util.{Loggable, Tags}

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
   with IMEventStore
   with Loggable {

  private[this] var _userStates = immutable.TreeSet[UserStateMsg]()(OrderingHelpers.DefaultUserStateMsgOrdering)
  private[this] var _policies = immutable.TreeSet[PolicyMsg]()(OrderingHelpers.DefaultPolicyMsgOrdering)
  private[this] var _resourceEvents = immutable.TreeSet[ResourceEventMsg]()(OrderingHelpers.DefaultResourceEventMsgOrdering)
  private[this] var _imEvents = immutable.TreeSet[IMEventMsg]()(OrderingHelpers.DefaultIMEventMsgOrdering)

  def propertyPrefix = None

  def configure(props: Props) = {
  }

  override def toString = {
    val map = Map(
      Tags.UserStateTag     -> _userStates.size,
      Tags.ResourceEventTag -> _resourceEvents.size,
      Tags.IMEventTag       -> _imEvents.size,
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
  def insertUserState(event: UserStateMsg) = {
    event.setInStoreID(event.getOriginalID)
    _userStates += event
    event
  }

  def findUserStateByUserID(userID: String) = {
    _userStates.find(_.getUserID == userID)
  }

  def findLatestUserStateForFullMonthBilling(userID: String, bmi: BillingMonthInfo) = {
    _userStates.filter { userState ⇒
      userState.getUserID == userID &&
      userState.getIsFullBillingMonth &&
      userState.getBillingYear == bmi.year &&
      userState.getBillingMonth == bmi.month
    }.lastOption
  }

  def findLatestUserState(userID: String) = {
    _userStates.filter(_.getUserID == userID).lastOption
  }
  //- UserStateStore

  //+ ResourceEventStore
  def pingResourceEventStore(): Unit = {
    // We are always live and kicking...
  }

  def insertResourceEvent(event: ResourceEventMsg) = {
    event.setInStoreID(event.getOriginalID)
    _resourceEvents += event
    event
  }

  def findResourceEventByID(id: String) = {
    _resourceEvents.find(_.getOriginalID == id)
  }

  def countOutOfSyncResourceEventsForBillingPeriod(userID: String, startMillis: Long, stopMillis: Long): Long = {
    _resourceEvents.filter { case ev ⇒
      ev.getUserID == userID &&
      // out of sync events are those that were received in the billing month but occurred in previous (or next?)
      // months
      MessageHelpers.isOutOfSyncForBillingPeriod(ev, startMillis, stopMillis)
    }.size.toLong
  }
  //- ResourceEventStore

  def foreachResourceEventOccurredInPeriod(
      userID: String,
      startMillis: Long,
      stopMillis: Long
  )(f: ResourceEventMsg ⇒ Unit): Unit = {
    _resourceEvents.filter { case ev ⇒
      ev.getUserID == userID &&
      MessageHelpers.isOccurredWithinMillis(ev, startMillis, stopMillis)
    }.foreach(f)
  }

  //+ IMEventStore
  def pingIMEventStore(): Unit = {
  }


  def insertIMEvent(event: IMEventMsg) = {
    event.setInStoreID(event.getOriginalID)
    _imEvents += event
    event
  }

  def findIMEventByID(id: String) = {
    _imEvents.find(_.getOriginalID == id)
  }


  /**
   * Find the `CREATE` even for the given user. Note that there must be only one such event.
   */
  def findCreateIMEventByUserID(userID: String) = {
    _imEvents.find { event ⇒
      event.getUserID() == userID && MessageHelpers.isIMEventCreate(event)
    }
  }

  /**
   * Scans events for the given user, sorted by `occurredMillis` in ascending order and runs them through
   * the given function `f`.
   *
   * Any exception is propagated to the caller. The underlying DB resources are properly disposed in any case.
   */
  def foreachIMEventInOccurrenceOrder(userID: String)(f: (IMEventMsg) ⇒ Boolean) = {
    var _shouldContinue = true
    for {
      msg <- _imEvents if _shouldContinue
    } {
      _shouldContinue = f(msg)
    }
    _shouldContinue
  }
  //- IMEventStore

  //+ PolicyStore
  def insertPolicy(policy: PolicyMsg): PolicyMsg = synchronized {
    policy.setInStoreID(policy.getOriginalID)
    _policies += policy
    policy
  }

  def loadPolicyAt(atMillis: Long): Option[PolicyMsg] = synchronized {
    _policies.to(MessageFactory.newDummyPolicyMsgAt(atMillis)).lastOption
  }

  def loadSortedPoliciesWithin(fromMillis: Long, toMillis: Long): SortedMap[Timeslot, PolicyMsg] = {
    immutable.SortedMap(_policies.
      from(MessageFactory.newDummyPolicyMsgAt(fromMillis)).
      to(MessageFactory.newDummyPolicyMsgAt(toMillis)).toSeq.
      map(p ⇒ (Timeslot(p.getValidFromMillis, p.getValidToMillis), p)): _*
    )
  }

  def foreachPolicy[U](f: (PolicyMsg) ⇒ U) {
    _policies.foreach(f)
  }
  //- PolicyStore
}
