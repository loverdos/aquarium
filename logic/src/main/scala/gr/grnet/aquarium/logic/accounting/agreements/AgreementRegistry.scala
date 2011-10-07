package gr.grnet.aquarium.logic.accounting.agreements

import gr.grnet.aquarium.logic.accounting.Agreement

object AgreementRegistry {

  val agreements = Map (
    1L -> DefaultAgreement
  )

  def getAgreement(agrId : Long) : Option[Agreement] = {
    agreements.get(agrId)
  }
}