package gr.grnet.aquarium.logic.accounting


abstract class Policy {

  abstract def calculateAmount(evt: AccountingEvent) : Float
}