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

import gr.grnet.aquarium.util.{TestMethods, RandomEventGenerator}
import org.junit.Assume._
import org.junit.Assert._
import collection.mutable.ArrayBuffer
import gr.grnet.aquarium.logic.events.ResourceEvent
import org.junit.{Before, After, Test}
import gr.grnet.aquarium.{MasterConf, LogicTestsAssumptions}

/**
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class EventStoreTest extends TestMethods with RandomEventGenerator {

  @Before
  def before() = {
  }

  @Test
  def testStoreEvent() = {
    assumeTrue(LogicTestsAssumptions.EnableMongoDBTests)

    val event = nextResourceEvent()
    val store = MasterConf.MasterConf.eventStore

    //assert(store.isJust)
  }

  @Test
  def testFindEventById(): Unit = {
    assumeTrue(LogicTestsAssumptions.EnableMongoDBTests)

    val event = nextResourceEvent()
    val store = MasterConf.MasterConf.eventStore

    val result1 = store.storeEvent(event)
    assert(result1.isJust)

    val result2 = store.findEventById[ResourceEvent](event.id)
    assertNotNone(result2)
  }

  @Test
  def testfindEventsByUserId(): Unit = {
    assumeTrue(LogicTestsAssumptions.EnableMongoDBTests)
    val events = new ArrayBuffer[ResourceEvent]()
    val store = MasterConf.MasterConf.eventStore

    (1 to 100).foreach {
      n =>
        val e = nextResourceEvent
        events += e
        store.storeEvent(e)
    }

    val mostUsedId = events
      .map{x => x.userId}
      .groupBy(identity)
      .mapValues(_.size)
      .foldLeft((0L,0))((acc, kv) => if (kv._2 > acc._2) kv else acc)._1

    val result = store.findEventsByUserId(mostUsedId)(None)
    assertEquals(events.filter(p => p.userId.equals(mostUsedId)).size, result.size)
  }

  @Test
  def testMultipleMongos = {
    assumeTrue(LogicTestsAssumptions.EnableMongoDBTests)
    val a = getMongo
    val b = getMongo
    //assertEquals(a.Connection.mongo.get.hashCode(), b.Connection.mongo.get.hashCode())
  }

  @After
  def after() = {
    assumeTrue(LogicTestsAssumptions.EnableMongoDBTests)
    val a = getMongo

    val col = a.mongo.getDB(
      MasterConf.MasterConf.get(MasterConf.Keys.persistence_db)
    ).getCollection(MongoDBStore.EVENTS_COLLECTION)

    val res = col.find
    while (res.hasNext)
      col.remove(res.next)
  }

  private def getMongo = MasterConf.MasterConf.eventStore.asInstanceOf[MongoDBStore]
}