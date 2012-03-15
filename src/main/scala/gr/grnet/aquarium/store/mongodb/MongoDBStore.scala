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

package gr.grnet.aquarium.store.mongodb

import gr.grnet.aquarium.util.Loggable
import com.mongodb.util.JSON
import gr.grnet.aquarium.user.UserState
import gr.grnet.aquarium.user.UserState.{JsonNames => UserStateJsonNames}
import gr.grnet.aquarium.util.displayableObjectInfo
import gr.grnet.aquarium.util.json.JsonSupport
import collection.mutable.ListBuffer
import gr.grnet.aquarium.store._
import gr.grnet.aquarium.logic.events.ResourceEvent.{JsonNames => ResourceJsonNames}
import gr.grnet.aquarium.logic.events.UserEvent.{JsonNames => UserEventJsonNames}
import gr.grnet.aquarium.logic.events.WalletEntry.{JsonNames => WalletJsonNames}
import gr.grnet.aquarium.logic.events.PolicyEntry.{JsonNames => PolicyJsonNames}
import java.util.Date
import gr.grnet.aquarium.logic.accounting.Policy
import gr.grnet.aquarium.logic.events._
import com.mongodb._
import com.ckkloverdos.maybe.{NoVal, Maybe}
import gr.grnet.aquarium.logic.accounting.dsl.{DSLResource, Timeslot, DSLPolicy, DSLComplexResource}

