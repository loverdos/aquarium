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
import com.mongodb._
import com.ckkloverdos.maybe.{Failed, Just, Maybe}

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
  extends ResourceEventStore with UserStore with WalletStore with Loggable {

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

  private[this] def _makeDBObject(any: JsonSupport): DBObject = {
    JSON.parse(any.toJson) match {
      case dbObject: DBObject ⇒
        dbObject
      case _ ⇒
        throw new StoreException("Could not transform %s -> %s".format(displayableObjectInfo(any), classOf[DBObject].getName))
    }
  }

  private[this] def _insertObject(collection: DBCollection, obj: JsonSupport): DBObject = {
    val dbObj = _makeDBObject(obj)
    collection insert dbObj
    dbObj
  }

  private[this] def _checkWasInserted(collection: DBCollection, obj: JsonSupport,  idName: String, id: String): String = {
    val cursor = collection.find(new BasicDBObject(idName, id))
    if (!cursor.hasNext) {
      val errMsg = "Failed to _store %s".format(displayableObjectInfo(obj))
      logger.error(errMsg)
      throw new StoreException(errMsg)
    }

    val retval = cursor.next.get("_id").toString
    cursor.close()
    retval
  }

  private[this] def _store[A <: AquariumEvent](entry: A, col: DBCollection) : Maybe[RecordID] = {
    try {
      // Store
      val dbObj = _makeDBObject(entry)
      col.insert(dbObj)

      // Get back to retrieve unique id
      val cursor = col.find(new BasicDBObject(JsonNames.id, entry.id))

      if (!cursor.hasNext) {
        cursor.close()
        logger.error("Failed to _store entry: %s".format(entry))
        return Failed(new StoreException("Failed to _store entry: %s".format(entry)))
      }

      val retval = Just(RecordID(cursor.next.get(JsonNames._id).toString))
      cursor.close()
      retval
    } catch {
      case m: MongoException =>
        logger.error("Unknown Mongo error: %s".format(m)); Failed(m)
    }
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
  def storeResourceEvent(event: ResourceEvent): Maybe[RecordID] = _store(event, rcevents)

  def findResourceEventById(id: String): Maybe[ResourceEvent] = MongoDBStore.findById(id, rcevents, MongoDBStore.dbObjectToResourceEvent)

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

  //+UserStore
  def storeUserState(userState: UserState): Maybe[RecordID] = {
    Maybe {
      val dbObj = _insertObject(users, userState)
      val id    = _checkWasInserted(users, userState, JsonNames.userId, userState.userId)
      RecordID(id)
    }
  }

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
  //-UserStore

  //+WalletStore
  def storeWalletEntry(entry: WalletEntry): Maybe[RecordID] = _store(entry, wallets)

  def findWalletEntryById(id: String): Maybe[WalletEntry] = MongoDBStore.findById[WalletEntry](id, wallets, MongoDBStore.dbObjectToWalletEntry)

  def findUserWalletEntries(userId: String) = findUserWalletEntriesFromTo(userId, new Date(0), new Date(Int.MaxValue))

  def findUserWalletEntriesFromTo(userId: String, from: Date, to: Date) : List[WalletEntry] = {
    val q = new BasicDBObject()
    // TODO: Is this the correct way for an AND query?
    q.put(JsonNames.timestamp, new BasicDBObject("$gt", from.getTime))
    q.put(JsonNames.timestamp, new BasicDBObject("$lt", to.getTime))
    q.put(JsonNames.userId, userId)

    MongoDBStore.runQuery[WalletEntry](q, wallets)(MongoDBStore.dbObjectToWalletEntry)(Some(_sortByTimestampAsc))
  }
  //-WalletStore
}

object MongoDBStore {
  def RESOURCE_EVENTS_COLLECTION = "rcevents"
  def USERS_COLLECTION = "users"
  def IM_EVENTS_COLLECTION = "imevents"
  def IM_WALLETS = "wallets"

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

  def runQuery[A <: AquariumEvent](query: BasicDBObject, collection: DBCollection)
                                  (deserializer: (DBObject) => A)
                                  (sortWith: Option[(A, A) => Boolean]): List[A] = {
    val cur = collection find query
    if(!cur.hasNext) {
      cur.close()
      Nil
    } else {
      val buff = new ListBuffer[A]()

      while(cur.hasNext) {
        buff += deserializer apply cur.next
      }

      cur.close()

      sortWith match {
        case Some(sorter) => buff.toList.sortWith(sorter)
        case None => buff.toList
      }
    }
  }
}
