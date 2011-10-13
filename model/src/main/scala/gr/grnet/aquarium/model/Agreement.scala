package gr.grnet.aquarium.model

import java.util.{Set, HashSet, Date}
import javax.persistence._

@Table(name = "Agreement")
@javax.persistence.Entity
class Agreement extends Id {

  @Column(name = "NAME")
  var name : String = ""

  @Column(name = "DESCR")
  var descr: String = ""

  @Column(name = "VALID_FROM")
  @Temporal(value = TemporalType.DATE)
  var validFrom: Date = new Date()

  @Column(name = "VALID_UNTIL")
  @Temporal(value = TemporalType.DATE)
  var validUntil: Date = new Date()

  @OneToMany(mappedBy = "agreement",  targetEntity = classOf[PriceList],
             cascade = Array(CascadeType.ALL))
  var prices : Set[PriceList] = new HashSet[PriceList]()
}