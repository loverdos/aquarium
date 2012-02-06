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

package gr.grnet.aquarium.store.memory

import gr.grnet.aquarium.user.UserState
import gr.grnet.aquarium.Configurable
import com.ckkloverdos.props.Props
import com.ckkloverdos.maybe.{NoVal, Just, Maybe}
import gr.grnet.aquarium.store._
import scala.collection.JavaConversions._
import java.util.Date
import collection.mutable.ConcurrentMap
import gr.grnet.aquarium.logic.events.{WalletEntry, ResourceEvent, UserEvent, PolicyEntry}
import java.util.concurrent.ConcurrentHashMap
import gr.grnet.aquarium.util.date.DateCalculator

/**
 * An implementation of various stores that persists data in memory
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>
 * @author Georgios Gousios <gousiosg@gmail.com>
 */

class MemStore extends UserStateStore
  with Configurable with PolicyStore
  with ResourceEventStore with UserEventStore
  with WalletEntryStore
  with StoreProvider {

  private[this] val userStateByUserId = new ConcurrentHashMap[String, Just[UserState]]()
  private val policyById: ConcurrentMap[String, PolicyEntry] = new ConcurrentHashMap[String, PolicyEntry]()
  private[this] val walletEntriesById: ConcurrentMap[String, WalletEntry] = new ConcurrentHashMap[String, WalletEntry]()
  private val userEventById: ConcurrentMap[String, UserEvent] = new ConcurrentHashMap[String, UserEvent]()
  private[this] val resourceEventsById: ConcurrentMap[String, ResourceEvent] = new ConcurrentHashMap[String, ResourceEvent]()

  def configure(props: Props) = {
  }

  //+ StoreProvider
  def userStateStore = this

  def resourceEventStore = this

  def walletEntryStore = this

  def userEventStore = this

  def policyStore = this
  //- StoreProvider


  def storeUserState(userState: UserState): Maybe[RecordID] = {
    val userId = userState.userId
    val userStateJ = Just(userState)
    userStateByUserId.put(userId, userStateJ)
    Just(RecordID(userId))
  }

  def findUserStateByUserId(userId: String) = {
    userStateByUserId.get(userId) match {
      case null       ⇒ NoVal
      case userStateJ ⇒ userStateJ
    }
  }

  def deleteUserState(userId: String) {
    if (userStateByUserId.containsKey(userId))
      userStateByUserId.remove(userId)
  }

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
  def storeResourceEvent(event: ResourceEvent) = {
    resourceEventsById(event.id) = event
    Just(RecordID(event.id))
  }

  def findResourceEventById(id: String) = {
    Maybe(resourceEventsById(id))
  }

  def findResourceEventsByUserId(userId: String)
                                (sortWith: Option[(ResourceEvent, ResourceEvent) => Boolean]): List[ResourceEvent] = {
    val byUserId = resourceEventsById.valuesIterator.filter(_.userId == userId).toArray
    val sorted = sortWith match {
      case Some(sorter) ⇒
        byUserId.sortWith(sorter)
      case None ⇒
        byUserId
    }

    sorted.toList
  }

  def findResourceEventsByUserIdAfterTimestamp(userId: String, timestamp: Long): List[ResourceEvent] = {
    resourceEventsById.valuesIterator.filter { ev ⇒
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
    resourceEventsById.valuesIterator.filter { ev ⇒
      ev.userId == userId &&
      ev.receivedMillis >= startTimeMillis &&
      ev.receivedMillis <= stopTimeMillis
    }.toList
  }

  def countOutOfSyncEventsForBillingMonth(userId: String, yearOfBillingMonth: Int, billingMonth: Int): Maybe[Long] = Maybe {
    val billingMonthDate = new DateCalculator(yearOfBillingMonth, billingMonth)
    val billingDateStart = billingMonthDate
    val billingDateEnd = billingDateStart.goEndOfThisMonth
    resourceEventsById.valuesIterator.filter { case ev ⇒
      // out of sync events are those that were received in the billing month but occurred in previous months
      val receivedMillis = ev.receivedMillis
      val occurredMillis = ev.occurredMillis

      billingDateStart.isAfterEqMillis(receivedMillis) && // the events that...
      billingDateEnd.isBeforeEqMillis (receivedMillis) && // ...were received withing the billing month
      (                                                 //
        billingDateStart.isAfterMillis(occurredMillis)    // but occurred before the billing period
        )
    }.size.toLong
  }
  //- ResourceEventStore

  def storeUserEvent(event: UserEvent) = {userEventById += (event.id -> event); Just(RecordID(event.id))}

  def findUserEventById(id: String) = Maybe{userEventById.getOrElse(id, null)}

  def findUserEventsByUserId(userId: String) = userEventById.values.filter{v => v.userId == userId}.toList

  def loadPolicies(after: Long) = policyById.values.foldLeft(List[PolicyEntry]()){
    (acc, v) => if(v.validFrom > after) v :: acc else acc
  }

  def storePolicy(policy: PolicyEntry) = {policyById += (policy.id -> policy); Just(RecordID(policy.id))}

  def updatePolicy(policy: PolicyEntry) = storePolicy(policy)
}