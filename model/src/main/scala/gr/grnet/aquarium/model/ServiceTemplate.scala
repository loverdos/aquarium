package gr.grnet.aquarium.model

import java.util.{HashSet, Set}
import javax.persistence._

@Table(name = "SERVICE_TEMPLATE")
@javax.persistence.Entity
class ServiceTemplate extends Id {

  @OneToMany(mappedBy = "template",  targetEntity = classOf[DefaultPermission],
             cascade = Array(CascadeType.ALL))
  var permissions : Set[DefaultPermission] = new HashSet[DefaultPermission]()

  @OneToMany(mappedBy = "template",  targetEntity = classOf[ServiceItem],
             cascade = Array(CascadeType.ALL))
  var items : Set[ServiceItem] = new HashSet[ServiceItem]()

  @ManyToOne(cascade = Array(CascadeType.ALL),
             targetEntity = classOf[ServiceTemplate])
  @JoinColumn(name = "RESOURCE_TYPE_ID")
  var resType : ResourceType = _
}