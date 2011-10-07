package gr.grnet.aquarium.logic.accounting


object AccountingEventType extends Enumeration {
  type AcountingEvent = Value
  val NetDataUp, NetDataDown, DiskSpace, VMTime = Value
}