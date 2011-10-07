package gr.grnet.aquarium.logic.accounting


abstract class Policy(et: AccountingEntryType.Value) {

  private def makeEntry(event: AccountingEvent, amount: Float) = {
    val entry = new AccountingEntry(event.relatedEvents(),
                                    event.date(), amount, et)
  }

  def process(evt: AccountingEvent) = {
    makeEntry(evt, calculateAmount(evt))
  }

  def calculateAmount(evt: AccountingEvent) : Float
}