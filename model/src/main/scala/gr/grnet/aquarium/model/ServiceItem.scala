package gr.grnet.aquarium.model

import javax.persistence._

@javax.persistence.Entity
@Table(name = "SERVICE_ITEM")
@NamedQuery(name="allServiceItems", query="select si from ServiceItem si")
class ServiceItem extends Id {

  @Column(name = "URL")
  var url : String = ""

  @OneToMany(mappedBy = "item",  targetEntity = classOf[ServiceItemConfig],
             cascade = Array(CascadeType.ALL))
  var configItems : java.util.Set[ServiceItemConfig] = new java.util.HashSet[ServiceItemConfig]()
}