/**
 * Mongodb implementation of the various aquarium stores.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class MongoDBStore(
    val mongo: Mongo,
    val database: String,
    val username: String,
    val password: String)
  extends ResourceEventStore
  with UserStateStore
  with WalletEntryStore
  with UserEventStore
  with PolicyStore
  with Loggable {

  private[store] lazy val resourceEvents = getCollection(MongoDBStore.RESOURCE_EVENTS_COLLECTION)
  private[store] lazy val userStates     = getCollection(MongoDBStore.USER_STATES_COLLECTION)
  private[store] lazy val userEvents     = getCollection(MongoDBStore.USER_EVENTS_COLLECTION)
  private[store] lazy val walletEntries  = getCollection(MongoDBStore.WALLET_ENTRIES_COLLECTION)
//  private[store] lazy val policies       = getCollection(MongoDBStore.POLICIES_COLLECTION)
  private[store] lazy val policyEntries  = getCollection(MongoDBStore.POLICY_ENTRIES_COLLECTION)

  private[this] def getCollection(name: String): DBCollection = {
    val db = mongo.getDB(database)
    //logger.debug("Authenticating to mongo")
    if(!db.isAuthenticated && !db.authenticate(username, password.toCharArray)) {
      throw new StoreException("Could not authenticate user %s".format(username))
    }
    db.getCollection(name)
  }

  private[this] def _sortByTimestampAsc[A <: AquariumEvent](one: A, two: A): Boolean = {
    if (one.occurredMillis > two.occurredMillis) false
    else if (one.occurredMillis < two.occurredMillis) true
    else true
  }

  private[this] def _sortByTimestampDesc[A <: AquariumEvent](one: A, two: A): Boolean = {
    if (one.occurredMillis < two.occurredMillis) false
    else if (one.occurredMillis > two.occurredMillis) true
    else true
  }

  //+ResourceEventStore
  def storeResourceEvent(event: ResourceEvent): Maybe[RecordID] =
    MongoDBStore.storeAquariumEvent(event, resourceEvents)

  def findResourceEventById(id: String): Maybe[ResourceEvent] =
    MongoDBStore.findById(id, resourceEvents, MongoDBStore.dbObjectToResourceEvent)

  def findResourceEventsByUserId(userId: String)
                                (sortWith: Option[(ResourceEvent, ResourceEvent) => Boolean]): List[ResourceEvent] = {
    val query = new BasicDBObject(ResourceJsonNames.userId, userId)

    MongoDBStore.runQuery(query, resourceEvents)(MongoDBStore.dbObjectToResourceEvent)(sortWith)
  }

  def findResourceEventsByUserIdAfterTimestamp(userId: String, timestamp: Long): List[ResourceEvent] = {
    val query = new BasicDBObject()
    query.put(ResourceJsonNames.userId, userId)
    query.put(ResourceJsonNames.occurredMillis, new BasicDBObject("$gt", timestamp))
    
    val sort = new BasicDBObject(ResourceJsonNames.occurredMillis, 1)

    val cursor = resourceEvents.find(query).sort(sort)

    try {
      val buffer = new scala.collection.mutable.ListBuffer[ResourceEvent]
      while(cursor.hasNext) {
        buffer += MongoDBStore.dbObjectToResourceEvent(cursor.next())
      }
      buffer.toList.sortWith(_sortByTimestampAsc)
    } finally {
      cursor.close()
    }
  }

  def findResourceEventHistory(userId: String, resName: String,
                               instid: Option[String], upTo: Long) : List[ResourceEvent] = {
    val query = new BasicDBObject()
    query.put(ResourceJsonNames.userId, userId)
    query.put(ResourceJsonNames.occurredMillis, new BasicDBObject("$lt", upTo))
    query.put(ResourceJsonNames.resource, resName)

    instid match {
      case Some(id) =>
        Policy.policy.findResource(resName) match {
          case Some(y) => query.put(ResourceJsonNames.details,
            new BasicDBObject(y.descriminatorField, instid.get))
          case None =>
        }
      case None =>
    }

    val sort = new BasicDBObject(ResourceJsonNames.occurredMillis, 1)
    val cursor = resourceEvents.find(query).sort(sort)

    try {
      val buffer = new scala.collection.mutable.ListBuffer[ResourceEvent]
      while(cursor.hasNext) {
        buffer += MongoDBStore.dbObjectToResourceEvent(cursor.next())
      }
      buffer.toList.sortWith(_sortByTimestampAsc)
    } finally {
      cursor.close()
    }
  }

  def findResourceEventsForReceivedPeriod(userId: String, startTimeMillis: Long, stopTimeMillis: Long): List[ResourceEvent] = {
    val query = new BasicDBObject()
    query.put(ResourceJsonNames.userId, userId)
    query.put(ResourceJsonNames.receivedMillis, new BasicDBObject("$gte", startTimeMillis))
    query.put(ResourceJsonNames.receivedMillis, new BasicDBObject("$lte", stopTimeMillis))

    // Sort them by increasing order for occurred time
    val orderBy = new BasicDBObject(ResourceJsonNames.occurredMillis, 1)

    MongoDBStore.runQuery[ResourceEvent](query, resourceEvents, orderBy)(MongoDBStore.dbObjectToResourceEvent)(None)
  }
  
  def countOutOfSyncEventsForBillingPeriod(userId: String, startMillis: Long, stopMillis: Long): Maybe[Long] = {
    Maybe {
      // FIXME: Implement
      0L
    }
  }

  def findAllRelevantResourceEventsForBillingPeriod(userId: String,
                                                    startMillis: Long,
                                                    stopMillis: Long): List[ResourceEvent] = {
    // FIXME: Implement
    Nil
  }
  //-ResourceEventStore

  //+ UserStateStore
  def storeUserState(userState: UserState): Maybe[RecordID] = {
    MongoDBStore.storeUserState(userState, userStates)
  }

  def findUserStateByUserId(userId: String): Maybe[UserState] = {
    Maybe {
      val query = new BasicDBObject(UserStateJsonNames.userId, userId)
      val cursor = userStates find query

      try {
        if(cursor.hasNext)
          MongoDBStore.dbObjectToUserState(cursor.next())
        else
          null
      } finally {
        cursor.close()
      }
    }
  }

  def findLatestUserStateForEndOfBillingMonth(userId: String,
                                              yearOfBillingMonth: Int,
                                              billingMonth: Int): Maybe[UserState] = {
    NoVal // FIXME: implement
  }

  def deleteUserState(userId: String) = {
    val query = new BasicDBObject(UserStateJsonNames.userId, userId)
    userStates.findAndRemove(query)
  }
  //- UserStateStore

  //+WalletEntryStore
  def storeWalletEntry(entry: WalletEntry): Maybe[RecordID] =
    MongoDBStore.storeAquariumEvent(entry, walletEntries)

  def findWalletEntryById(id: String): Maybe[WalletEntry] =
    MongoDBStore.findById[WalletEntry](id, walletEntries, MongoDBStore.dbObjectToWalletEntry)

  def findUserWalletEntries(userId: String) = {
    // TODO: optimize
    findUserWalletEntriesFromTo(userId, new Date(0), new Date(Int.MaxValue))
  }

  def findUserWalletEntriesFromTo(userId: String, from: Date, to: Date) : List[WalletEntry] = {
    val q = new BasicDBObject()
    // TODO: Is this the correct way for an AND query?
    q.put(WalletJsonNames.occurredMillis, new BasicDBObject("$gt", from.getTime))
    q.put(WalletJsonNames.occurredMillis, new BasicDBObject("$lt", to.getTime))
    q.put(WalletJsonNames.userId, userId)

    MongoDBStore.runQuery[WalletEntry](q, walletEntries)(MongoDBStore.dbObjectToWalletEntry)(Some(_sortByTimestampAsc))
  }

  def findWalletEntriesAfter(userId: String, from: Date) : List[WalletEntry] = {
    val q = new BasicDBObject()
    q.put(WalletJsonNames.occurredMillis, new BasicDBObject("$gt", from.getTime))
    q.put(WalletJsonNames.userId, userId)

    MongoDBStore.runQuery[WalletEntry](q, walletEntries)(MongoDBStore.dbObjectToWalletEntry)(Some(_sortByTimestampAsc))
  }

  def findLatestUserWalletEntries(userId: String) = {
    Maybe {
      val orderBy = new BasicDBObject(WalletJsonNames.occurredMillis, -1) // -1 is descending order
      val cursor = walletEntries.find().sort(orderBy)

      try {
        val buffer = new scala.collection.mutable.ListBuffer[WalletEntry]
        if(cursor.hasNext) {
          val walletEntry = MongoDBStore.dbObjectToWalletEntry(cursor.next())
          buffer += walletEntry

          var _previousOccurredMillis = walletEntry.occurredMillis
          var _ok = true

          while(cursor.hasNext && _ok) {
            val walletEntry = MongoDBStore.dbObjectToWalletEntry(cursor.next())
            var currentOccurredMillis = walletEntry.occurredMillis
            _ok = currentOccurredMillis == _previousOccurredMillis
            
            if(_ok) {
              buffer += walletEntry
            }
          }

          buffer.toList
        } else {
          null
        }
      } finally {
        cursor.close()
      }
    }
  }

  def findPreviousEntry(userId: String, resource: String,
                        instanceId: String,
                        finalized: Option[Boolean]): List[WalletEntry] = {
    val q = new BasicDBObject()
    q.put(WalletJsonNames.userId, userId)
    q.put(WalletJsonNames.resource, resource)
    q.put(WalletJsonNames.instanceId, instanceId)
    finalized match {
      case Some(x) => q.put(WalletJsonNames.finalized, x)
      case None =>
    }

    MongoDBStore.runQuery[WalletEntry](q, walletEntries)(MongoDBStore.dbObjectToWalletEntry)(Some(_sortByTimestampAsc))
  }
  //-WalletEntryStore

  //+UserEventStore
  def storeUserEvent(event: UserEvent): Maybe[RecordID] =
    MongoDBStore.storeAny[UserEvent](event, userEvents, UserEventJsonNames.userId,
      _.userID, MongoDBStore.jsonSupportToDBObject)


  def findUserEventById(id: String): Maybe[UserEvent] =
    MongoDBStore.findById[UserEvent](id, userEvents, MongoDBStore.dbObjectToUserEvent)

  def findUserEventsByUserId(userId: String): List[UserEvent] = {
    val query = new BasicDBObject(UserEventJsonNames.userId, userId)
    MongoDBStore.runQuery(query, userEvents)(MongoDBStore.dbObjectToUserEvent)(Some(_sortByTimestampAsc))
  }
  //-UserEventStore

  //+PolicyStore
  def loadPolicyEntriesAfter(after: Long): List[PolicyEntry] = {
    val query = new BasicDBObject(PolicyEntry.JsonNames.validFrom,
      new BasicDBObject("$gt", after))
    MongoDBStore.runQuery(query, policyEntries)(MongoDBStore.dbObjectToPolicyEntry)(Some(_sortByTimestampAsc))
  }

  def storePolicyEntry(policy: PolicyEntry): Maybe[RecordID] = MongoDBStore.storePolicyEntry(policy, policyEntries)


  def updatePolicyEntry(policy: PolicyEntry) = {
    //Find the entry
    val query = new BasicDBObject(PolicyEntry.JsonNames.id, policy.id)
    val policyObject = MongoDBStore.jsonSupportToDBObject(policy)
    policyEntries.update(query, policyObject, true, false)
  }
  
  def findPolicyEntry(id: String) =
    MongoDBStore.findById[PolicyEntry](id, policyEntries, MongoDBStore.dbObjectToPolicyEntry)

  //-PolicyStore
}

object MongoDBStore {
  object JsonNames {
    final val _id = "_id"
  }

  /**
   * Collection holding the [[gr.grnet.aquarium.logic.events.ResourceEvent]]s.
   *
   * Resource events are coming from all systems handling billable resources.
   */
  final val RESOURCE_EVENTS_COLLECTION = "resevents"

  /**
   * Collection holding the snapshots of [[gr.grnet.aquarium.user.UserState]].
   *
   * [[gr.grnet.aquarium.user.UserState]] is held internally within [[gr.grnet.aquarium.user.actor.UserActor]]s.
   */
  final val USER_STATES_COLLECTION = "userstates"

  /**
   * Collection holding [[gr.grnet.aquarium.logic.events.UserEvent]]s.
   *
   * User events are coming from the IM module (external).
   */
  final val USER_EVENTS_COLLECTION = "userevents"

  /**
   * Collection holding [[gr.grnet.aquarium.logic.events.WalletEntry]].
   *
   * Wallet entries are generated internally in Aquarium.
   */
  final val WALLET_ENTRIES_COLLECTION = "wallets"

  /**
   * Collection holding [[gr.grnet.aquarium.logic.accounting.dsl.DSLPolicy]].
   */
