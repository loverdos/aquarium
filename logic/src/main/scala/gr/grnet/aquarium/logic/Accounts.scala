package gr.grnet.aquarium.logic

import gr.grnet.aquarium.model._
import org.scala_libs.jpa.{ThreadLocalEM, LocalEMF}

object DB extends LocalEMF("aquarium", true) with ThreadLocalEM {}

trait Accounts {

  def addUserToGroup(u : User, g : Group) = {
    g.users.add u
    DB.persist g
  }


  def addUserToOrg(u : User, o : Organization) = {
    o.users.add u
    DB.persist o
  }
}