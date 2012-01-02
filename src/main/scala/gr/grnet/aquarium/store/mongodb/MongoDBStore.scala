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
import gr.grnet.aquarium.util.displayableObjectInfo
import gr.grnet.aquarium.util.json.JsonSupport
import collection.mutable.ListBuffer
import gr.grnet.aquarium.store._
import gr.grnet.aquarium.logic.events.{WalletEntry, ResourceEvent, AquariumEvent}
import gr.grnet.aquarium.logic.events.ResourceEvent.JsonNames
import java.util.Date
import com.ckkloverdos.maybe.Maybe
import com.mongodb._

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
  extends ResourceEventStore with UserStateStore with WalletEntryStore with Loggable {

  private[store] lazy val rcevents: DBCollection = getCollection(MongoDBStore.RESOURCE_EVENTS_COLLECTION)
  private[store] lazy val users: DBCollection = getCollection(MongoDBStore.USERS_COLLECTION)
  private[store] lazy val imevents: DBCollection = getCollection(MongoDBStore.IM_EVENTS_COLLECTION)
  private[store] lazy val wallets: DBCollection = getCollection(MongoDBStore.IM_WALLETS)

  private[this] def getCollection(name: String): DBCollection = {
    val db = mongo.getDB(database)
    if(!db.authenticate(username, password.toCharArray)) {
      throw new StoreException("Could not authenticate user %s".format(username))
    }
    db.getCollection(name)
  }

  /* TODO: Some of the following methods rely on JSON (de-)serialization).
  * A method based on proper object serialization would be much faster.
  */

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
    MongoDBStore.storeAquariumEvent(event, rcevents)

  def findResourceEventById(id: String): Maybe[ResourceEvent] =
    MongoDBStore.findById(id, rcevents, MongoDBStore.dbObjectToResourceEvent)

  def findResourceEventsByUserId(userId: String)
                                (sortWith: Option[(ResourceEvent, ResourceEvent) => Boolean]): List[ResourceEvent] = {
    val query = new BasicDBObject(JsonNames.userId, userId)

    MongoDBStore.runQuery(query, rcevents)(MongoDBStore.dbObjectToResourceEvent)(sortWith)
  }

  def findResourceEventsByUserIdAfterTimestamp(userId: String, timestamp: Long): List[ResourceEvent] = {
    val query = new BasicDBObject()
    query.put(JsonNames.userId, userId)
    query.put(JsonNames.timestamp, new BasicDBObject("$gte", timestamp))
    
    val sort = new BasicDBObject(JsonNames.timestamp, 1)

    val cursor = rcevents.find(query).sort(sort)

    try {
      val buffer = new scala.collection.mutable.ListBuffer[ResourceEvent]
      while(cursor.hasNext) {
        buffer += MongoDBStore.dbObjectToResourceEvent(cursor.next())
      }
      buffer.toList
    } finally {
      cursor.close()
    }
  }
  //-ResourceEventStore

  //+UserStateStore
  def storeUserState(userState: UserState): Maybe[RecordID] =
    MongoDBStore.storeUserState(userState, users)

  def findUserStateByUserId(userId: String): Maybe[UserState] = {
    Maybe {
      val query = new BasicDBObject(JsonNames.userId, userId)
      val cursor = rcevents find query

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
  //-UserStateStore

  //+WalletEntryStore
  def storeWalletEntry(entry: WalletEntry): Maybe[RecordID] =
    MongoDBStore.storeAquariumEvent(entry, wallets)

  def findWalletEntryById(id: String): Maybe[WalletEntry] =
    MongoDBStore.findById[WalletEntry](id, wallets, MongoDBStore.dbObjectToWalletEntry)

  def findUserWalletEntries(userId: String) = {
    // TODO: optimize
    findUserWalletEntriesFromTo(userId, new Date(0), new Date(Int.MaxValue))
  }

  def findUserWalletEntriesFromTo(userId: String, from: Date, to: Date) : List[WalletEntry] = {
    val q = new BasicDBObject()
    // TODO: Is this the correct way for an AND query?
    q.put(JsonNames.timestamp, new BasicDBObject("$gt", from.getTime))
    q.put(JsonNames.timestamp, new BasicDBObject("$lt", to.getTime))
    q.put(JsonNames.userId, userId)

    MongoDBStore.runQuery[WalletEntry](q, wallets)(MongoDBStore.dbObjectToWalletEntry)(Some(_sortByTimestampAsc))
  }


  //-WalletEntryStore
  def findLatestUserWalletEntries(userId: String) = {
    Maybe {
      val orderBy = new BasicDBObject(JsonNames.occurredMillis, -1) // -1 is descending order
      val cursor = wallets.find().sort(orderBy)

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
}

object MongoDBStore {
  final val RESOURCE_EVENTS_COLLECTION = "resevents"
  final val PROCESSED_RESOURCE_EVENTS_COLLECTION = "procresevents"
  final val USERS_COLLECTION = "users"
  final val IM_EVENTS_COLLECTION = "imevents"
  final val IM_WALLETS = "wallets"

  def dbObjectToResourceEvent(dbObject: DBObject): ResourceEvent = {
    ResourceEvent.fromJson(JSON.serialize(dbObject))
  }

  def dbObjectToUserState(dbObj: DBObject): UserState = {
    UserState.fromJson(JSON.serialize(dbObj))
  }

  def dbObjectToWalletEntry(dbObj: DBObject): WalletEntry = {
    WalletEntry.fromJson(JSON.serialize(dbObj))
  }

  def findById[A >: Null <: AquariumEvent](id: String, collection: DBCollection, deserializer: (DBObject) => A) : Maybe[A] = Maybe {
    val query = new BasicDBObject(JsonNames.id, id)
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
    storeAny[A](event, collection, JsonNames.id, (e) => e.id, MongoDBStore.jsonSupportToDBObject)
  }

  def storeUserState(userState: UserState, collection: DBCollection): Maybe[RecordID] = {
    storeAny[UserState](userState, collection, JsonNames.userId, _.userId, MongoDBStore.jsonSupportToDBObject)
  }

  def storeAny[A](any: A,
                  collection: DBCollection,
                  idName: String,
                  idValueProvider: (A) => String,
                  serializer: (A) => DBObject) : Maybe[RecordID] = Maybe {
    // Store
    val dbObj = serializer apply any
    val writeResult = collection insert dbObj
    writeResult.getLastError().throwOnError()

    // Get back to retrieve unique id
    val cursor = collection.find(new BasicDBObject(idName, idValueProvider(any)))

    try {
      // TODO: better way to get _id?
      if(cursor.hasNext)
        RecordID(cursor.next().get(JsonNames._id).toString)
      else
        throw new StoreException("Could not store %s to %s".format(any, collection))
    } finally {
      cursor.close()
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
