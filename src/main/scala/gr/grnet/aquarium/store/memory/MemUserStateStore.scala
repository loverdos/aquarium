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

/**
 * A user store backed by main memory.
 *
 * The IDs returned are the original user IDs.
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>
 * @author Georgios Gousios <gousiosg@gmail.com>
 */

class MemUserStateStore extends UserStateStore
  with Configurable with PolicyStore
  with ResourceEventStore with UserEventStore
  with WalletEntryStore {

  private[this] val userStateByUserId = new ConcurrentHashMap[String, Just[UserState]]()
  private val policyById: ConcurrentMap[String, PolicyEntry] = new ConcurrentHashMap[String, PolicyEntry]()
  
  def configure(props: Props) = {
  }

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

  def storeWalletEntry(entry: WalletEntry) = null

  def findWalletEntryById(id: String) = null

  def findUserWalletEntries(userId: String) = null

  def findUserWalletEntriesFromTo(userId: String, from: Date, to: Date) = null

  def findLatestUserWalletEntries(userId: String) = null

  def findPreviousEntry(userId: String, resource: String, instanceId: String, finalized: Option[Boolean]) = null

  def findWalletEntriesAfter(userId: String, from: Date) = null

  def storeResourceEvent(event: ResourceEvent) = null

  def findResourceEventById(id: String) = null

  def findResourceEventsByUserId(userId: String)(sortWith: Option[(ResourceEvent, ResourceEvent) => Boolean]) = null

  def findResourceEventsByUserIdAfterTimestamp(userId: String, timestamp: Long) = null

  def findResourceEventHistory(userId: String, resName: String, instid: Option[String], upTo: Long) = null

  def findResourceEventsForReceivedPeriod(userId: String, startTimeMillis: Long, stopTimeMillis: Long) = null

  def countOutOfSyncEventsForBillingMonth(userId: String, yearOfBillingMonth: Int, billingMonth: Int) = null

  def storeUserEvent(event: UserEvent) = null

  def findUserEventById(id: String) = null

  def findUserEventsByUserId(userId: String) = null

  def loadPolicies(after: Long) = policyById.values.foldLeft(List[PolicyEntry]()){
    (acc, v) => if(v.validFrom > after) v :: acc else acc
  }

  def storePolicy(policy: PolicyEntry) = {policyById += (policy.id -> policy); Just(RecordID(policy.id))}

  def updatePolicy(policy: PolicyEntry) = storePolicy(policy)
}