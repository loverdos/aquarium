package gr.grnet.aquarium.logic.accounting

object EventType extends Enumeration {
  type EventType = Value
  val PithosNewFile, PithosFileDelete, SynnefoStartVM,
  SynnefoEndVM, SynnefoCreateVM = Value
}
