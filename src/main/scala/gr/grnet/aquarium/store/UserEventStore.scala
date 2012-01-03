package gr.grnet.aquarium.store

import com.ckkloverdos.maybe.Maybe
import gr.grnet.aquarium.logic.events.{ResourceEvent, UserEvent}

/**
 * * An abstraction for Aquarium user event stores.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait UserEventStore {

  def storeUserEvent(event: UserEvent): Maybe[RecordID]

  def findUserEventById(id: String): Maybe[UserEvent]

  def findUserEventsByUserId(userId: String)
                            (sortWith: Option[(UserEvent, UserEvent) => Boolean]): List[UserEvent]
}