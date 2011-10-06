package gr.grnet.aquarium.logic.accounting


object AccountingEventType extends Enumeration {
  type InputEvent = Value
  val NetDataUp, NetDataDown, DiskSpace, VMTime = Value
}