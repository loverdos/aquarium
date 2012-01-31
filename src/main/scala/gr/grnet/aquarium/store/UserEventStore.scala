package gr.grnet.aquarium.store

import com.ckkloverdos.maybe.Maybe
import gr.grnet.aquarium.logic.events.{UserEvent}

/**
 * Store for external user events
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait UserEventStore {

  /**
   * Store an event
   */
  def storeUserEvent(event: UserEvent): Maybe[RecordID]

  /**
   * Find a user event by event ID
   */
  def findUserEventById(id: String): Maybe[UserEvent]

  /**
   * Find all user events by user ID
   */
  def findUserEventsByUserId(userId: String): List[UserEvent]
}