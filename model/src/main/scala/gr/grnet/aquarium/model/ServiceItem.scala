package gr.grnet.aquarium.model

import java.util.{Set, HashSet}
import javax.persistence._

@javax.persistence.Entity
@Table(name = "SERVICE_ITEM")
@NamedQueries(Array(
  new NamedQuery(name="allSi", query="select si from ServiceItem si"),
  new NamedQuery(name="siPerEntity",
                 query="select si from ServiceItem si where si.owner = :owner")
))
class ServiceItem extends Id {

  @Column(name = "URL")
  var url : String = ""

  @ManyToOne(cascade = Array(CascadeType.ALL),
             targetEntity = classOf[ServiceItem])
  @JoinColumn(name = "ENTITY_ID")
  var owner : gr.grnet.aquarium.model.Entity = _

  @OneToMany(mappedBy = "item",  targetEntity = classOf[ServiceItemConfig],
             cascade = Array(CascadeType.ALL))
  var configItems : Set[ServiceItemConfig] = new HashSet[ServiceItemConfig]()

  @OneToMany(mappedBy = "item",  targetEntity = classOf[Bill],
             cascade = Array(CascadeType.ALL))
  var bill : java.util.Set[Bill] = new java.util.HashSet[Bill]()

  @ManyToOne(cascade = Array(CascadeType.ALL),
             targetEntity = classOf[ServiceTemplate])
  @JoinColumn(name = "SERVICE_TEMPLATE_ID")
  var template : ServiceTemplate = _

  @OneToMany(mappedBy = "item",  targetEntity = classOf[Permission],
             cascade = Array(CascadeType.ALL))
  var permissions : Set[Permission] = new HashSet[Permission]()
}
