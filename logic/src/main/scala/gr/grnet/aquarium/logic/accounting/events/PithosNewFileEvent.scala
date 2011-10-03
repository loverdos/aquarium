package gr.grnet.aquarium.logic.accounting.events

import java.util.Date
import gr.grnet.aquarium.logic.accounting.{User, EventType, InputEvent}

class PithosNewFileEvent(when: Date, who: User, amount: Float)
  extends InputEvent(EventType.PithosNewFile, when, who) {

  def process() = {
    
  }
}