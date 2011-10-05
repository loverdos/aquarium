package gr.grnet.aquarium.logic.events

import gr.grnet.aquarium.logic.accounting.{InputEventType, InputEvent}
import java.util.Date

object EventProcessor {

  def process(from: Date, to: Date,
              events: (Date, Date) => List[Event]) : List[InputEvent] = {
    val evts = events(from, to)

    evts.map{f => f.who}.distinct.map { //Events are calculated per user
      u =>
        //Time sorted list of events for user
        val userEvents = evts.filter(_.who == u).sortBy(_.when)
        userEvents.map {
        e => e match {
          case v : VMCreated => //Find stop event and calculate time usage
            val time = findVMStopEvent(userEvents, v) match {
              case Some(x) => x.when
              case None => to
            }
            new InputEvent(InputEventType.VMTime, e.when(), u, time.getTime - v.w.getTime)
//          case v : VMStarted =>None
//          case v : VMStopped =>None
          case v : DiskSpaceChanged => new InputEvent(InputEventType.DiskSpace, e.when(), u, v.bytes)
          case v : DataUploaded => new InputEvent(InputEventType.NetDataUp, e.when, u, v.bytes)
          case v : DataDownloaded => new InputEvent(InputEventType.NetDataDown, e.when, u, v.bytes)
//          case v : SSaasVMCreated => None
//          case v : SSaasVMStarted =>None
//          case v : SSaasVMStopped =>None
//          case _  => None
        }
      }
    }.flatten
  }

  /** Find a the first corresponding VM stop event in a list of messages*/
  def findVMStopEvent(events: List[Event], v: VMCreated) : Option[VMStopped] = {
    events.find{
      f => (
        f.id() == v.id() &&
        f.who() == v.who &&
        f.when().compareTo(v.when()) > 0) &&
        f.isInstanceOf[VMStopped]
    }.asInstanceOf[Option[VMStopped]]
  }

  def getEvents(from: Date, to: Date): List[Event] = {
    //Tmp list of events
    List[Event](
      new VMCreated(1, new Date(123), 2, 1),
      new VMStarted(2, new Date(123), 2, 1),
      new VMCreated(3, new Date(125), 2, 2),
      new DiskSpaceChanged(4, new Date(122), 2, 1554),
      new DataUploaded(5, new Date(122), 2, 1554),
      new DiskSpaceChanged(6, new Date(122), 1, 1524),
      new DataUploaded(7, new Date(122), 1, 1524),
      new DiskSpaceChanged(8, new Date(122), 1, 1332)
    )
  }
}