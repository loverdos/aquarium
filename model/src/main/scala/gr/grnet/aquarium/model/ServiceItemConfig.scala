package gr.grnet.aquarium.model

import javax.persistence._

case class ServiceItemConfigKey(var item_id : Int, var resource_id : Int)

@Table(name = "SERVICE_ITEM_CONFIG")
@IdClass(classOf[ServiceItemConfigKey])
@javax.persistence.Entity
class ServiceItemConfig {

  //def this() = this("")

  @javax.persistence.Id
  @Column(name ="SERVICE_ITEM_ID", nullable=false, updatable=false,
          insertable=false)
  var item_id: Int = _

  @javax.persistence.Id
  @Column(name ="CONSUMABLE_RESOURCE_ID", nullable=false, updatable=false,
          insertable=false)
  var resource_id: Int = _

  @ManyToOne (cascade = Array(CascadeType.ALL),
              targetEntity = classOf[ServiceItem])
  @JoinColumn(name = "SERVICE_ITEM_ID")
  var item : ServiceItem = _

  @ManyToOne (cascade = Array(CascadeType.ALL),
              targetEntity = classOf[ConsumableResource])
  @JoinColumn(name = "CONSUMABLE_RESOURCE_ID")
  var resource : ConsumableResource = _

  @Column(name = "QUANTITY")
  var quantity: Int = _
}