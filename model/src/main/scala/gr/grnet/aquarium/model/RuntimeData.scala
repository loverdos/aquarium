package gr.grnet.aquarium.model

import javax.persistence._
import java.util.Date

@javax.persistence.Entity
@Table(name = "RUNTIME_DATA")
class RuntimeData extends Id {

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "TIMESTAMP")
  var timestamp : Date = new Date()

  @Column(name = "MEASUREMENT")
  var measurement : Float = 0.0F

  @ManyToOne(cascade = Array(CascadeType.ALL),
              targetEntity = classOf[ServiceItemConfig])
  @JoinColumn(name = "SERVICE_ITEM_CONFIG_ID")
  var item : ServiceItemConfig = _
}