package gr.grnet.aquarium.logic.events

import gr.grnet.aquarium.util.json.JsonHelpers
import net.liftweb.json._

/**
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class WalletEntry(override val id: String,
                       override val occurredMillis: Long,
                       sourceEventIDs: Array[String], // The events that triggered this WalletEntry
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
  def fromJson(json: String): ResourceEvent = {
    implicit val formats = JsonHelpers.DefaultJsonFormats
    val jsonAST = parse(json)
    Extraction.extract[ResourceEvent](jsonAST)
  }

  def zero = WalletEntry("", 1L, Array(),1,"","foo", false)
}