package gr.grnet.aquarium.model

import javax.persistence._
import java.util.{Set, HashSet}

@javax.persistence.Entity
@Table(name = "ENTITY_TYPE")
class EntityType extends Id {

  val USER = 0x1
  val GROUP = 0x2
  val ORGANIZATION = 0x4

  @Column(name = "TYPE")
  var entType : Int = 0

  @Column(name = "NAME")
  var name : String = _

  @OneToMany(mappedBy = "enttype",  targetEntity = classOf[DefaultPermission],
             cascade = Array(CascadeType.ALL))
  var defPerms : Set[DefaultPermission] = new HashSet[DefaultPermission]()
}