//  final val POLICIES_COLLECTION = "policies"

  /**
   * Collection holding [[gr.grnet.aquarium.logic.events.PolicyEntry]].
   */
  final val POLICY_ENTRIES_COLLECTION = "policyEntries"

  /* TODO: Some of the following methods rely on JSON (de-)serialization).
  * A method based on proper object serialization would be much faster.
  */
  def dbObjectToResourceEvent(dbObject: DBObject): ResourceEvent = {
    ResourceEvent.fromJson(JSON.serialize(dbObject))
  }

  def dbObjectToUserState(dbObj: DBObject): UserState = {
    UserState.fromJson(JSON.serialize(dbObj))
  }

  def dbObjectToWalletEntry(dbObj: DBObject): WalletEntry = {
    WalletEntry.fromJson(JSON.serialize(dbObj))
  }

  def dbObjectToUserEvent(dbObj: DBObject): UserEvent = {
    UserEvent.fromJson(JSON.serialize(dbObj))
  }

  def dbObjectToPolicyEntry(dbObj: DBObject): PolicyEntry = {
    PolicyEntry.fromJson(JSON.serialize(dbObj))
  }

  def findById[A >: Null <: AquariumEvent](id: String, collection: DBCollection, deserializer: (DBObject) => A) : Maybe[A] = Maybe {
    val query = new BasicDBObject(ResourceJsonNames.id, id)
    val cursor = collection find query

    try {
      if(cursor.hasNext)
        deserializer apply cursor.next
      else
        null: A // will be transformed to NoVal by the Maybe polymorphic constructor
    } finally {
      cursor.close()
    }
  }

  def runQuery[A <: AquariumEvent](query: DBObject, collection: DBCollection, orderBy: DBObject = null)
                                  (deserializer: (DBObject) => A)
                                  (sortWith: Option[(A, A) => Boolean]): List[A] = {
    val cursor0 = collection find query
    val cursor = if(orderBy ne null) {
      cursor0 sort orderBy
    } else {
      cursor0
    } // I really know that docs say that it is the same cursor.

    if(!cursor.hasNext) {
      cursor.close()
      Nil
    } else {
      val buff = new ListBuffer[A]()

      while(cursor.hasNext) {
        buff += deserializer apply cursor.next
      }

      cursor.close()

      sortWith match {
        case Some(sorter) => buff.toList.sortWith(sorter)
        case None => buff.toList
      }
    }
  }

  def storeAquariumEvent[A <: AquariumEvent](event: A, collection: DBCollection) : Maybe[RecordID] = {
    storeAny[A](event, collection, ResourceJsonNames.id, (e) => e.id, MongoDBStore.jsonSupportToDBObject)
  }

  def storeUserState(userState: UserState, collection: DBCollection): Maybe[RecordID] = {
    storeAny[UserState](userState, collection, ResourceJsonNames.userId, _.userId, MongoDBStore.jsonSupportToDBObject)
  }
  
  def storePolicyEntry(policyEntry: PolicyEntry, collection: DBCollection): Maybe[RecordID] = {
    storeAny[PolicyEntry](policyEntry, collection, PolicyJsonNames.id, _.id, MongoDBStore.jsonSupportToDBObject)
  }

  def storeAny[A](any: A,
                  collection: DBCollection,
                  idName: String,
                  idValueProvider: (A) => String,
                  serializer: (A) => DBObject) : Maybe[RecordID] = {
    import com.ckkloverdos.maybe.effect

    Maybe {
      val dbObj = serializer apply any
      val writeResult = collection insert dbObj
      writeResult.getLastError().throwOnError()

      // Get back to retrieve unique id
      val cursor = collection.find(new BasicDBObject(idName, idValueProvider(any)))
      cursor
    } flatMap { cursor ⇒
      effect {
        if(cursor.hasNext)
          RecordID(cursor.next().get(JsonNames._id).toString)
        else
          throw new StoreException("Could not store %s to %s".format(any, collection))
      } {} { cursor.close() }
    }
  }

  def jsonSupportToDBObject(any: JsonSupport): DBObject = {
    JSON.parse(any.toJson) match {
      case dbObject: DBObject ⇒
        dbObject
      case _ ⇒
        throw new StoreException("Could not transform %s -> %s".format(displayableObjectInfo(any), classOf[DBObject].getName))
    }
  }
}
