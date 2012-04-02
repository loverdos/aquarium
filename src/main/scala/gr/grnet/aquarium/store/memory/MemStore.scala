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
import com.ckkloverdos.maybe.{NoVal, Just, Maybe}
import gr.grnet.aquarium.store._
import scala.collection.JavaConversions._
import java.util.Date
import collection.mutable.ConcurrentMap
import gr.grnet.aquarium.logic.events.{WalletEntry, ResourceEvent, UserEvent, PolicyEntry}
import java.util.concurrent.ConcurrentHashMap
import gr.grnet.aquarium.user.UserState
import gr.grnet.aquarium.simulation.uid.ConcurrentVMLocalUIDGenerator
import gr.grnet.aquarium.{AquariumException, Configurable}

/**
 * An implementation of various stores that persists data in memory.
 *
 * This is just for testing purposes.
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>
 * @author Georgios Gousios <gousiosg@gmail.com>
 */

class MemStore extends UserStateStore
  with Configurable with PolicyStore
  with ResourceEventStore with UserEventStore
  with WalletEntryStore
  with StoreProvider {

  private[this] val idGen = new ConcurrentVMLocalUIDGenerator(1000)
  
  private[this] var _userStates     = List[UserState]()
  private[this] var _policyEntries  = List[PolicyEntry]()
  private[this] var _resourceEvents = List[ResourceEvent]()

  private[this] val walletEntriesById: ConcurrentMap[String, WalletEntry] = new ConcurrentHashMap[String, WalletEntry]()
  private val userEventById: ConcurrentMap[String, UserEvent] = new ConcurrentHashMap[String, UserEvent]()

  def configure(props: Props) = {
  }

  override def toString = {
    val map = Map(
      "UserState"     -> _userStates.size,
      "ResourceEvent" -> _resourceEvents.size,
      "UserEvent"     -> userEventById.size,
      "PolicyEntry"   -> _policyEntries.size,
      "WalletEntry"   -> walletEntriesById.size
    )

    "MemStore(%s)" format map
  }

  //+ StoreProvider
  def userStateStore = this

  def resourceEventStore = this

  def walletEntryStore = this

  def userEventStore = this

  def policyStore = this
  //- StoreProvider


  //+ UserStateStore
  def storeUserState(userState: UserState): Maybe[RecordID] = {
    _userStates = userState.copy(id = idGen.nextUID()) :: _userStates
    Just(RecordID(_userStates.head._id))
  }

  def findUserStateByUserId(userId: String) = {
    _userStates.find(_.userId == userId) match {
      case Some(userState) ⇒
        Just(userState)
      case None ⇒
        NoVal
    }
  }

  def findLatestUserStateForEndOfBillingMonth(userId: String,
                                              yearOfBillingMonth: Int,
                                              billingMonth: Int): Maybe[UserState] = {
    val goodOnes = _userStates.filter { userState ⇒
        val f1 = userState.userId == userId
        val f2 = userState.isFullBillingMonthState
        val bm = userState.theFullBillingMonth
        val f3 = (bm ne null) && {
          bm.year == yearOfBillingMonth && bm.month == billingMonth
        }

        f1 && f2 && f3
    }
    
    goodOnes.sortWith {
      case (us1, us2) ⇒
        us1.oldestSnapshotTime > us2.oldestSnapshotTime
    } match {
      case head :: _ ⇒
        Just(head)
      case _ ⇒
        NoVal
    }
  }

  def deleteUserState(userId: String) {
    _userStates.filterNot(_.userId == userId)
  }
  //- UserStateStore

  //- WalletEntryStore
  def storeWalletEntry(entry: WalletEntry): Maybe[RecordID] = {
    walletEntriesById.put(entry.id, entry)
    Just(RecordID(entry.id))
  }

  def findWalletEntryById(id: String): Maybe[WalletEntry] = {
    Maybe(walletEntriesById.apply(id))
  }

  def findUserWalletEntries(userId: String): List[WalletEntry] = {
    walletEntriesById.valuesIterator.filter(_.userId == userId).toList
  }

  def findUserWalletEntriesFromTo(userId: String, from: Date, to: Date): List[WalletEntry] = {
    walletEntriesById.valuesIterator.filter { we ⇒
      val receivedDate = we.receivedDate

      we.userId == userId &&
      ( (from before receivedDate) || (from == receivedDate) ) &&
      ( (to   after  receivedDate) || (to   == receivedDate) )
      true
    }.toList
  }

  def findLatestUserWalletEntries(userId: String): Maybe[List[WalletEntry]] = NoVal

  def findPreviousEntry(userId: String,
                        resource: String,
                        instanceId: String,
                        finalized: Option[Boolean]): List[WalletEntry] = Nil

  def findWalletEntriesAfter(userId: String, from: Date): List[WalletEntry] = {
    walletEntriesById.valuesIterator.filter { we ⇒
      val occurredDate = we.occurredDate

      we.userId == userId &&
            ( (from before occurredDate) || (from == occurredDate) )
    }.toList
  }
  //- WalletEntryStore

  //+ ResourceEventStore

  override def clearResourceEvents() = {
    _resourceEvents = Nil
  }

  def storeResourceEvent(event: ResourceEvent) = {
    _resourceEvents ::= event
    Just(RecordID(event.id))
  }

  def findResourceEventById(id: String) = {
    _resourceEvents.find(ev ⇒ ev.id == id) match {
      case Some(ev) ⇒ Just(ev)
      case None     ⇒ NoVal
    }
  }

  def findResourceEventsByUserId(userId: String)
                                (sortWith: Option[(ResourceEvent, ResourceEvent) => Boolean]): List[ResourceEvent] = {
    val byUserId = _resourceEvents.filter(_.userId == userId).toArray
    val sorted = sortWith match {
      case Some(sorter) ⇒
        byUserId.sortWith(sorter)
      case None ⇒
        byUserId
    }

    sorted.toList
  }

  def findResourceEventsByUserIdAfterTimestamp(userId: String, timestamp: Long): List[ResourceEvent] = {
    _resourceEvents.filter { ev ⇒
      ev.userId == userId &&
      (ev.occurredMillis > timestamp)
    }.toList
  }

  def findResourceEventHistory(userId: String,
                               resName: String,
                               instid: Option[String],
                               upTo: Long): List[ResourceEvent] = {
    Nil
  }

  def findResourceEventsForReceivedPeriod(userId: String,
                                          startTimeMillis: Long,
                                          stopTimeMillis: Long): List[ResourceEvent] = {
    _resourceEvents.filter { ev ⇒
      ev.userId == userId &&
      ev.isReceivedWithinMillis(startTimeMillis, stopTimeMillis)
    }.toList
  }

  def countOutOfSyncEventsForBillingPeriod(userId: String, startMillis: Long, stopMillis: Long): Maybe[Long] = Maybe {
    _resourceEvents.filter { case ev ⇒
      // out of sync events are those that were received in the billing month but occurred in previous (or next?)
      // months
      ev.isOutOfSyncForBillingPeriod(startMillis, stopMillis)
    }.size.toLong
  }

  /**
   * Finds all relevant resource events for the billing period.
   * The relevant events are those:
   * a) whose `occurredMillis` is within the given billing period or
   * b) whose `receivedMillis` is within the given billing period.
   *
   * Order them by `occurredMillis`
   */
  override def findAllRelevantResourceEventsForBillingPeriod(userId: String,
                                                             startMillis: Long,
                                                             stopMillis: Long): List[ResourceEvent] = {
    _resourceEvents.filter { case ev ⇒
      ev.isOccurredOrReceivedWithinMillis(startMillis, stopMillis)
    }.toList sortWith { case (ev1, ev2) ⇒ ev1.occurredMillis <= ev2.occurredMillis }
  }
  //- ResourceEventStore

  //+ UserEventStore
  def storeUnparsed(json: String) = throw new AquariumException("Not implemented")

  def storeUserEvent(event: UserEvent) = {userEventById += (event.id -> event); Just(RecordID(event.id))}

  def findUserEventById(id: String) = Maybe{userEventById.getOrElse(id, null)}

  def findUserEventsByUserId(userId: String) = userEventById.valuesIterator.filter{v => v.userID == userId}.toList
  //- UserEventStore

  def loadPolicyEntriesAfter(after: Long) =
    _policyEntries.filter(p => p.validFrom > after)
            .sortWith((a,b) => a.validFrom < b.validFrom)

  def storePolicyEntry(policy: PolicyEntry) = {_policyEntries = policy :: _policyEntries; Just(RecordID(policy.id))}

  def updatePolicyEntry(policy: PolicyEntry) =
    _policyEntries = _policyEntries.foldLeft(List[PolicyEntry]()){
      (acc, p) =>
        if (p.id == policy.id)
          policy :: acc
        else
          p :: acc
  }

  def findPolicyEntry(id: String) = _policyEntries.find(p => p.id == id) match {
    case Some(x) => Just(x)
    case None => NoVal
  }
}