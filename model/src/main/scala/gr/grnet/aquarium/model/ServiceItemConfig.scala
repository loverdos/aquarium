package gr.grnet.aquarium.model

import javax.persistence._

@Table(name = "SERVICE_ITEM_CONFIG")
@javax.persistence.Entity
class ServiceItemConfig extends Id {

  @ManyToOne (cascade = Array(CascadeType.ALL),
              targetEntity = classOf[ServiceItem])
  @JoinColumn(name = "SERVICE_ITEM_ID")
  var item : ServiceItem = _

  @ManyToOne (cascade = Array(CascadeType.ALL),
              targetEntity = classOf[ConsumableResource])
  @JoinColumn(name = "CONSUMABLE_RESOURCE_ID")
  var resource : ConsumableResource = _

  @Column(name = "QUANTITY")
  var quantity: Float = _

  @OneToMany(mappedBy = "item",  targetEntity = classOf[RuntimeData],
             cascade = Array(CascadeType.ALL))
  var runtime : java.util.Set[RuntimeData] = new java.util.HashSet[RuntimeData]()
}