package gr.grnet.aquarium.model

import javax.persistence._

@Table(name = "ORGANIZATION")
@javax.persistence.Entity
class Permission extends Id {

  var action : Action = _

  var entity : Entity = _
}