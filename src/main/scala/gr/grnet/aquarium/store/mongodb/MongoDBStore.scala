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

import com.mongodb.util.JSON
import gr.grnet.aquarium.computation.UserState.{JsonNames ⇒ UserStateJsonNames}
import gr.grnet.aquarium.util.json.JsonSupport
import collection.mutable.ListBuffer
import gr.grnet.aquarium.event.model.im.IMEventModel
import gr.grnet.aquarium.event.model.im.IMEventModel.{Names ⇒ IMEventNames}
import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import gr.grnet.aquarium.event.model.resource.ResourceEventModel.{Names ⇒ ResourceEventNames}
import gr.grnet.aquarium.store._
import gr.grnet.aquarium.event.model.PolicyEntry.{JsonNames ⇒ PolicyJsonNames}
import gr.grnet.aquarium.logic.accounting.Policy
import com.mongodb._
import org.bson.types.ObjectId
import com.ckkloverdos.maybe.Maybe
import gr.grnet.aquarium.util._
import gr.grnet.aquarium.converter.Conversions
import gr.grnet.aquarium.computation.UserState
import gr.grnet.aquarium.event.model.{ExternalEventModel, PolicyEntry}

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
  with IMEventStore
  with PolicyStore
  with Loggable {

  override type IMEvent = MongoDBIMEvent
  override type ResourceEvent = MongoDBResourceEvent

  private[store] lazy val resourceEvents   = getCollection(MongoDBStore.RESOURCE_EVENTS_COLLECTION)
  private[store] lazy val userStates       = getCollection(MongoDBStore.USER_STATES_COLLECTION)
  private[store] lazy val imEvents         = getCollection(MongoDBStore.IM_EVENTS_COLLECTION)
  private[store] lazy val unparsedIMEvents = getCollection(MongoDBStore.UNPARSED_IM_EVENTS_COLLECTION)
  private[store] lazy val policyEntries    = getCollection(MongoDBStore.POLICY_ENTRIES_COLLECTION)

  private[this] def getCollection(name: String): DBCollection = {
    val db = mongo.getDB(database)
    //logger.debug("Authenticating to mongo")
    if(!db.isAuthenticated && !db.authenticate(username, password.toCharArray)) {
      throw new StoreException("Could not authenticate user %s".format(username))
    }
    db.getCollection(name)
  }

  private[this] def _sortByTimestampAsc[A <: ExternalEventModel](one: A, two: A): Boolean = {
    if (one.occurredMillis > two.occurredMillis) false
    else if (one.occurredMillis < two.occurredMillis) true
    else true
  }

  //+ResourceEventStore
  def createResourceEventFromOther(event: ResourceEventModel): ResourceEvent = {
    MongoDBResourceEvent.fromOther(event, null)
  }

  def pingResourceEventStore(): Unit = synchronized {
    MongoDBStore.ping(mongo)
  }

  def insertResourceEvent(event: ResourceEventModel) = {
    val localEvent = MongoDBResourceEvent.fromOther(event, new ObjectId().toStringMongod)
    MongoDBStore.insertObject(localEvent, resourceEvents, MongoDBStore.jsonSupportToDBObject)
    localEvent
  }

  def findResourceEventById(id: String): Option[ResourceEvent] = {
    MongoDBStore.findBy(ResourceEventNames.id, id, resourceEvents, MongoDBResourceEvent.fromDBObject)
  }

  def findResourceEventsByUserId(userId: String)
                                (sortWith: Option[(ResourceEvent, ResourceEvent) => Boolean]): List[ResourceEvent] = {
    val query = new BasicDBObject(ResourceEventNames.userID, userId)

    MongoDBStore.runQuery(query, resourceEvents)(MongoDBResourceEvent.fromDBObject)(sortWith)
  }

  def findResourceEventsByUserIdAfterTimestamp(userId: String, timestamp: Long): List[ResourceEvent] = {
    val query = new BasicDBObject()
    query.put(ResourceEventNames.userID, userId)
    query.put(ResourceEventNames.occurredMillis, new BasicDBObject("$gt", timestamp))
    
    val sort = new BasicDBObject(ResourceEventNames.occurredMillis, 1)

    val cursor = resourceEvents.find(query).sort(sort)

    try {
      val buffer = new scala.collection.mutable.ListBuffer[ResourceEvent]
      while(cursor.hasNext) {
        buffer += MongoDBResourceEvent.fromDBObject(cursor.next())
      }
      buffer.toList.sortWith(_sortByTimestampAsc)
    } finally {
      cursor.close()
    }
  }

  def findResourceEventHistory(userId: String, resName: String,
                               instid: Option[String], upTo: Long) : List[ResourceEvent] = {
    val query = new BasicDBObject()
    query.put(ResourceEventNames.userID, userId)
    query.put(ResourceEventNames.occurredMillis, new BasicDBObject("$lt", upTo))
    query.put(ResourceEventNames.resource, resName)

    instid match {
      case Some(id) =>
        Policy.policy.findResource(resName) match {
          case Some(y) => query.put(ResourceEventNames.details,
            new BasicDBObject(y.descriminatorField, instid.get))
          case None =>
        }
      case None =>
    }

    val sort = new BasicDBObject(ResourceEventNames.occurredMillis, 1)
    val cursor = resourceEvents.find(query).sort(sort)

    try {
      val buffer = new scala.collection.mutable.ListBuffer[ResourceEvent]
      while(cursor.hasNext) {
        buffer += MongoDBResourceEvent.fromDBObject(cursor.next())
      }
      buffer.toList.sortWith(_sortByTimestampAsc)
    } finally {
      cursor.close()
    }
  }

  def findResourceEventsForReceivedPeriod(userId: String, startTimeMillis: Long, stopTimeMillis: Long): List[ResourceEvent] = {
    val query = new BasicDBObject()
    query.put(ResourceEventNames.userID, userId)
    query.put(ResourceEventNames.receivedMillis, new BasicDBObject("$gte", startTimeMillis))
    query.put(ResourceEventNames.receivedMillis, new BasicDBObject("$lte", stopTimeMillis))

    // Sort them by increasing order for occurred time
    val orderBy = new BasicDBObject(ResourceEventNames.occurredMillis, 1)

    MongoDBStore.runQuery[ResourceEvent](query, resourceEvents, orderBy)(MongoDBResourceEvent.fromDBObject)(None)
  }
  
  def countOutOfSyncResourceEventsForBillingPeriod(userID: String, startMillis: Long, stopMillis: Long): Long = {
    val query = new BasicDBObjectBuilder().
      add(ResourceEventModel.Names.userID, userID).
      // received within the period
      add(ResourceEventModel.Names.receivedMillis, new BasicDBObject("$gte", startMillis)).
      add(ResourceEventModel.Names.receivedMillis, new BasicDBObject("$lte", stopMillis)).
      // occurred outside the period
      add("$or", {
        val dbList = new BasicDBList()
        dbList.add(0, new BasicDBObject(ResourceEventModel.Names.occurredMillis, new BasicDBObject("$lt", startMillis)))
        dbList.add(1, new BasicDBObject(ResourceEventModel.Names.occurredMillis, new BasicDBObject("$gt", stopMillis)))
        dbList
      }).
      get()

    resourceEvents.count(query)
  }

  def findAllRelevantResourceEventsForBillingPeriod(userId: String,
                                                    startMillis: Long,
                                                    stopMillis: Long): List[ResourceEvent] = {
    // FIXME: Implement
    Nil
  }
  //-ResourceEventStore

  //+ UserStateStore
  def insertUserState(userState: UserState) = {
    MongoDBStore.insertUserState(
      userState.copy(_id = new ObjectId().toString),
      userStates,
      MongoDBStore.jsonSupportToDBObject
    )
  }

  def findUserStateByUserID(userID: String): Option[UserState] = {
    val query = new BasicDBObject(UserStateJsonNames.userID, userID)
    val cursor = userStates find query

    MongoDBStore.firstResultIfExists(cursor, MongoDBStore.dbObjectToUserState)
  }

  def findLatestUserStateForEndOfBillingMonth(userID: String,
                                              yearOfBillingMonth: Int,
                                              billingMonth: Int): Option[UserState] = {
    val query = new BasicDBObjectBuilder().
      add(UserState.JsonNames.userID, userID).
      add(UserState.JsonNames.isFullBillingMonthState, true).
      add(UserState.JsonNames.theFullBillingMonth.year, yearOfBillingMonth).
      add(UserState.JsonNames.theFullBillingMonth.month, billingMonth).
      get()

    // Descending order, so that the latest comes first
    val sorter = new BasicDBObject(UserState.JsonNames.occurredMillis, -1)

    val cursor = userStates.find(query).sort(sorter)

    MongoDBStore.firstResultIfExists(cursor, MongoDBStore.dbObjectToUserState)
  }

  def deleteUserState(userId: String) = {
    val query = new BasicDBObject(UserStateJsonNames.userID, userId)
    userStates.findAndRemove(query)
  }
  //- UserStateStore

  //+IMEventStore
  def createIMEventFromJson(json: String) = {
    MongoDBStore.createIMEventFromJson(json)
  }

  def createIMEventFromOther(event: IMEventModel) = {
    MongoDBStore.createIMEventFromOther(event)
  }

  def pingIMEventStore(): Unit = {
    MongoDBStore.ping(mongo)
  }

  def insertIMEvent(event: IMEventModel): IMEvent = {
    val localEvent = MongoDBIMEvent.fromOther(event, new ObjectId().toStringMongod)
    MongoDBStore.insertObject(localEvent, imEvents, MongoDBStore.jsonSupportToDBObject)
    localEvent
  }

  def findIMEventById(id: String): Option[IMEvent] = {
    MongoDBStore.findBy(IMEventNames.id, id, imEvents, MongoDBIMEvent.fromDBObject)
  }


  /**
   * Find the `CREATE` even for the given user. Note that there must be only one such event.
   */
  def findCreateIMEventByUserID(userID: String): Option[IMEvent] = {
    val query = new BasicDBObjectBuilder().
      add(IMEventNames.userID, userID).
      add(IMEventNames.eventType, IMEventModel.EventTypeNames.create).get()

    // Normally one such event is allowed ...
    val cursor = imEvents.find(query).sort(new BasicDBObject(IMEventNames.occurredMillis, 1))

    MongoDBStore.firstResultIfExists(cursor, MongoDBIMEvent.fromDBObject)
  }

  def findLatestIMEventByUserID(userID: String): Option[IMEvent] = {
    val query = new BasicDBObject(IMEventNames.userID, userID)
    val cursor = imEvents.find(query).sort(new BasicDBObject(IMEventNames.occurredMillis, -1))

    MongoDBStore.firstResultIfExists(cursor, MongoDBIMEvent.fromDBObject)
  }

  /**
   * Find the very first activation event for a particular user.
   *
   */
  def findFirstIsActiveIMEventByUserID(userID: String): Option[IMEvent] = {
    val query = new BasicDBObjectBuilder().
      add(IMEventNames.userID, userID).
      add(IMEventNames.isActive, true).get()

    val cursor = imEvents.find(query).sort(new BasicDBObject(IMEventNames.occurredMillis, 1))

    withCloseable(cursor) { cursor ⇒
      if(cursor.hasNext) {
        Some(MongoDBIMEvent.fromDBObject(cursor.next()))
      } else {
        None
      }
   }
  }

  /**
   * Scans events for the given user, sorted by `occurredMillis` in ascending order and runs them through
   * the given function `f`.
   *
   * Any exception is propagated to the caller. The underlying DB resources are properly disposed in any case.
   */
  def replayIMEventsInOccurrenceOrder(userID: String)(f: (IMEvent) => Unit) = {
    val query = new BasicDBObject(IMEventNames.userID, userID)
    val cursor = imEvents.find(query).sort(new BasicDBObject(IMEventNames.occurredMillis, 1))

    withCloseable(cursor) { cursor ⇒
      while(cursor.hasNext) {
        val model = MongoDBIMEvent.fromDBObject(cursor.next())
        f(model)
      }
    }
  }
  //-IMEventStore



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
  
  def findPolicyEntry(id: String) = {
    MongoDBStore.findBy(PolicyJsonNames.id, id, policyEntries, MongoDBStore.dbObjectToPolicyEntry)
  }

  //-PolicyStore
}

