package gr.grnet.aquarium.model

import javax.persistence._

@javax.persistence.Entity
@DiscriminatorValue("2")
class Group extends Entity {

  @ManyToOne(optional = true)
  @JoinColumn(name = "SUB_GROUP_ID")
  var group :Group = _

  @ManyToOne(optional = true)
  var org : Organization = _

  @ManyToMany(targetEntity = classOf[User],
              cascade= Array(CascadeType.PERSIST, CascadeType.MERGE))
  @JoinTable(name="USER_GROUP",
             joinColumns = Array(new JoinColumn(name="USER_ID")),
             inverseJoinColumns = Array(new JoinColumn(name="GROUP_ID")))
  var users : java.util.Set[User] = new java.util.HashSet[User]()
}