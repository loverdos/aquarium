package gr.grnet.aquarium.model.test

import gr.grnet.aquarium.model._
import org.scala_libs.jpa.{LocalEMF, ThreadLocalEM}
import org.junit.{Before, Test}

object JPA extends LocalEMF("aquarium") with ThreadLocalEM {}

class TestJPAWeb {

  @Test
  def testBasicEMFunctionality() = {

    val user = new User
    user.name = "foobar"
    JPA.persist(user)
    JPA.getTransaction().commit()

    val a = JPA.find(classOf[User], 1L)
  }

  @Test
  def testEntities() = {
    JPA.getTransaction().begin()

    //Recursive organizations
    val org1 = new Organization
    org1.name = "NTUA"

    JPA.persist(org1)

    val org2 = new Organization
    org2.name = "AUEB"
    org1.parent = org1

    JPA.persist(org2)
    JPA.find(classOf[Organization], org1.id)

    JPA.getTransaction().commit()
  }
}