object MongoDBStore {
  object JsonNames {
    final val _id = "_id"
  }

  /**
   * Collection holding the [[gr.grnet.aquarium.event.model.resource.ResourceEventModel]]s.
   *
   * Resource events are coming from all systems handling billable resources.
   */
  final val RESOURCE_EVENTS_COLLECTION = "resevents"

  /**
   * Collection holding the snapshots of [[gr.grnet.aquarium.computation.UserState]].
   *
   * [[gr.grnet.aquarium.computation.UserState]] is held internally within [[gr.grnet.aquarium.actor.service.user.UserActor]]s.
   */
  final val USER_STATES_COLLECTION = "userstates"

  /**
   * Collection holding [[gr.grnet.aquarium.event.model.im.IMEventModel]]s.
   *
   * User events are coming from the IM module (external).
   */
  final val IM_EVENTS_COLLECTION = "imevents"

  /**
   * Collection holding [[gr.grnet.aquarium.event.model.im.IMEventModel]]s that could not be parsed to normal objects.
   *
   * We of course assume at least a valid JSON representation.
   *
   * User events are coming from the IM module (external).
   */
  final val UNPARSED_IM_EVENTS_COLLECTION = "unparsed_imevents"

  /**
   * Collection holding [[gr.grnet.aquarium.logic.accounting.dsl.DSLPolicy]].
   */
//  final val POLICIES_COLLECTION = "policies"

