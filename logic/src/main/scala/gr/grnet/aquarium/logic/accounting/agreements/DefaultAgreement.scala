package gr.grnet.aquarium.logic.accounting.agreements

import gr.grnet.aquarium.logic.accounting.{AccountingEventType, Agreement}

object DefaultAgreement extends Agreement {

  override val id = 1L

  override val pricelist = Map (
    AccountingEventType.DiskSpace -> 0.00002F,
    AccountingEventType.NetDataDown -> 0.0001F,
    AccountingEventType.NetDataUp -> 0.0001F,
    AccountingEventType.VMTime -> 0.001F
  )
}