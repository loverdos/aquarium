package gr.grnet.aquarium.logic.accounting

import java.util.Date

abstract class InputEvent(et: EventType.Value, when: Date, who: User) {

  def process()

  def findRule(): Policy = who.agreement.policy(et, when)
}