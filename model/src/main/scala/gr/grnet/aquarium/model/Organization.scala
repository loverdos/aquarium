package gr.grnet.aquarium.model

import javax.persistence._

@Table(name = "ORGANIZATION")
@javax.persistence.Entity
class Organization extends Id {

  @Column(name = "NAME")
  var name: String = ""

  @Column(name = "CREDITS")
  var credits: Int = 0

  @ManyToOne(optional = true)
  @JoinColumn(name = "PARENT_ORG_ID")
  var parent : Organization = _

  @OneToMany(mappedBy = "org",  targetEntity = classOf[Group],
             cascade = Array(CascadeType.ALL))
  var groups : java.util.Set[Group] = new java.util.HashSet[Group]()

  @OneToMany(mappedBy = "org",  targetEntity = classOf[User],
             cascade = Array(CascadeType.ALL))
  var users : java.util.Set[User] = new java.util.HashSet[User]()
}