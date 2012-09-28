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

package gr.grnet.aquarium.store.mongodb

import collection.immutable
import com.mongodb._
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.converter.StdConverters
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.message.MessageConstants
import gr.grnet.aquarium.message.avro.gen.{UserAgreementHistoryMsg, UserStateMsg, IMEventMsg, ResourceEventMsg, PolicyMsg}
import gr.grnet.aquarium.message.avro.{MessageHelpers, MessageFactory, OrderingHelpers, AvroHelpers}
import gr.grnet.aquarium.store._
import gr.grnet.aquarium.util._
import gr.grnet.aquarium.util.json.JsonSupport
import gr.grnet.aquarium.{Aquarium, AquariumException}
import org.apache.avro.specific.SpecificRecord
import org.bson.types.ObjectId

/**
 * Mongodb implementation of the various aquarium stores.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Prodromos Gerakios <pgerakio@grnet.gr>
 */
class MongoDBStore(
    val aquarium: Aquarium,
    val mongo: Mongo,
    val database: String,
    val username: String,
    val password: String)
  extends ResourceEventStore
  with UserStateStore
  with IMEventStore
  with PolicyStore
  with Loggable {

  private[store] lazy val resourceEvents = getCollection(MongoDBStore.ResourceEventCollection)
  private[store] lazy val userStates = getCollection(MongoDBStore.UserStateCollection)
  private[store] lazy val imEvents = getCollection(MongoDBStore.IMEventCollection)
  private[store] lazy val policies = getCollection(MongoDBStore.PolicyCollection)

  private[this] def doAuthenticate(db: DB) {
    if(!db.isAuthenticated && !db.authenticate(username, password.toCharArray)) {
      throw new AquariumException("Could not authenticate user %s".format(username))
    }
  }

  private[this] def getCollection(name: String): DBCollection = {
    val db = mongo.getDB(database)
    doAuthenticate(db)
    db.getCollection(name)
  }

  //+ResourceEventStore
  def pingResourceEventStore(): Unit = synchronized {
    getCollection(MongoDBStore.ResourceEventCollection)
    MongoDBStore.ping(mongo)
  }

  def insertResourceEvent(event: ResourceEventMsg) = {
    val mongoID = new ObjectId()
    event.setInStoreID(mongoID.toStringMongod)

    val dbObject = new BasicDBObjectBuilder().
      add(MongoDBStore.JsonNames._id, mongoID).
      add(MongoDBStore.JsonNames.payload, AvroHelpers.bytesOfSpecificRecord(event)).
      add(MongoDBStore.JsonNames.userID, event.getUserID).
      add(MongoDBStore.JsonNames.occurredMillis, event.getOccurredMillis).
      add(MongoDBStore.JsonNames.receivedMillis, event.getReceivedMillis).
    get()

    MongoDBStore.insertDBObject(dbObject, resourceEvents)
    event
  }

  def findResourceEventByID(id: String): Option[ResourceEventMsg] = {
    val dbObjectOpt = MongoDBStore.findOneByAttribute(resourceEvents, MongoDBStore.JsonNames.id, id)
    for {
      dbObject ← dbObjectOpt
      payload = dbObject.get(MongoDBStore.JsonNames.payload)
      msg = AvroHelpers.specificRecordOfBytes(payload.asInstanceOf[Array[Byte]], new ResourceEventMsg)
    } yield msg
  }

  def countOutOfSyncResourceEventsForBillingPeriod(userID: String, startMillis: Long, stopMillis: Long): Long = {
    val query = new BasicDBObjectBuilder().
      add(MongoDBStore.JsonNames.userID, userID).
      // received within the period
      add(MongoDBStore.JsonNames.receivedMillis, new BasicDBObject("$gte", startMillis)).
      add(MongoDBStore.JsonNames.receivedMillis, new BasicDBObject("$lte", stopMillis)).
      // occurred outside the period
      add("$or", {
        val dbList = new BasicDBList()
        dbList.add(0, new BasicDBObject(MongoDBStore.JsonNames.occurredMillis, new BasicDBObject("$lt", startMillis)))
        dbList.add(1, new BasicDBObject(MongoDBStore.JsonNames.occurredMillis, new BasicDBObject("$gt", stopMillis)))
        dbList
      }).
      get()

    resourceEvents.count(query)
  }

  def foreachResourceEventOccurredInPeriod(
      userID: String,
      startMillis: Long,
      stopMillis: Long
  )(f: ResourceEventMsg ⇒ Unit): Unit = {

    val query = new BasicDBObjectBuilder().
      add(MongoDBStore.JsonNames.userID, userID).
      add(MongoDBStore.JsonNames.occurredMillis, new BasicDBObject("$gte", startMillis)).
      add(MongoDBStore.JsonNames.occurredMillis, new BasicDBObject("$lte", stopMillis)).
      get()

    val sorter = new BasicDBObject(MongoDBStore.JsonNames.occurredMillis, 1)
    val cursor = resourceEvents.find(query).sort(sorter)

    withCloseable(cursor) { cursor ⇒
      while(cursor.hasNext) {
        val nextDBObject = cursor.next()
        val payload = nextDBObject.get(MongoDBStore.JsonNames.payload).asInstanceOf[Array[Byte]]
        val nextEvent = AvroHelpers.specificRecordOfBytes(payload, new ResourceEventMsg)

        f(nextEvent)
      }
    }
  }
  //-ResourceEventStore

  //+ UserStateStore
  def findUserStateByUserID(userID: String) = {
    val dbObjectOpt = MongoDBStore.findOneByAttribute(userStates, MongoDBStore.JsonNames.userID, userID)
    for {
      dbObject <- dbObjectOpt
      payload = dbObject.get(MongoDBStore.JsonNames.payload).asInstanceOf[Array[Byte]]
      msg = AvroHelpers.specificRecordOfBytes(payload, new UserStateMsg)
    } yield {
      msg
    }
  }

  def findLatestUserStateForFullMonthBilling(userID: String, bmi: BillingMonthInfo) = {
    val query = new BasicDBObjectBuilder().
      add(MongoDBStore.JsonNames.userID, userID).
      add(MongoDBStore.JsonNames.isForFullMonth, true).
      add(MongoDBStore.JsonNames.billingYear, bmi.year).
      add(MongoDBStore.JsonNames.billingMonth, bmi.month).
      get()

    // Descending order, so that the latest comes first
    val sorter = new BasicDBObject(MongoDBStore.JsonNames.occurredMillis, -1)

    val cursor = userStates.find(query).sort(sorter)

    withCloseable(cursor) { cursor ⇒
      MongoDBStore.findNextPayloadRecord(cursor, new UserStateMsg)
    }
  }

  def findLatestUserState(userID: String) = {
    val query = new BasicDBObjectBuilder().
      add(MongoDBStore.JsonNames.userID, userID).
      get()

    // Descending order, so that the latest comes first
    val sorter = new BasicDBObject(MongoDBStore.JsonNames.occurredMillis, -1)

    val cursor = userStates.find(query).sort(sorter)

    withCloseable(cursor) { cursor ⇒
      MongoDBStore.findNextPayloadRecord(cursor, new UserStateMsg)
    }
  }

  /**
   * Stores a user state.
   */
  def insertUserState(event: UserStateMsg)= {
    val mongoID = new ObjectId()
    event.setInStoreID(mongoID.toStringMongod)

    val dbObject = new BasicDBObjectBuilder().
      add(MongoDBStore.JsonNames._id, mongoID).
      add(MongoDBStore.JsonNames.payload, AvroHelpers.bytesOfSpecificRecord(event)).
      add(MongoDBStore.JsonNames.userID, event.getUserID).
      add(MongoDBStore.JsonNames.occurredMillis, event.getOccurredMillis).
      add(MongoDBStore.JsonNames.isForFullMonth, event.getIsForFullMonth).
      add(MongoDBStore.JsonNames.billingYear, event.getBillingYear).
      add(MongoDBStore.JsonNames.billingMonth, event.getBillingMonth).
      add(MongoDBStore.JsonNames.billingMonthDay, event.getBillingMonthDay).
    get()

    MongoDBStore.insertDBObject(dbObject, userStates)
    event
  }
  //- UserStateStore

  //+IMEventStore
  def pingIMEventStore(): Unit = {
    getCollection(MongoDBStore.IMEventCollection)
    MongoDBStore.ping(mongo)
  }

  def insertIMEvent(event: IMEventMsg) = {
    val mongoID = new ObjectId()
    event.setInStoreID(mongoID.toStringMongod)

    val dbObject = new BasicDBObjectBuilder().
      add(MongoDBStore.JsonNames._id, mongoID).
      add(MongoDBStore.JsonNames.payload, AvroHelpers.bytesOfSpecificRecord(event)).
      add(MongoDBStore.JsonNames.userID, event.getUserID).
      add(MongoDBStore.JsonNames.eventType, event.getEventType().toLowerCase).
      add(MongoDBStore.JsonNames.occurredMillis, event.getOccurredMillis).
      add(MongoDBStore.JsonNames.receivedMillis, event.getReceivedMillis).
    get()

    MongoDBStore.insertDBObject(dbObject, imEvents)
    event
  }

  def findIMEventByID(id: String) = {
    val dbObjectOpt = MongoDBStore.findOneByAttribute(imEvents, MongoDBStore.JsonNames.id, id)
    for {
      dbObject ← dbObjectOpt
      payload = dbObject.get(MongoDBStore.JsonNames.payload).asInstanceOf[Array[Byte]]
      msg = AvroHelpers.specificRecordOfBytes(payload, new IMEventMsg)
    } yield {
      msg
    }
  }


  /**
   * Find the `CREATE` even for the given user. Note that there must be only one such event.
   */
  def findCreateIMEventByUserID(userID: String) = {
    val query = new BasicDBObjectBuilder().
      add(MongoDBStore.JsonNames.userID, userID).
      add(MongoDBStore.JsonNames.eventType, MessageConstants.IMEventMsg.EventTypes.create).get()

    // Normally one such event is allowed ...
    val cursor = imEvents.find(query).sort(new BasicDBObject(MongoDBStore.JsonNames.occurredMillis, 1))

    val dbObjectOpt = withCloseable(cursor) { cursor ⇒
      if(cursor.hasNext) {
        Some(cursor.next())
      } else {
        None
      }
    }

    for {
      dbObject <- dbObjectOpt
      payload = dbObject.get(MongoDBStore.JsonNames.payload).asInstanceOf[Array[Byte]]
      msg = AvroHelpers.specificRecordOfBytes(payload, new IMEventMsg)
    } yield {
      msg
    }
  }

  /**
   * Scans events for the given user, sorted by `occurredMillis` in ascending order and runs them through
   * the given function `f`.
   *
   * Any exception is propagated to the caller. The underlying DB resources are properly disposed in any case.
   */
  def foreachIMEventInOccurrenceOrder(userID: String)(f: (IMEventMsg) ⇒ Boolean) = {
    val query = new BasicDBObject(MongoDBStore.JsonNames.userID, userID)
    val cursor = imEvents.find(query).sort(new BasicDBObject(MongoDBStore.JsonNames.occurredMillis, 1))

    var _shouldContinue = true
    withCloseable(cursor) { cursor ⇒
      while(_shouldContinue && cursor.hasNext) {
        val dbObject = cursor.next()
        val payload = dbObject.get(MongoDBStore.JsonNames.payload).asInstanceOf[Array[Byte]]
        val msg = AvroHelpers.specificRecordOfBytes(payload, new IMEventMsg)

        _shouldContinue = f(msg)
      }
    }

    _shouldContinue
  }
  //-IMEventStore

  //+PolicyStore
  def foreachPolicy[U](f: PolicyMsg ⇒ U) {
    val cursor = policies.find()
    withCloseable(cursor) { cursor ⇒
      while(cursor.hasNext) {
        val dbObject = cursor.next()
        val payload = dbObject.get(MongoDBStore.JsonNames.payload).asInstanceOf[Array[Byte]]
        val policy = AvroHelpers.specificRecordOfBytes(payload, new PolicyMsg)
        f(policy)
      }
    }
  }

  def insertPolicy(policy: PolicyMsg): PolicyMsg = {
    val mongoID = new ObjectId()
    policy.setInStoreID(mongoID.toStringMongod)
    val dbObject = new BasicDBObjectBuilder().
      add(MongoDBStore.JsonNames._id, mongoID).
      add(MongoDBStore.JsonNames.validFromMillis, policy.getValidFromMillis).
      add(MongoDBStore.JsonNames.validToMillis, policy.getValidToMillis).
      add(MongoDBStore.JsonNames.payload, AvroHelpers.bytesOfSpecificRecord(policy)).
    get()

    MongoDBStore.insertDBObject(dbObject, policies)
    policy
  }

  def loadPolicyAt(atMillis: Long): Option[PolicyMsg] = {
    // FIXME Inefficient
    var _policies = immutable.TreeSet[PolicyMsg]()(OrderingHelpers.DefaultPolicyMsgOrdering)
    foreachPolicy(_policies += _)
    _policies.to(MessageFactory.newDummyPolicyMsgAt(atMillis)).lastOption
  }

  def loadSortedPoliciesWithin(fromMillis: Long, toMillis: Long): immutable.SortedMap[Timeslot, PolicyMsg] = {
    // FIXME Inefficient
    var _policies = immutable.TreeSet[PolicyMsg]()(OrderingHelpers.DefaultPolicyMsgOrdering)
    foreachPolicy(_policies += _)

    immutable.SortedMap(_policies.
      from(MessageFactory.newDummyPolicyMsgAt(fromMillis)).
      to(MessageFactory.newDummyPolicyMsgAt(toMillis)).toSeq.
      map(p ⇒ (Timeslot(p.getValidFromMillis, p.getValidToMillis), p)): _*
    )
  }
  //-PolicyStore
}

