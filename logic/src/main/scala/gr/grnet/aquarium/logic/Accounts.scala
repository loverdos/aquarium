package gr.grnet.aquarium.logic

import gr.grnet.aquarium.model._

trait Accounts {

  def addUserToGroup(u : User, g : Group) = {
    DB.persist(g)
  }


  def addUserToOrg(u : User, o : Organization) = {
    DB.persist(o)
  }


}