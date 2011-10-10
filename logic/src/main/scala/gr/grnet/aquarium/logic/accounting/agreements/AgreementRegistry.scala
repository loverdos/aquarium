package gr.grnet.aquarium.logic.accounting.agreements

import gr.grnet.aquarium.logic.accounting.Agreement

object AgreementRegistry {

  var agreements = Map[Long, Agreement] (
    DefaultAgreement.id -> DefaultAgreement
  )

  def getAgreement(agrId : Long) : Option[Agreement] = {
    agreements.get(agrId)
  }

  def addAgreement[A <: Agreement](agr : A) = {
    agreements = agreements ++ Map(agr.id -> agr)
  }
}