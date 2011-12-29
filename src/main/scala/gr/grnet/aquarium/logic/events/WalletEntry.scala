package gr.grnet.aquarium.logic.events

import gr.grnet.aquarium.util.json.JsonHelpers

/**
 * A WalletEntry is a derived entity. Its data represent money/credits and are calculated based on
 * resource events.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class WalletEntry(
    override val id: String,           // The id at the client side (the sender) TODO: Rename to remoteId or something...
    override val occurredMillis: Long, // When it occurred at client side (the sender)
    override val receivedMillis: Long, // When it was received by Aquarium
    sourceEventIDs: List[String],      // The events that triggered this WalletEntry
    value: Float,
    reason: String,
    userId: String,
    finalized: Boolean)
  extends AquariumEvent(id, occurredMillis, receivedMillis) {

  assert(occurredMillis > 0)
  assert(value > 0F)
  assert(!userId.isEmpty)

  def validate = true
}

object WalletEntry {
  def fromJson(json: String): WalletEntry = {
    JsonHelpers.jsonToObject[WalletEntry](json)
  }

  def zero = WalletEntry("", 1L, 1L, Nil,1,"","foo", false)
}