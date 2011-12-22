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
import com.ckkloverdos.maybe.{Failed, Just, Maybe}
import com.mongodb.util.JSON
import gr.grnet.aquarium.user.UserState
import gr.grnet.aquarium.user.UserState.JsonNames
import gr.grnet.aquarium.util.displayableObjectInfo
import gr.grnet.aquarium.util.json.JsonSupport
import collection.mutable.{ListBuffer}
import gr.grnet.aquarium.store._
import gr.grnet.aquarium.logic.events.{WalletEntry, UserEvent, ResourceEvent, AquariumEvent}
import java.util.Date
import com.mongodb._

/**
 * Mongodb implementation of the event _store (and soon the user _store).
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class MongoDBStore(
    val mongo: Mongo,
    val database: String,
    val username: String,
    val password: String)
  extends EventStore with UserStore with WalletStore with Loggable {

  private[store] lazy val events: DBCollection = getCollection(MongoDBStore.EVENTS_COLLECTION)
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

  private[this] def _deserializeEvent[A <: AquariumEvent](a: DBObject): A = {
    //TODO: Distinguish events and deserialize appropriately
    ResourceEvent.fromJson(JSON.serialize(a)).asInstanceOf[A]
  }

  private[this] def _deserializeUserState(dbObj: DBObject): UserState = {
    val jsonString = JSON.serialize(dbObj)
    UserState.fromJson(jsonString)
  }

  private[this] def _makeDBObject(any: JsonSupport): DBObject = {
    JSON.parse(any.toJson) match {
      case dbObject: DBObject ⇒
        dbObject
      case _ ⇒
        throw new StoreException("Could not transform %s -> %s".format(displayableObjectInfo(any), classOf[DBObject].getName))
    }
  }

  private[this] def _prepareFieldQuery(name: String, value: String): DBObject = {
    val dbObj = new BasicDBObject(1)
    dbObj.put(name, value)
    dbObj
  }

  private[this] def _insertObject(collection: DBCollection, obj: JsonSupport): DBObject = {
    val dbObj = _makeDBObject(obj)
    collection insert dbObj
    dbObj
  }

  private[this] def _checkWasInserted(collection: DBCollection, obj: JsonSupport,  idName: String, id: String): String = {
    val cursor = collection.find(_prepareFieldQuery(idName, id))
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
      val cursor = col.find(_prepareFieldQuery("id", entry.id))

      if (!cursor.hasNext) {
        logger.error("Failed to _store entry: %s".format(entry))
        return Failed(new StoreException("Failed to _store entry: %s".format(entry)))
      }

      Just(RecordID(cursor.next.get("_id").toString))
    } catch {
      case m: MongoException =>
        logger.error("Unknown Mongo error: %s".format(m)); Failed(m)
    }
  }

  private[this] def _findById[A <: AquariumEvent](id: String, col: DBCollection) : Option[A] = {
    val q = new BasicDBObject()
    q.put("id", id)

    val cur = col.find(q)

    if (cur.hasNext)
      Some(_deserializeEvent(cur.next))
    else
      None
  }
  
  private[this] def _query[A <: AquariumEvent](q: BasicDBObject,
                                              col: DBCollection)
                                              (sortWith: Option[(A, A) => Boolean]): List[A] = {
    val cur = col.find(q)
    if (!cur.hasNext)
      return List()

    val buff = new ListBuffer[A]()

    while(cur.hasNext)
      buff += _deserializeEvent(cur.next)

    sortWith match {
      case Some(sorter) => buff.toList.sortWith(sorter)
      case None => buff.toList
    }
  }

  private[this] def _sortByTimestampAsc[A <: AquariumEvent](one: A, two: A): Boolean = {
    if (one.timestamp > two.timestamp) false
    else if (one.timestamp < two.timestamp) true
    else true
  }

  private[this] def _sortByTimestampDesc[A <: AquariumEvent](one: A, two: A): Boolean = {
    if (one.timestamp < two.timestamp) false
    else if (one.timestamp > two.timestamp) true
    else true
  }

  //+EventStore
  def storeEvent[A <: AquariumEvent](event: A): Maybe[RecordID] = _store(event, events)

  def findEventById[A <: AquariumEvent](id: String): Option[A] = _findById[A](id, events)

  def findEventsByUserId[A <: AquariumEvent](userId: String)
                                            (sortWith: Option[(A, A) => Boolean]): List[A] = {
    val q = new BasicDBObject()
    q.put("userId", userId)

    _query(q, events)(sortWith)
  }
  //-EventStore

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
      val queryObj = _prepareFieldQuery(JsonNames.userId, userId)
      val cursor = events.find(queryObj)

      if(!cursor.hasNext) {
        cursor.close()
        null
      } else {
        val userState = _deserializeUserState(cursor.next())
        cursor.close()
        userState
      }
    }
  }
  //-UserStore

  //+WalletStore
  def store(entry: WalletEntry): Maybe[RecordID] = _store(entry, wallets)

  def findEntryById(id: String): Option[WalletEntry] = _findById[WalletEntry](id, wallets)

  def findAllUserEntries(userId: String) = findUserEntriesFromTo(userId, new Date(0), new Date(Int.MaxValue))

  def findUserEntriesFromTo(userId: String, from: Date, to: Date) : List[WalletEntry] = {
    val q = new BasicDBObject()
    q.put("timestamp", new BasicDBObject("$gt", from.getTime))
    q.put("timestamp", new BasicDBObject("$lt", to.getTime))
    q.put("userId", userId)

    _query[WalletEntry](q, wallets)(Some(_sortByTimestampAsc))
  }
  //-WalletStore
}

object MongoDBStore {
  def EVENTS_COLLECTION = "events"
  def USERS_COLLECTION = "users"
  def IM_EVENTS_COLLECTION = "imevents"
  def IM_WALLETS = "wallets"
}
