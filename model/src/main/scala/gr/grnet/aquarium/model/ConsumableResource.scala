package gr.grnet.aquarium.model

import javax.persistence._

@Table(name = "CONSUMABLE_RESOURCE")
@javax.persistence.Entity
class ConsumableResource extends Id {

  // TODO: The following should be an enum
  @Column(name = "RESOURCE_TYPE")
  var restype : String = _

  @Column(name = "UNIT_TYPE")
  var unittype : String = _

  @Column(name = "COST")
  var cost : Float = _

  @OneToMany(mappedBy = "resource",  targetEntity = classOf[ServiceItemConfig],
             cascade = Array(CascadeType.ALL))
  var configItems : java.util.Set[ServiceItemConfig] = new java.util.HashSet[ServiceItemConfig]()
}