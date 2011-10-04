package gr.grnet.aquarium.logic.accounting


abstract class Policy {

  def calculateAmount(evt: AccountingEvent) : Float
}