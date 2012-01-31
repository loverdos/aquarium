package gr.grnet.aquarium.store

import com.ckkloverdos.maybe.Maybe
import gr.grnet.aquarium.logic.events.{UserEvent}

/**
 *
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait UserEventStore {

  def storeUserEvent(event: UserEvent): Maybe[RecordID]


  def findUserEventById(id: String): Maybe[UserEvent]


  def findUserEventsByUserId(userId: String): List[UserEvent]
}