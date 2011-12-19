package gr.grnet.aquarium.logic.events

import gr.grnet.aquarium.util.json.JsonHelpers
import net.liftweb.json.{Extraction, parse => parseJson}
import gr.grnet.aquarium.MasterConf._

/**
 * Represents an incoming user event.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
case class UserEvent(
  override val id: String,
  override val timestamp: Long,
  userId: String,
  eventVersion: Short,
  eventType: Short, //1: create, 2: modify
  state: String,    //ACTIVE, SUSPENDED
  idp: String,
  tenant: String,
  roles: Array[String]
  ) extends AquariumEvent(id, timestamp) {

  assert(eventType == 1 || eventType == 2)
  assert(state.equalsIgnoreCase("ACTIVE") ||
    state.equalsIgnoreCase("SUSPENDED"))

  /**
   * Validate this event according to the following rules:
   *
   * Valid event states: `(eventType, state)`:
   *  - `a := 1, ACTIVE`
   *  - `b := 2, ACTIVE`
   *  - `c := 2, SUSPENDED`
   *
   * Valid transitions:
   *  - `(non-existent) -> a`
   *  - `a -> c`
   *  - `c -> b`
   */
  def validate: Boolean = {

    if (eventType == 1) {
      if (MasterConf.IMStore.userExists(userId))
        return false

      if (!state.equalsIgnoreCase("ACTIVE")) return false
      return true
    }

    // All user events are of type 2 (modify) from hereon
    val oldEvent = MasterConf.IMStore.findLastUserEvent(this.userId)

    oldEvent match {
      case Some(x) =>
        x.state match {
          case y if (y.equalsIgnoreCase("SUSPENDED")) => this.state match {
            case z if (z.equalsIgnoreCase("ACTIVE")) =>  //OK
            case _ => return false
          }
          case y if (y.equalsIgnoreCase("ACTIVE")) =>  this.state match {
            case z if (z.equalsIgnoreCase("SUSPENDED")) =>
              if (x.eventType == 2) return false
            case _ => return false
          }
        }
      case None => return false
    }

    true
  }
}

object UserEvent {
  def fromJson(json: String): ResourceEvent = {
    implicit val formats = JsonHelpers.DefaultJsonFormats
    val jsonAST = parseJson(json)
    Extraction.extract[ResourceEvent](jsonAST)
  }
}
