package gr.grnet.aquarium.logic

import gr.grnet.aquarium.model._

trait Accounts {

  def addUserToGroup(u : User, g : Group) = {
    g.users.add(u)
    DB.persist(g)
  }


  def addUserToOrg(u : User, o : Organization) = {
    o.entities.add(u)
    u.organizations.add(o)
    DB.persist(o)
  }


}