package gr.grnet.aquarium.logic.accounting

import java.util.Date

abstract class Agreement {
  def policy(et: EventType.Value, d: Date) : Policy
}