package gr.grnet.aquarium.logic.accounting

object AccountingEntryType extends Enumeration {
  type InputEvent = Value
  val STORAGE_CHARGE, NET_CHARGE, DEBIT, VMTIME_CHARGE = Value
}
