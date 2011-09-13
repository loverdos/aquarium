package gr.grnet.aquarium.model

import javax.persistence._

@javax.persistence.Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="ENTITY_TYPE",
  discriminatorType=DiscriminatorType.INTEGER
)
abstract class Entity extends Id {

  @Column(name = "NAME")
  var name: String = ""

  @Column(name = "CREDITS", nullable = true)
  var credits: Int = 0
}