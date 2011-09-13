package gr.grnet.aquarium.model.test

import gr.grnet.aquarium.model._
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
  }

  @After
  def after() = {
    DB.getTransaction.rollback
  }
}
