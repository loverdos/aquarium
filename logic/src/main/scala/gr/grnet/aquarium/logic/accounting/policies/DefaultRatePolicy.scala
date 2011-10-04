package gr.grnet.aquarium.logic.accounting.policies

import gr.grnet.aquarium.logic.accounting.{InputEventType, AccountingEvent, Policy}
import collection.immutable.HashMap

object DefaultRatePolicy extends Policy {

  val rates = new HashMap[InputEventType.Value, Float]

  private def init() = {
    
  }
  
  def calculateAmount(evt: AccountingEvent) : Float = {
    if (rates.isEmpty) init()

    0F
  }
}