package gr.grnet.aquarium.logic.accounting

import agreements.AgreementRegistry
import java.util.Date
import gr.grnet.aquarium.model.{Entity, DB}

class AccountingEvent(et: AccountingEventType.Value, when: Date,
                 who: Long, amount: Double, rel: List[Long]) {

  def date() = when
  def entity() = DB.find(classOf[Entity], who)
  def value() = amount
  def relatedEvents() = rel
  def kind() = et

  def process() = {}
  def policy() : Policy = {null}

  def getRate() : Float = {
    val ent = DB.find(classOf[Entity], who).get
    val agreement = AgreementRegistry.getAgreement(ent.agreement)

    agreement match {
      case Some(x) => x.pricelist.get(et).getOrElse(0)
      case None => 0
    }
  }
}
