package gr.grnet.aquarium.logic.accounting.policies

import gr.grnet.aquarium.logic.accounting.{AccountingEvent, AccountingEntryType, Policy}

class DefaultRatePolicy(et: AccountingEntryType.Value) extends Policy(et) {

  def calculateAmount(evt: AccountingEvent) : Float = {
    evt.value() * evt.getRate()
  }
}