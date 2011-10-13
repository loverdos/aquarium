package gr.grnet.aquarium.model

import javax.persistence._

case class PriceListKey(agreement_id: Long, restype_id: Long)

@javax.persistence.Entity
@IdClass(classOf[PriceListKey])
@Table(name = "PRICELIST")
class PriceList {
  @javax.persistence.Id
  @Column(name = "AGREEMENT_ID", nullable = false,
          updatable = false, insertable = false)
  var agreement_id : Long = 0L

  @javax.persistence.Id
  @Column(name = "RESTYPE_ID", nullable = false,
          updatable = false, insertable = false)
  var restype_id : Long = 0L

  @Column(name = "PRICE")
  var price : Float = 0F

  @ManyToOne(cascade = Array(CascadeType.ALL),
             targetEntity = classOf[gr.grnet.aquarium.model.Agreement])
  @JoinColumn(name = "AGREEMENT_ID")
  var agreement: Agreement = _

  @ManyToOne(cascade = Array(CascadeType.ALL),
             targetEntity = classOf[gr.grnet.aquarium.model.ResourceType])
  @JoinColumn(name = "RESTYPE_ID")
  var restype: ResourceType = _
}