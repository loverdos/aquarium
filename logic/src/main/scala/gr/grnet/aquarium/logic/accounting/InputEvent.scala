package gr.grnet.aquarium.logic.accounting

import java.util.Date
import gr.grnet.aquarium.model.{Entity, DB}

class InputEvent(et: InputEventType.Value, when: Date,
                 who: Long, amount: Double) {

  def process() = {}

  def findRule(): Policy = {
    val e = DB.find[Entity](classOf[Entity], who)
    null
    //who.agreement.policy(et, when)
  }
}