object MongoDBStore {
  final val JsonNames = gr.grnet.aquarium.util.json.JsonNames

  final val ResourceEventCollection = "resevents"

  final val UserStateCollection = "userstates"

  final val UserAgreementHistoryCollection = "useragreementhistory"

  final val IMEventCollection = "imevents"

  final val PolicyCollection = "policies"

  def firstResultIfExists[A](cursor: DBCursor, f: DBObject ⇒ A): Option[A] = {
    withCloseable(cursor) { cursor ⇒
      if(cursor.hasNext) {
        Some(f(cursor.next()))
      } else {
        None
      }
    }
  }

  def ping(mongo: Mongo): Unit = synchronized {
    // This requires a network roundtrip
    mongo.isLocked
  }

  def findOneByAttribute(
      collection: DBCollection,
      attributeName: String,
      attributeValue: String,
      sortByOpt: Option[DBObject] = None
  ): Option[DBObject] =  {
    val query = new BasicDBObject(attributeName, attributeValue)
    val cursor = sortByOpt match {
      case None         ⇒ collection find query
      case Some(sortBy) ⇒ collection find query sort sortBy
    }
    withCloseable(cursor) { cursor ⇒
      if(cursor.hasNext) Some(cursor.next()) else None
    }
  }

  def insertDBObject(dbObj: DBObject, collection: DBCollection) {
    collection.insert(dbObj, WriteConcern.JOURNAL_SAFE)
  }

  def findNextPayloadRecord[R <: SpecificRecord](cursor: DBCursor, fresh: R): Option[R] = {
    for {
      dbObject <- if(cursor.hasNext) Some(cursor.next()) else None
      payload = dbObject.get(MongoDBStore.JsonNames.payload).asInstanceOf[Array[Byte]]
      msg = AvroHelpers.specificRecordOfBytes(payload, fresh)
    } yield {
      msg
    }
  }

  def jsonSupportToDBObject(jsonSupport: JsonSupport) = {
    StdConverters.AllConverters.convertEx[DBObject](jsonSupport)
  }
}
