package gr.grnet.aquarium.model

import javax.persistence._

@Table(name = "PERMISSION")
@javax.persistence.Entity
class Permission extends Id {

  @ManyToOne(cascade = Array(CascadeType.ALL),
             targetEntity = classOf[Action])
  @JoinColumn(name = "ACTION_ID")
  var action : Action = _

  @ManyToOne(cascade = Array(CascadeType.ALL),
             targetEntity = classOf[gr.grnet.aquarium.model.Entity])
  @JoinColumn(name = "ENTITY_ID")
  var entity : gr.grnet.aquarium.model.Entity  = _

  @ManyToOne(cascade = Array(CascadeType.ALL),
             targetEntity = classOf[ServiceItem])
  @JoinColumn(name = "SERVICE_ITEM_ID")
  var item : ServiceItem = _
}