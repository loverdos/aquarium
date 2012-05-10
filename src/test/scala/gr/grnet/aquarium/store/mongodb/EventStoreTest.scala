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

import org.junit.Assert._
import org.junit.Assume._
import gr.grnet.aquarium.Configurator._
import gr.grnet.aquarium.util.{RandomEventGenerator, TestMethods}
import collection.mutable.ArrayBuffer
import org.junit.{After, Test}
import gr.grnet.aquarium.{StoreConfigurator, LogicTestsAssumptions}
import gr.grnet.aquarium.store.memory.MemStore
import gr.grnet.aquarium.event.model.resource.ResourceEventModel

/**
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class EventStoreTest extends TestMethods
with RandomEventGenerator with StoreConfigurator {

  @Test
  def testStoreEvent() = {
    assumeTrue(LogicTestsAssumptions.EnableStoreTests)

    val event = nextResourceEvent()
    val store = config.resourceEventStore
    val result = store.insertResourceEvent(event)
  }

  @Test
  def testFindEventById(): Unit = {
    assumeTrue(LogicTestsAssumptions.EnableStoreTests)

    val event = nextResourceEvent()
    val store = config.resourceEventStore

    store.insertResourceEvent(event)

    store.findResourceEventById(event.id)
  }

  @Test
  def testfindEventsByUserId(): Unit = {
    assumeTrue(LogicTestsAssumptions.EnableStoreTests)
    val events = new ArrayBuffer[ResourceEventModel]()
    val store = config.resourceEventStore

    (1 to 100).foreach {
      n =>
        val e = nextResourceEvent
        events += e
        store.insertResourceEvent(e)
    }

    val mostUsedId = events
      .map{x => x.userID}
      .groupBy(identity)
      .mapValues(_.size)
      .foldLeft(("",0))((acc, kv) => if (kv._2 > acc._2) kv else acc)._1

    val result = store.findResourceEventsByUserId(mostUsedId)(None)
    assertEquals(events.filter(p => p.userID.equals(mostUsedId)).size, result.size)
  }

  @Test
  def testMultipleMongos = {
    assumeTrue(LogicTestsAssumptions.EnableStoreTests)
    val a = getMongo
    val b = getMongo
    //assertEquals(a.Connection.mongo.get.hashCode(), b.Connection.mongo.get.hashCode())
  }

  @After
  def after: Unit = {
    if (isInMemStore) return

    val a = getMongo

    val col = a.mongo.getDB(
      MasterConfigurator.get(MongoDBStoreProvider.MongoDBKeys.dbschema)
    ).getCollection(MongoDBStore.RESOURCE_EVENTS_COLLECTION)

    //val res = col.find
    //while (res.hasNext)
    //  col.remove(res.next)
  }

  private lazy val config = configurator
  private val isInMemStore = config.resourceEventStore.isInstanceOf[MemStore]
  private def getMongo = config.resourceEventStore.asInstanceOf[MongoDBStore]
}