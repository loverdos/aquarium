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

  @ManyToMany(targetEntity = classOf[ServiceTemplate],
              mappedBy = "resTypes",
              cascade= Array(CascadeType.ALL))
  var srvTemplates : Set[ServiceTemplate] = new HashSet[ServiceTemplate]()

  @OneToMany(mappedBy = "restype",  targetEntity = classOf[PriceList],
             cascade = Array(CascadeType.ALL))
  var agreements : Set[PriceList] = new HashSet[PriceList]()
}

object ResourceType {
  def VMTIME       = getResourceByType(0x1, "CPU")
  def DISKSPACE    = getResourceByType(0x2, "DISKSPACE")
  def NETBANDWIDTH = getResourceByType(0x4, "NETBANDWIDTH")
  def LICENSE      = getResourceByType(0x8, "LICENSE")

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