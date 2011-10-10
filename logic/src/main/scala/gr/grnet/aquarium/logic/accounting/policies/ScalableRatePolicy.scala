package gr.grnet.aquarium.logic.accounting.policies

import gr.grnet.aquarium.logic.accounting.{AccountingEventType, AccountingEvent, Policy, AccountingEntryType}

class ScalableRatePolicy(et: AccountingEntryType.Value) extends Policy(et) {

   def calculateAmount(evt: AccountingEvent) : Float = {
    evt.kind() match {
      case AccountingEventType.VMTime =>
        if (evt.value() <= 25)
          evt.value() * evt.getRate() * 1
        else if (evt.value() > 25 && evt.value() < 50)
          evt.value() * evt.getRate() * 1.2F
        else
          evt.value() * evt.getRate() * 2
      case AccountingEventType.DiskSpace =>
        if (evt.value() <= 100)
          evt.value() * evt.getRate() * 1
        else if (evt.value() > 150 && evt.value() < 200)
          evt.value() * evt.getRate() * 1.2F
        else
          evt.value() * evt.getRate() * 2
      case AccountingEventType.NetDataUp =>
        if (evt.value() <= 1000)
          evt.value() * evt.getRate() * 1
        else if (evt.value() > 1500 && evt.value() < 2000)
          evt.value() * evt.getRate() * 1.2F
        else
          evt.value() * evt.getRate() * 2
      case AccountingEventType.NetDataDown =>
        if (evt.value() <= 500)
          evt.value() * evt.getRate() * 1
        else if (evt.value() > 1000 && evt.value() < 1500)
          evt.value() * evt.getRate() * 1.2F
        else
          evt.value() * evt.getRate() * 2
    }
  }
}