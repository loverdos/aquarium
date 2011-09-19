package gr.grnet.aquarium.model

import java.util.{Set, HashSet}
import javax.persistence._

@NamedQueries(Array(
  new NamedQuery(name="allSi", query="select si from ServiceItem si"),
  new NamedQuery(name="siPerEntity",
                 query="select si from ServiceItem si where si.owner = :owner")
))
@javax.persistence.Entity
@Table(name = "SERVICE_ITEM")
class ServiceItem extends Id {

  @Column(name = "URL")
  var url : String = ""

  @ManyToOne(cascade = Array(CascadeType.ALL),
             targetEntity = classOf[gr.grnet.aquarium.model.Entity])
  @JoinColumn(name = "ENTITY_ID")
  var owner : gr.grnet.aquarium.model.Entity  = _

  @OneToMany(mappedBy = "item",  targetEntity = classOf[ServiceItemConfig],
             cascade = Array(CascadeType.ALL))
  var configItems : Set[ServiceItemConfig] = new HashSet[ServiceItemConfig]()

  @OneToMany(mappedBy = "item",  targetEntity = classOf[Bill],
             cascade = Array(CascadeType.ALL))
  var bill : Set[Bill] = new HashSet[Bill]()

  @ManyToOne(cascade = Array(CascadeType.ALL),
             targetEntity = classOf[ServiceTemplate])
  @JoinColumn(name = "SERVICE_TEMPLATE_ID")
  var template : ServiceTemplate = _

  @OneToMany(mappedBy = "item",  targetEntity = classOf[Permission],
             cascade = Array(CascadeType.ALL))
  var permissions : Set[Permission] = new HashSet[Permission]()
}
