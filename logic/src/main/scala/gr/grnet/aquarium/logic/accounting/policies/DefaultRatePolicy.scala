package gr.grnet.aquarium.logic.accounting.policies

import gr.grnet.aquarium.logic.accounting.{AccountingEventType, AccountingEntry, Policy}
import collection.immutable.HashMap

object DefaultRatePolicy extends Policy {

  val rates = new HashMap[AccountingEventType.Value, Float]

  private def init() = {
    
  }
  
  def calculateAmount(evt: AccountingEntry) : Float = {
    if (rates.isEmpty) init()

    0F
  }
}