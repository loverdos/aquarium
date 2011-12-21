package gr.grnet.aquarium.store.mongodb

import gr.grnet.aquarium.util.{RandomEventGenerator, TestMethods}
import gr.grnet.aquarium.MasterConf._
import org.junit.Assume._
import gr.grnet.aquarium.LogicTestsAssumptions
import org.junit.{After, Test}

/**
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class IMStoreTest extends TestMethods with RandomEventGenerator {

  @Test
  def testStoreEvent() = {
    assumeTrue(LogicTestsAssumptions.EnableStoreTests)

    val event = nextUserEvent()
    val store = MasterConf.IMStore
    val result = store.storeUserEvent(event)

    assert(result.isJust)
  }

  @Test
  def testfindUserEventById() = {

  }

  @After
  def after() = {
    val a = getMongo

    val col = a.mongo.getDB(MasterConf.get(Keys.persistence_db)
    ).getCollection(MongoDBStore.EVENTS_COLLECTION)

    val res = col.find
    while (res.hasNext)
      col.remove(res.next)
  }

  private def getMongo = MasterConf.IMStore.asInstanceOf[MongoDBStore]
}