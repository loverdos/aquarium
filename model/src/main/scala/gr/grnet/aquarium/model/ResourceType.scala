package gr.grnet.aquarium.model

import javax.persistence._

@javax.persistence.Entity
@Table(name = "RESOURCE_TYPE")
class ResourceType extends Id {

  val CPU = 0x1
  val RAM = 0x2
  val DISKSPACE = 0x4
  val NETBANDWIDTH = 0x8
  val LICENCE = 0x10

  @Column(name = "TYPE")
  var resType : Int = 0

  @Column(name = "NAME")
  var name : String = _
}
