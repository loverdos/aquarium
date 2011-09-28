package gr.grnet.aquarium.model

import javax.persistence._
import java.util.{Set, HashSet}

@Table(name = "ORGANIZATION")
@javax.persistence.Entity
@DiscriminatorValue("3")
class Organization extends Entity {

  @ManyToOne(targetEntity = classOf[Organization])
  @JoinColumn(name = "PARENT_ORG_ID")
  var parent : Organization = _

  @ManyToMany(targetEntity = classOf[User],
              cascade= Array(CascadeType.ALL))
  @JoinTable(name="USER_ORGANIZATION",
             joinColumns = Array(new JoinColumn(name="USER_ID")),
             inverseJoinColumns = Array(new JoinColumn(name="ORGANIZATION_ID")))
  var users : Set[User] = new HashSet[User]()

  @ManyToMany(targetEntity = classOf[User],
              cascade= Array(CascadeType.ALL))
  @JoinTable(name="GROUP_ORGANIZATION",
             joinColumns = Array(new JoinColumn(name="GROUP_ID")),
             inverseJoinColumns = Array(new JoinColumn(name="ORGANIZATION_ID")))
  var groups : Set[Group] = new HashSet[Group]()
}
