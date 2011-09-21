package gr.grnet.aquarium.model

import javax.persistence._
import java.util.{Set, HashSet}

@Table(name = "ORGANIZATION")
@javax.persistence.Entity
class Organization extends Id {

  @Column(name = "NAME")
  var name: String = ""

  @Column(name = "CREDITS")
  var credits: Int = 0

  @ManyToOne(targetEntity = classOf[Organization])
  @JoinColumn(name = "PARENT_ORG_ID")
  var parent : Organization = _

  @ManyToMany(targetEntity = classOf[Entity],
              cascade = Array(CascadeType.ALL))
  @JoinTable(name="ENTITY_ORGANIZATION",
             joinColumns = Array(new JoinColumn(name="ENTITY_ID")),
             inverseJoinColumns = Array(new JoinColumn(name="ORGANIZATION_ID")))
  var entities : Set[Entity] = new HashSet[Entity]()
}
