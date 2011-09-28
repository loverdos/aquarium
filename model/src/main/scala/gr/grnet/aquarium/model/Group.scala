package gr.grnet.aquarium.model

import javax.persistence._
import java.util.{Set, HashSet}

@javax.persistence.Entity
@DiscriminatorValue("2")
class Group extends Entity {

  @ManyToOne(optional = true)
  @JoinColumn(name = "PARENT_GROUP_ID")
  var group : Group = _

  @ManyToMany(targetEntity = classOf[User],
              cascade= Array(CascadeType.ALL))
  @JoinTable(name="USER_GROUP",
             joinColumns = Array(new JoinColumn(name="USER_ID")),
             inverseJoinColumns = Array(new JoinColumn(name="GROUP_ID")))
  var users : Set[User] = new HashSet[User]()
}