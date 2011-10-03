package gr.grnet.aquarium.logic.accounting.agreements

import java.util.Date
import gr.grnet.aquarium.logic.accounting.{Policy, EventType, Agreement}

class DefaultAgreement extends Agreement {

  

  def policy(et: EventType.Value, d: Date) : Policy = {
    null
  }
}