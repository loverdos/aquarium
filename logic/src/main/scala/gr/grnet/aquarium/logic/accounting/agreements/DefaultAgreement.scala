package gr.grnet.aquarium.logic.accounting.agreements

import java.util.Date
import gr.grnet.aquarium.logic.accounting.{InputEventType, Policy, Agreement}

class DefaultAgreement extends Agreement {

  def policy(et: InputEventType.Value, d: Date) : Policy = {
    null
  }
}