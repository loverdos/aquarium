package gr.grnet.aquarium.store

import com.ckkloverdos.maybe.Maybe
import gr.grnet.aquarium.logic.events.{UserEvent, AquariumEvent}

/**
 * Store for IM events
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait IMStore {

  def storeUserEvent(event: UserEvent): Maybe[RecordID]

  def findUserEventById(id: String): Option[UserEvent]

  def findUserEventsByUserId[A <: AquariumEvent](userId: Long)(sortWith: Option[(A, A) => Boolean]): List[A]

  def findLastUserEvent(userId: String): Option[UserEvent]

  def userExists(userId: String): Boolean
}