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
  var credits: Float = 0

  @Column(name = "AGREEMENT", nullable = true)
  var agreement: Long = 0

  @OneToMany(mappedBy = "entity",  targetEntity = classOf[Permission],
             cascade = Array(CascadeType.ALL))
  var permissions : Set[Permission] = new HashSet[Permission]()

  @OneToMany(mappedBy = "owner",  targetEntity = classOf[ServiceItem],
             cascade = Array(CascadeType.ALL))
  var serviceItems : Set[ServiceItem] = new HashSet[ServiceItem]()
}