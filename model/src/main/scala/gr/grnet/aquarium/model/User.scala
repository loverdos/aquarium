package gr.grnet.aquarium.model

import javax.persistence._

@javax.persistence.Entity
@DiscriminatorValue("1")
class User extends Entity {

  @ManyToOne(optional = true)
  @JoinColumn(name = "ORG_ID", referencedColumnName = "ID")
  var org : Organization = _

  @ManyToMany(targetEntity = classOf[Group],
              mappedBy = "users",
              cascade = Array(CascadeType.PERSIST, CascadeType.MERGE))
  var groups : java.util.Set[Group] = new java.util.HashSet[Group]()

}