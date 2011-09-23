package gr.grnet.aquarium.model

import javax.persistence._
import java.util.{HashSet, Set}

@javax.persistence.Entity
@Table(name = "RESOURCE_TYPE")
@NamedQueries(Array(
  new NamedQuery(name = "restypeFromType",
                 query = "select rt from ResourceType rt where rt.resType = :type")
))
class ResourceType extends Id {

  @Column(name = "TYPE")
  var resType : Int = 0

  @Column(name = "NAME")
  var name : String = ""

  @OneToMany(mappedBy = "restype",  targetEntity = classOf[ConsumableResource],
             cascade = Array(CascadeType.ALL))
  var consResources : Set[ConsumableResource] = new HashSet[ConsumableResource]()

  @ManyToMany(targetEntity = classOf[ServiceTemplate],
              mappedBy = "resTypes",
              cascade= Array(CascadeType.ALL))
  var srvTemplates : Set[ServiceTemplate] = new HashSet[ServiceTemplate]()
}

object ResourceType {
  def CPU          = getResourceByType(0x1, "CPU")
  def RAM          = getResourceByType(0x2, "RAM")
  def DISKSPACE    = getResourceByType(0x4, "DISKSPACE")
  def NETBANDWIDTH = getResourceByType(0x8, "NETBANDWIDTH")
  def LICENSE      = getResourceByType(0x10, "LICENSE")

  private def getResourceByType(restype : Int, name : String) : ResourceType = {
    val res = DB.findAll[ResourceType]("restypeFromType",
                                       "type" -> restype).headOption
    if (!res.isEmpty)
      return res.get

    val rt = new ResourceType
    rt.resType = restype
    rt.name = name

    DB.persist(rt)
    rt
  }
}