  /**
   * Collection holding [[gr.grnet.aquarium.event.model.PolicyEntry]].
   */
  final val POLICY_ENTRIES_COLLECTION = "policyEntries"

  def dbObjectToUserState(dbObj: DBObject): UserState = {
    UserState.fromJson(JSON.serialize(dbObj))
  }

  def firstResultIfExists[A](cursor: DBCursor, f: DBObject ⇒ A): Option[A] = {
    withCloseable(cursor) { cursor ⇒
      if(cursor.hasNext) {
        Some(f(cursor.next()))
      } else {
        None
      }
    }
  }

  def dbObjectToPolicyEntry(dbObj: DBObject): PolicyEntry = {
    PolicyEntry.fromJson(JSON.serialize(dbObj))
  }

  def ping(mongo: Mongo): Unit = synchronized {
    // This requires a network roundtrip
    mongo.isLocked
  }

  def findBy[A >: Null <: AnyRef](name: String,
                                  value: String,
                                  collection: DBCollection,
                                  deserializer: (DBObject) => A) : Option[A] = {
    val query = new BasicDBObject(name, value)
    val cursor = collection find query

    withCloseable(cursor) { cursor ⇒
      if(cursor.hasNext)
        Some(deserializer apply cursor.next)
      else
        None
    }
  }

