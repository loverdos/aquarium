package gr.grnet.aquarium.logic.events

import java.util.Date
import gr.grnet.aquarium.logic.accounting.InputEvent

object EventProcessor {

  def process(from: Date, to: Date) : List[InputEvent] = {
    List[InputEvent]()
  }

  /** Reads events from event store */
  def getEvents(from: Date, to: Date): List[Event] = {
    //Tmp list of events
    List[Event](
      new VMCreated(1, new Date(123), 2, 1),
      new VMStarted(2, new Date(123), 2, 1),
      new VMCreated(3, new Date(125), 2, 2),
      new FileCreated(4, new Date(122), 2, "/a/foo", 1554),
      new DataUploaded(5, new Date(122), 2, 1554),
      new FileCreated(6, new Date(122), 1, "/a/bar", 1524),
      new DataUploaded(7, new Date(122), 1, 1524),
      new FileModified(8, new Date(122), 1, "/a/foo", 1332)
    )
  }
}