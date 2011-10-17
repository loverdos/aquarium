package gr.grnet.aquarium.logic.accounting

import agreements.AgreementRegistry
import gr.grnet.aquarium.model.{Entity, DB}
import policies.DefaultRatePolicy
import java.util.Date


/** An accounting event represents any event that the accounting system
 *  must process.
 */
class AccountingEvent(et: AccountingEventType.Value, start: Date,
                      end: Date, who: Long, amount: Float, rel: List[Long]) {

  def dateStart() = start
  def dateEnd() = end

  def entity() = DB.find(classOf[Entity], who) match {
    case Some(x) => x
    case None => throw new Exception("No user with id: " + who)
  }

  def value() = amount

  def relatedEvents() = rel

  def kind() = et

  def process() = {
    val evts =  policy().map{p => p.process(this)}
    val total = evts.map(x => x.amount).foldLeft(0F)((a, sum) => a + sum)
    new AccountingEntry(rel, new Date(), total, evts.head.entryType)
  }

  def policy(): List[Policy] = {
    val agr = AgreementRegistry.getAgreement(entity().agreement) match {
      case Some(x) => x
      case None => throw new Exception("No agreement with id:" +
        entity().agreement)
    }

    /*agr.policy(et, when) match {
      case Some(x) => x
      case None => throw new Exception("No charging policy for event type:" +
        et + " in period starting from:" + when.getTime)
    }*/
    List()
  }

  def getRate(): Float = {
    val agreement = AgreementRegistry.getAgreement(entity.agreement)

    agreement match {
      case Some(x) => x.pricelist.get(et).getOrElse(0)
      case None => 0
    }
  }
}
