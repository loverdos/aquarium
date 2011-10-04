package gr.grnet.aquarium.logic.accounting


object InputEventType extends Enumeration {
  type InputEvent = Value
  val NetDataUp, NetDataDown, DiskSpace, VMTime = Value
}