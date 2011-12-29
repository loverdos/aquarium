package gr.grnet.aquarium.logic.events

import gr.grnet.aquarium.util.json.JsonHelpers
import net.liftweb.json.{Extraction, parse => parseJson}
import gr.grnet.aquarium.MasterConf._
import com.ckkloverdos.maybe.{Failed, NoVal, Just}

/**
 * Represents an incoming user event.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
case class UserEvent(
  override val id: String,
  override val occurredMillis: Long,
  var aqTimestamp: Long = 0,
  userId: String,
  eventVersion: Short,
  eventType: Short, //1: create, 2: modify
  state: String,    //ACTIVE, SUSPENDED
  idp: String,
  tenant: String,
  roles: Array[String]
  ) extends AquariumEvent(id, occurredMillis) {

  assert(eventType == 1 || eventType == 2)
  assert(state.equalsIgnoreCase("ACTIVE") ||
    state.equalsIgnoreCase("SUSPENDED"))

  if (eventType == 1)
    if(!state.equalsIgnoreCase("ACTIVE"))
      assert(false)

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

    MasterConf.userStore.findUserStateByUserId(userId) match {
      case Just(x) =>
        if (eventType == 1){
          logger.warn("User to create exists: IMEvent".format(this.toJson));
          return false
        }
      case NoVal =>
        if (eventType != 2){
          logger.warn("Inexistent user to modify. IMEvent:".format(this.toJson))
          return false
        }
      case Failed(x,y) =>
        logger.warn("Error retrieving user state: %s".format(x))
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