  def runQuery[A <: ExternalEventModel](query: DBObject, collection: DBCollection, orderBy: DBObject = null)
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

  def storeUserState(userState: UserState, collection: DBCollection) = {
    storeAny[UserState](userState, collection, ResourceEventNames.userID, _.userID, MongoDBStore.jsonSupportToDBObject)
  }
  
  def storePolicyEntry(policyEntry: PolicyEntry, collection: DBCollection): Maybe[RecordID] = {
    Maybe(storeAny[PolicyEntry](policyEntry, collection, PolicyJsonNames.id, _.id, MongoDBStore.jsonSupportToDBObject))
  }

  def storeAny[A](any: A,
                  collection: DBCollection,
                  idName: String,
                  idValueProvider: (A) => String,
                  serializer: (A) => DBObject) : RecordID = {

    val dbObject = serializer apply any
    val _id = new ObjectId()
    dbObject.put("_id", _id)
    val writeResult = collection.insert(dbObject, WriteConcern.JOURNAL_SAFE)
    writeResult.getLastError().throwOnError()

    RecordID(dbObject.get("_id").toString)
  }

  // FIXME: consolidate
  def insertUserState[A <: UserState](obj: A, collection: DBCollection, serializer: A ⇒ DBObject) = {
    val dbObject = serializer apply obj
    val objectId = obj._id  match {
      case null ⇒
        val _id = new ObjectId()
        dbObject.put("_id", _id)
        _id

      case _id ⇒
        _id
    }

    dbObject.put(JsonNames._id, objectId)

    collection.insert(dbObject, WriteConcern.JOURNAL_SAFE)

    obj
  }

  def insertObject[A <: MongoDBEventModel](obj: A, collection: DBCollection, serializer: (A) => DBObject) : Unit = {
    val dbObject = serializer apply obj
    val objectId = obj._id  match {
      case null ⇒
        val _id = new ObjectId()
        dbObject.put("_id", _id)
        _id

      case _id ⇒
        _id
    }

    dbObject.put(JsonNames._id, objectId)

    collection.insert(dbObject, WriteConcern.JOURNAL_SAFE)
  }

  def jsonSupportToDBObject(jsonSupport: JsonSupport) = {
    Conversions.jsonSupportToDBObject(jsonSupport)
  }

  final def isLocalIMEvent(event: IMEventModel) = event match {
    case _: MongoDBIMEvent ⇒ true
    case _ ⇒ false
  }

  final def createIMEventFromJson(json: String) = {
    MongoDBIMEvent.fromJsonString(json)
  }

  final def createIMEventFromOther(event: IMEventModel) = {
    MongoDBIMEvent.fromOther(event, null)
  }

  final def createIMEventFromJsonBytes(jsonBytes: Array[Byte]) = {
    MongoDBIMEvent.fromJsonBytes(jsonBytes)
  }
}
