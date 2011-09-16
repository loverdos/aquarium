package gr.grnet.aquarium.model

import javax.persistence._

@javax.persistence.Entity
@Table(name = "DEFAULT_PERMISSION")
class DefaultPermission extends Id{

  @ManyToOne(cascade = Array(CascadeType.ALL),
             targetEntity = classOf[EntityType])
  @JoinColumn(name = "ENTITY_TYPE_ID")
  var enttype : EntityType = _

  @ManyToOne(cascade = Array(CascadeType.ALL),
             targetEntity = classOf[Action])
  @JoinColumn(name = "ACTION_ID")
  var action : Action = _

  @ManyToOne(cascade = Array(CascadeType.ALL),
             targetEntity = classOf[ServiceTemplate])
  @JoinColumn(name = "SERVICE_TEMPLATE_ID")
  var template : ServiceTemplate = _
}