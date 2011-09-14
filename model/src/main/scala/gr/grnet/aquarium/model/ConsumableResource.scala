package gr.grnet.aquarium.model

import javax.persistence._

@Table(name = "CONSUMABLE_RESOURCE")
@javax.persistence.Entity
class ConsumableResource extends Id {

  @Column(name = "NAME")
  var name : String = _

  @Column(name = "RESOURCE_TYPE")
  var restype : String = _

  @Column(name = "UNIT_TYPE")
  var unittype : String = _

  @Column(name = "COST")
  var cost : Float = _

  @OneToMany(mappedBy = "resource",  targetEntity = classOf[ServiceItemConfig],
             cascade = Array(CascadeType.REMOVE))
  var configItems : java.util.Set[ServiceItemConfig] = new java.util.HashSet[ServiceItemConfig]()
}