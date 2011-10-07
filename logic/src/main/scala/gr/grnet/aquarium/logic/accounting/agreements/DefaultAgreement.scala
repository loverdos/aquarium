package gr.grnet.aquarium.logic.accounting.agreements

import gr.grnet.aquarium.logic.accounting.{AccountingEventType, Agreement}

object DefaultAgreement extends Agreement {

  val pricelist = Map(
    AccountingEventType.DiskSpace -> 0.00002,
    AccountingEventType.NetDataDown -> 0.0001,
    AccountingEventType.NetDataUp -> 0.0001,
    AccountingEventType.VMTime -> 0.001
  )
}