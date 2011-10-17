package gr.grnet.aquarium.logic

import events.{EventProcessor, Event}
import gr.grnet.aquarium.model._

trait Bills {

  def calcBills() = {
    val entities = DB.findAll[Entity]("select e from Entity e")
    //entities.foreach{x => calcBills(x)}
  }

  def calcBills(e: Entity) = {
    EventProcessor.process(eventsFromDB(e))(f => true)
  }

  def eventsFromDB(e: Entity) : List[Event] = {
    val events = List()
    events
  }
}