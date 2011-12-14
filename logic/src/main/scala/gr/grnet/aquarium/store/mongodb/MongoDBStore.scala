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
import gr.grnet.aquarium.logic.events.{ResourceEvent, AquariumEvent}
import com.ckkloverdos.maybe.{Failed, Just, Maybe}
import com.mongodb.{MongoException, MongoOptions, ServerAddress}
import com.mongodb.casbah.Imports._

/**
 * Mongodb implementation of the message store.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class MongoDBStore(host: String, port: String,
                   username: String, passwd: String,
                   database: String)
  extends EventStore with Loggable {


  lazy val mongo: Option[MongoConnection] = {
    try {
      val addr = new ServerAddress(host, port.toInt)
      val opt = new MongoOptions()
      Some(MongoConnection(addr, opt))
    } catch {
      case e: MongoException =>
        logger.error(("Cannot connect to mongo at %s:%s (uname=%s). " +
          "Cause:").format(host, port, username, e))
        None
      case nfe: NumberFormatException =>
        logger.error("%s is not a valid port number".format(port))
        None
    }
  }

  private[store] lazy val events: MongoCollection = getCollection("events")

  private[store] lazy val users: MongoCollection = getCollection("user")

  private[store]def getCollection(name: String): MongoCollection = {
    mongo match {
      case Some(x) =>
        val db = x.getDB(database)
        if(!db.authenticate(username, passwd))
          throw new StoreException("Could not authenticate user %s".format(username))
        db(name)
      case None => throw new StoreException("No connection to Mongo")
    }
  }

  /* TODO: Some of the following methods rely on JSON (de-)serialization).
   * A method based on proper object serialization would be much faster.
   */

  //EventStore methods
  def storeEvent[A <: AquariumEvent](event: A): Maybe[RecordID] = {
    try {
      // Store
      events += event

      // TODO: Make this retrieval a configurable option
      // Get back to retrieve unique id
      val q = MongoDBObject("id" -> event.id)
      val cur = events.find(q)

      if (!cur.hasNext) {
        logger.error("Failed to store event: %s".format(event))
        return Failed(new StoreException("Failed to store event: %s".format(event)))
      }

      Just(RecordID(cur.next.get("id").toString))
    } catch {
      case m: MongoException =>
        logger.error("Unknown Mongo error: %s".format(m)); Failed(m)
    }
  }

  def findEventById[A <: AquariumEvent](id: String): Option[A] = {
    val a: Option[DBObject] = events.findOne(DBObject("id" -> id))
    a.map(x => x: MongoDBObject).map(x => x: A)
  }

  def findEventsByUserId[A <: AquariumEvent](userId: Long)
                                      (sortWith: Option[(A, A) => Boolean]): List[A] = {
    List()
  }

  implicit def toMongoDBObject[A <: AquariumEvent](a: A): DBObject = {

    val builder = MongoDBObject.newBuilder
    a.toMap.foreach{x => builder += x._1 -> x._2}

    builder.result()
  }

  implicit def toAqEventSubclass[A <: AquariumEvent](a: MongoDBObject): A = {
    //TODO: This must be amended when we have more AquariumEvent subclasses
    val id = a.getOrElse("id", "0").asInstanceOf[String]
    val userId = a.getOrElse("userId", 0).asInstanceOf[Long]
    val clientId = a.getOrElse("clientId", 0).asInstanceOf[Long]
    val resource = a.getOrElse("resource", "").asInstanceOf[String]
    val timestamp = a.getOrElse("timestamp", 0).asInstanceOf[Long]
    val eventVersion = a.getOrElse("eventVersion", 0).asInstanceOf[Short]
    val details = a.getOrElse("details", Map()).asInstanceOf[Map[String, String]]
    ResourceEvent(id,userId,clientId,resource,timestamp,eventVersion,details).asInstanceOf[A]
  }
}
