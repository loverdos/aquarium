package gr.grnet.aquarium.model

import javax.persistence._
import java.util.{Set, HashSet}

@Table(name = "CONSUMABLE_RESOURCE")
@javax.persistence.Entity
class ConsumableResource extends Id {

  @Column(name = "NAME")
  var name : String = ""

  @ManyToOne (cascade = Array(CascadeType.ALL),
              targetEntity = classOf[ResourceType])
  @JoinColumn(name = "RESOURCE_TYPE_ID")
  var restype : ResourceType = _

}
