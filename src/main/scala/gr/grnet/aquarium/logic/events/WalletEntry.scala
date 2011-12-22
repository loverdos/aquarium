package gr.grnet.aquarium.logic.events

import gr.grnet.aquarium.util.json.JsonHelpers
import net.liftweb.json._

/**
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
case class WalletEntry(override val id: String,
                       override val timestamp: Long,
                       related: Array[String],
                       value: Float,
                       reason: String,
                       userId: String,
                       finalized: Boolean)
  extends AquariumEvent(id, timestamp) {

  assert(timestamp > 0)
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