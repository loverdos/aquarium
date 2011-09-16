package gr.grnet.aquarium.model

import javax.persistence._
import java.util.{Set, HashSet}

@Table(name = "ACTION")
@javax.persistence.Entity
class Action extends Id {

  @Column(name = "NAME")
  val name : String = ""

  @OneToMany(mappedBy = "action",  targetEntity = classOf[DefaultPermission],
             cascade = Array(CascadeType.ALL))
  val defaultPermissions : Set[DefaultPermission] = new HashSet[DefaultPermission]

  @OneToMany(mappedBy = "action",  targetEntity = classOf[Permission],
             cascade = Array(CascadeType.ALL))
  val permissions : Set[Permission] = new HashSet[Permission]
}