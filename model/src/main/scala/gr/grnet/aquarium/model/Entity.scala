package gr.grnet.aquarium.model

import javax.persistence._
import java.util.{HashSet, Set}

@javax.persistence.Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="ENTITY_TYPE",
  discriminatorType=DiscriminatorType.INTEGER
)
@Table(name="ENTITY")
abstract class Entity extends Id {

  @Column(name = "NAME")
  var name: String = ""

  @Column(name = "CREDITS", nullable = true)
  var credits: Int = 0

  @OneToMany(mappedBy = "entity",  targetEntity = classOf[Permission],
             cascade = Array(CascadeType.ALL))
  var permissions : Set[Permission] = new HashSet[Permission]()

  @OneToMany(mappedBy = "owner",  targetEntity = classOf[ServiceItem],
             cascade = Array(CascadeType.ALL))
  var serviceItems : Set[ServiceItem] = new HashSet[ServiceItem]()

  @ManyToMany(targetEntity = classOf[Organization],
              mappedBy = "entities",
              cascade = Array(CascadeType.ALL))
  var organizations : Set[Organization] = new HashSet[Organization]()
}