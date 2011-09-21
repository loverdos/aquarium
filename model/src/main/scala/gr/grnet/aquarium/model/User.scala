package gr.grnet.aquarium.model

import javax.persistence._
import java.util.{Set, HashSet}

@javax.persistence.Entity
@DiscriminatorValue("1")
class User extends gr.grnet.aquarium.model.Entity {

  @ManyToMany(targetEntity = classOf[Group],
              mappedBy = "users",
              cascade = Array(CascadeType.ALL))
  var groups : Set[Group] = new HashSet[Group]()
}