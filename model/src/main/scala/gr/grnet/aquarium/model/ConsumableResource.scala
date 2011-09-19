package gr.grnet.aquarium.model

import javax.persistence._
import java.util.{Set, HashSet}

@Table(name = "CONSUMABLE_RESOURCE")
@javax.persistence.Entity
class ConsumableResource extends Id {

  @ManyToOne (cascade = Array(CascadeType.ALL),
              targetEntity = classOf[ResourceType])
  @JoinColumn(name = "RESOURCE_TYPE_ID")
  var restype : ResourceType = _

  @Column(name = "UNIT_TYPE")
  var unittype : String = _

  @Column(name = "COST")
  var cost : Float = _

  @OneToMany(mappedBy = "resource",  targetEntity = classOf[ServiceItemConfig],
             cascade = Array(CascadeType.ALL))
  var configItems : Set[ServiceItemConfig] = new HashSet[ServiceItemConfig]()
}