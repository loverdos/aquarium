package gr.grnet.aquarium.logic.accounting

import java.util.Date
import gr.grnet.aquarium.model.{Entity, DB}

class AccountingEvent(et: AccountingEventType.Value, when: Date,
                 who: Long, amount: Double, rel: List[Long]) {

  def process() = {}
  def policy() : Policy = {null}

  def getRate() : Float = {
    val ent = DB.find(classOf[Entity], who).get

    0F
  }

  
}
