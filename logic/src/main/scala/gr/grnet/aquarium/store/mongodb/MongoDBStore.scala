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
import gr.grnet.aquarium.store.{RecordID, StoreException, EventStore}
import com.ckkloverdos.maybe.{Failed, Just, Maybe}
import gr.grnet.aquarium.logic.events.{ResourceEvent, AquariumEvent}
import com.mongodb.util.JSON
import com.mongodb._
import collection.mutable.ListBuffer
import com.ckkloverdos.props.Props
import gr.grnet.aquarium.{MasterConf, Configurable}

/**
 * Mongodb implementation of the event store (and soon the user store).
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class MongoDBStore(val mongo: Mongo, val database: String,
                   val username: String, val password: String) extends EventStore with Loggable {
  private[store] lazy val events: DBCollection = getCollection(MongoDBStore.EVENTS_COLLECTION)
  private[store] lazy val users: DBCollection = getCollection(MongoDBStore.USERS_COLLECTION)

  def getCollection(name: String): DBCollection = {
    val db = mongo.getDB(database)
    if(!db.authenticate(username, password.toCharArray)) {
      throw new StoreException("Could not authenticate user %s".format(username))
    }
    db.getCollection(name)
  }

  /* TODO: Some of the following methods rely on JSON (de-)serialization).
   * A method based on proper object serialization would be much faster.
   */

  //EventStore methods
  def storeEvent[A <: AquariumEvent](event: A): Maybe[RecordID] = {
    try {
      // Store
      val obj = JSON.parse(event.toJson).asInstanceOf[DBObject]
      events.insert(obj)

      // TODO: Make this retrieval a configurable option
      // Get back to retrieve unique id
      val q = new BasicDBObject()
      q.put("id", event.id)

      val cur = events.find(q)

      if (!cur.hasNext) {
        logger.error("Failed to store event: %s".format(event))
        Failed(new StoreException("Failed to store event: %s".format(event)))
      }

      Just(RecordID(cur.next.get("_id").toString))
    } catch {
      case m: MongoException =>
        logger.error("Unknown Mongo error: %s".format(m)); Failed(m)
    }
  }

  def findEventById[A <: AquariumEvent](id: String): Option[A] = {
    val q = new BasicDBObject()
    q.put("id", id)

    val cur = events.find(q)

    if (cur.hasNext)
      Some(deserialize(cur.next))
    else
      None
  }

  def findEventsByUserId[A <: AquariumEvent](userId: Long)
                                            (sortWith: Option[(A, A) => Boolean]): List[A] = {
    val q = new BasicDBObject()
    q.put("userId", userId)

    val cur = events.find(q)

    if (!cur.hasNext)
      return List()

    val buff = new ListBuffer[A]()

    while(cur.hasNext)
      buff += deserialize(cur.next)

    sortWith match {
      case Some(sorter) => buff.toList.sortWith(sorter)
      case None => buff.toList
    }
  }

  private def deserialize[A <: AquariumEvent](a: DBObject): A = {
    //TODO: Distinguish events and deserialize appropriately
    ResourceEvent.fromJson(JSON.serialize(a)).asInstanceOf[A]
  }
}

object MongoDBStore {
  def EVENTS_COLLECTION = "events"
  def USERS_COLLECTION = "users"
}