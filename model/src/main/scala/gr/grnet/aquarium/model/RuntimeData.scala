package gr.grnet.aquarium.model

import java.util.Date
import javax.persistence._

@javax.persistence.Entity
@Table(name = "RUNTIME_DATA")
class RuntimeData extends Id {

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "TIMESTAMP")
  var timestamp : Date = _

  @Column(name = "MEASUREMENT")
  var measurement : Float = _

  @ManyToOne(cascade = Array(CascadeType.ALL),
              targetEntity = classOf[ServiceItemConfig])
  @JoinColumn(name = "SERVICE_ITEM_CONFIG_ID")
  var item : ServiceItemConfig = _
}