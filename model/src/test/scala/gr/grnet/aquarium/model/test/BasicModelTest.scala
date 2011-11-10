package gr.grnet.aquarium.model.test

import gr.grnet.aquarium.model._
import org.junit._
import Assert._

class BasicModelTest {

  @Before
  def before() = {
    if (!TestDB.getTransaction.isActive)
      TestDB.getTransaction.begin
  }

  @Test
  def testBasicEMFunctionality() = {

    val user = new User
    user.name = "foobar"
    TestDB.persist(user)
    TestDB.flush()

    val a = TestDB.find(classOf[User], user.id)
    assert(a.exists(u => u.id == user.id))
  }

  @Test
  def testEntities() = {

    //Recursive organizations
    val org1 = new Organization
    org1.name = "EDET"

    TestDB.persist(org1)

    val org2 = new Organization
    org2.name = "AUEB"
    org2.parent = org1

    TestDB.persist(org2)
    TestDB.flush()

    assertTrue(org1.id != org2.id)

    val results = TestDB.find(classOf[Organization], org1.id)
    assert(results.exists(o => o.id == org1.id))

    //Add to entity with composite key
    val srv1 = new ServiceItem
    srv1.url = "http://foo.bar/"
    TestDB.persist(srv1)

  }

  @After
  def after() = {
    TestDB.getTransaction.rollback()
  }
}
