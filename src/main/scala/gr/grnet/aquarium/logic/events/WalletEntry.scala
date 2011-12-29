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
    override val id: String,
    override val occurredMillis: Long,
    sourceEventIDs: List[String], // The events that triggered this WalletEntry
    value: Float,
    reason: String,
    userId: String,
    finalized: Boolean)
  extends AquariumEvent(id, occurredMillis) {

  assert(occurredMillis > 0)
  assert(value > 0F)
  assert(!userId.isEmpty)

  def validate = true
}

object WalletEntry {
  def fromJson(json: String): WalletEntry = {
    JsonHelpers.jsonToObject[WalletEntry](json)
  }

  def zero = WalletEntry("", 1L, Nil,1,"","foo", false)
}