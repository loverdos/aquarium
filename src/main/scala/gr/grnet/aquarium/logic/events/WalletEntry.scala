package gr.grnet.aquarium.logic.events

import gr.grnet.aquarium.util.json.JsonHelpers

/**
 * A WalletEntry is a derived entity. Its data represent money/credits and are calculated based on
 * resource events.
 *
 * Wallet entries give us a picture of when credits are calculated from resource events.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class WalletEntry(
    override val id: String,           // The id at the client side (the sender) TODO: Rename to remoteId or something...
    // When it occurred at client side (the sender).
    // This denotes the `occurredMillis` attribute of the oldest resource event that is referenced
    // by `sourceEventIDs`. The reason for this convention is pure from a query-oriented point of view.
    // See how things are calculated in `UserActor`.
    override val occurredMillis: Long,
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
  
  def fromResourceEvent(rceId: String): Boolean = {
    sourceEventIDs contains rceId
  }

  def setRcvMillis(millis: Long) = copy(receivedMillis = millis)
}

object WalletEntry {
  def fromJson(json: String): WalletEntry = {
    JsonHelpers.jsonToObject[WalletEntry](json)
  }

  def zero = WalletEntry("", 1L, 1L, Nil,1,"","foo", false)
}