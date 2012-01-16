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
    override val occurredMillis: Long, // The time of oldest matching resource event
    override val receivedMillis: Long, // The time the cost calculation was done
    sourceEventIDs: List[String],      // The events that triggered this WalletEntry
    value: Float,
    reason: String,
    userId: String,
    resource: String,
    instanceId: String,
    finalized: Boolean)
  extends AquariumEvent(id, occurredMillis, receivedMillis) {

  assert(occurredMillis > 0)
  assert(value >= 0F)
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

  def zero = WalletEntry("", 1L, 1L, Nil,1,"","foo", "bar", "0", false)

  object JsonNames {
    final val _id = "_id"
    final val id = "id"
    final val occurredMillis = "occurredMillis"
    final val receivedMillis = "receivedMillis"
    final val sourceEventIDs = "sourceEventIDs"
    final val value = "value"
    final val reason = "reason"
    final val userId = "userId"
    final val resource = "resource"
    final val instanceId = "instanceId"
    final val finalized = "finalized"
  }
}