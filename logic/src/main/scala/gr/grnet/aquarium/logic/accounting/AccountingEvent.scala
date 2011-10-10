package gr.grnet.aquarium.logic.accounting

import agreements.AgreementRegistry
import java.util.Date
import gr.grnet.aquarium.model.{Entity, DB}

class AccountingEvent(et: AccountingEventType.Value, when: Date,
                      who: Long, amount: Double, rel: List[Long]) {

  def date() = when

  def entity() = DB.find(classOf[Entity], who) match {
    case Some(x) => x
    case None => throw new Exception("No user with id: " + who)
  }

  def value() = amount

  def relatedEvents() = rel

  def kind() = et

  def process() = policy().process(this)

  def policy(): Policy = {
    val agr = AgreementRegistry.getAgreement(entity().agreement) match {
      case Some(x) => x
      case None => throw new Exception("No agreement with id:" +
        entity().agreement)
    }

    agr.policy(et, when) match {
      case Some(x) => x
      case None => throw new Exception("No charging policy for event type:" +
        et + " in period starting from:" + when)
    }
  }

  def getRate(): Float = {
    val agreement = AgreementRegistry.getAgreement(entity.agreement)

    agreement match {
      case Some(x) => x.pricelist.get(et).getOrElse(0)
      case None => 0
    }
  }
}
