package gr.grnet.aquarium.logic.accounting

import java.util.Date

/**The result of processing an accounting event*/
class AccountingEntry(sourceEvents: List[Long], when: Date,
                      amnt: Float, et: AccountingEntryType.Value) {
  val events = sourceEvents
  val amount = amnt
  val entryType = et
}
