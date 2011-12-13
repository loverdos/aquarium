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

import org.junit.Test
import gr.grnet.aquarium.util.{TestMethods, RandomEventGenerator}
import org.junit.Assume._
import org.junit.Assert._
import gr.grnet.aquarium.LogicTestsAssumptions
import gr.grnet.aquarium.store.{RecordID, Store}
import collection.mutable.ArrayBuffer
import gr.grnet.aquarium.logic.events.ResourceEvent

/**
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */

class EventStoreTest extends TestMethods with RandomEventGenerator {

  @Test
  def testStoreEvent() = {
    assumeTrue(LogicTestsAssumptions.EnableMongoDBTests)

    val event = nextResourceEvent()
    val store = Store.getEventStore()

    val result = store.get.storeEvent(event)
    assert(result.isJust)
    assertEquals(event.id, result.getOr(RecordID("foo")).id)
  }

  @Test
  def testfindEventsByUserId(): Unit = {
    assumeTrue(LogicTestsAssumptions.EnableMongoDBTests)
    val events = new ArrayBuffer[ResourceEvent]()
    val store = Store.getEventStore()

    (1 to 100).foreach {
      n =>
        val e = nextResourceEvent
        events += e
        store.get.storeEvent(e)
    }

    val mostUsedId = events.map{x => x.userId}.groupBy(identity).mapValues(_.size).foldLeft((0L,0))((acc, kv) => if (kv._2 > acc._2) kv else acc)._1
    println("Most used id:" + mostUsedId)

    store.get.findEventsByUserId(mostUsedId)(None)

  }
}