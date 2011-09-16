package gr.grnet.aquarium.model

import javax.persistence._

@javax.persistence.Entity
@Table(name = "SERVICE_ITEM")
@NamedQueries(Array(
  new NamedQuery(name="allServiceItems", query="select si from ServiceItem si"),
  new NamedQuery(name="servItemsPerEntity", query="select si from ServiceItem si")
))
class ServiceItem extends Id {

  @Column(name = "URL")
  var url : String = ""

  @OneToMany(mappedBy = "item",  targetEntity = classOf[ServiceItemConfig],
             cascade = Array(CascadeType.ALL))
  var configItems : java.util.Set[ServiceItemConfig] = new java.util.HashSet[ServiceItemConfig]()

  @OneToMany(mappedBy = "item",  targetEntity = classOf[Bill],
             cascade = Array(CascadeType.ALL))
  var bill : java.util.Set[Bill] = new java.util.HashSet[Bill]()
}
