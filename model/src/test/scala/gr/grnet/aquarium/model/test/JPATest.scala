package gr.grnet.aquarium.model.test

import gr.grnet.aquarium.model._
import scala.collection.JavaConversions._
import org.scala_libs.jpa.{LocalEMF, ThreadLocalEM}
import org.junit._
import Assert._

object DB extends LocalEMF("aquarium") with ThreadLocalEM {}

class TestJPAWeb {

  @Before
  def before() = {
    if (!DB.getTransaction.isActive)
      DB.getTransaction.begin
  }

  @Test
  def testBasicEMFunctionality() = {

    val user = new User
    user.name = "foobar"
    DB.persist(user)
    DB.flush()

    val a = DB.find(classOf[User], user.id)
    assert(a.exists(u => u.id == user.id))
  }

  @Test
  def testEntities() = {

    //Recursive organizations
    val org1 = new Organization
    org1.name = "EDET"

    DB.persist(org1)

    val org2 = new Organization
    org2.name = "AUEB"
    org2.parent = org1

    DB.persist(org2)
    DB.flush()

    assertTrue(org1.id != org2.id)

    val results = DB.find(classOf[Organization], org1.id)
    assert(results.exists(o => o.id == org1.id))

    //Add to entity with composite key
    val srv1 = new ServiceItem
    srv1.url = "http://foo.bar/"
    DB.persist(srv1)

    val res1 = new ConsumableResource
    res1.restype = "CPU"
    res1.unittype = "CPU/hr"
    res1.cost = 10f
    DB.persist(res1)

    val res2 = new ConsumableResource
    res2.restype = "RAM"
    res2.unittype = "MB/hr"
    res2.cost = 11f
    DB.persist(res2)

    def addServiceConfig(srv : ServiceItem,
                               res : ConsumableResource,
                               value : Float) {
      val srvcfg2 = new ServiceItemConfig
      srvcfg2.item = srv
      srvcfg2.resource = res
      srvcfg2.quantity = value
      srv.configItems.add(srvcfg2)
      res.configItems.add(srvcfg2)
      DB.persist(srvcfg2)
      DB.flush()
    }

    addServiceConfig(srv1, res1, 4f)
    addServiceConfig(srv1, res2, 128f)

    val a = DB.find(classOf[ServiceItem], srv1.id)
    assert(a.exists(o => o.id == srv1.id))
    assertEquals(2, a.get.configItems.size)

    //Entity navigation tests
    val all = DB.findAll[ServiceItem]("allServiceItems")
    all.foreach(f => asScalaSet(f.configItems).foreach(i => assertFalse(i.quantity <= 0)))
  }

  @After
  def after() = {
    DB.getTransaction.rollback
  }
}
