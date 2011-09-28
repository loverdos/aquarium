package gr.grnet.aquarium.model

import javax.persistence._
import java.util.{Set, HashSet}

@Table(name = "CONSUMABLE_RESOURCE")
@javax.persistence.Entity
class ConsumableResource extends Id {

  @Column(name = "NAME")
  var name : String = ""

  @ManyToOne (cascade = Array(CascadeType.ALL),
              targetEntity = classOf[ResourceType])
  @JoinColumn(name = "RESOURCE_TYPE_ID")
  var restype : ResourceType = _

  @Column(name = "UNIT")
  var unit : String = ""

  @Column(name = "PERIOD")
  var period : String = ""

  @Column(name = "COST")
  var cost : Float = 0.0F

  @OneToMany(mappedBy = "resource",  targetEntity = classOf[ServiceItemConfig],
             cascade = Array(CascadeType.ALL))
  var configItems : Set[ServiceItemConfig] = new HashSet[ServiceItemConfig]()
}

case class Period(sec : Long, name : String) {
  def toMonth : Float = sec / (3600 * 24 * 30)
  def toWeek : Float = sec / (3600 * 24 * 30)
  def toDay : Float = sec / (3600 * 24 * 30)
}

object Period {
  def MONTH = Period(3600 * 24 * 30, "MONTH")
  def WEEK  = Period(3600 * 24 * 7, "WEEK")
  def DAY   = Period(3600 * 24, "DAY")
  def HOUR  = Period(3600, "HOUR")
  def MIN   = Period(60, "MIN")
  def SEC   = Period(1, "SEC")
}
