package gr.grnet.aquarium.model

import javax.persistence._
import java.util.{HashSet, Set}

@Table(name = "SERVICE_TEMPLATE")
@javax.persistence.Entity
class ServiceTemplate extends Id {

  @Column(name = "NAME")
  var name : String = _

  @OneToMany(mappedBy = "template",  targetEntity = classOf[DefaultPermission],
             cascade = Array(CascadeType.ALL))
  var permissions : Set[DefaultPermission] = new HashSet[DefaultPermission]()

  @OneToMany(mappedBy = "template",  targetEntity = classOf[ServiceItem],
             cascade = Array(CascadeType.ALL))
  var items : Set[ServiceItem] = new HashSet[ServiceItem]()

  @ManyToMany(targetEntity = classOf[ResourceType],
              cascade = Array(CascadeType.ALL))
  @JoinTable(name="SERVICE_TEMPLATE_RESOURCES",
             joinColumns = Array(new JoinColumn(name="RESOURCE_TYPE_ID")),
             inverseJoinColumns = Array(new JoinColumn(name="RESOURCE_ID")))
  var resTypes : Set[ResourceType] = new HashSet[ResourceType]()
}