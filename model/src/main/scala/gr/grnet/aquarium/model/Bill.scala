package gr.grnet.aquarium.model

import java.util.Date
import javax.persistence._

@javax.persistence.Entity
@Table(name = "BILL")
class Bill extends Id {

  @Temporal(TemporalType.DATE)
  @Column(name = "WHEN_CALCULATED")
  var whenCalculated : Date = _

  @Column(name = "COST")
  var cost : Float = _

  @ManyToOne(cascade = Array(CascadeType.ALL),
             targetEntity = classOf[gr.grnet.aquarium.model.Entity])
  @JoinColumn(name = "ENTITY_ID")
  var entity : gr.grnet.aquarium.model.Entity = _

  @ManyToOne(cascade = Array(CascadeType.ALL),
             targetEntity = classOf[ServiceItem])
  @JoinColumn(name = "SERVICE_ITEM_ID")
  var item : ServiceItem = _
}
