package gr.grnet.aquarium.logic.events

import gr.grnet.aquarium.logic.accounting.{InputEventType, InputEvent}
import java.util.Date

object EventProcessor {

  def process(from: Option[Date], to: Option[Date],
              events: (Option[Date], Option[Date]) => List[Event]): List[InputEvent] = {
    val evts = events(from, to)

    class DummyEvent(et: InputEventType.Value, when: Date,
                     who: Long) extends InputEvent(et, when, who, 0)

    evts.map {f => f.who}.distinct.map {
      //Events are calculated per user
      u =>
      //Time sorted list of events for user
        val userEvents = evts.filter(_.who == u).sortBy(_.when)
        userEvents.map {
          e => e match {
            case v: VMStarted => //Find stop event and calculate time usage
              val time = findVMStopEvent(userEvents, v) match {
                case Some(x) => x.when
                case None => to.getOrElse(new Date()) //Now
              }
              new InputEvent(InputEventType.VMTime, e.when(),
                             u, time.getTime - v.w.getTime)
            //          case v : VMStarted =>None
            //          case v : VMStopped =>None
            case v: DiskSpaceChanged =>
              new InputEvent(InputEventType.DiskSpace, e.when(), u, v.bytes)
            case v: DataUploaded =>
              new InputEvent(InputEventType.NetDataUp, e.when, u, v.bytes)
            case v: DataDownloaded =>
              new InputEvent(InputEventType.NetDataDown, e.when, u, v.bytes)
            //          case v : SSaasVMCreated => None
            //          case v : SSaasVMStarted =>None
            //          case v : SSaasVMStopped =>None
            case _ => new DummyEvent(InputEventType.DiskSpace, e.when(), u)
          }
        }
    }.flatten.filter(p => !p.isInstanceOf[DummyEvent])
  }

  /**Find a the first corresponding VM stop event in a list of messages*/
  def findVMStopEvent(events: List[Event], v: VMStarted): Option[VMStopped] = {
    events.find {
      f => (
        f.id() == v.id() &&
          f.who() == v.who &&
          f.when().compareTo(v.when()) > 0) &&
        f.isInstanceOf[VMStopped]
    }.asInstanceOf[Option[VMStopped]]
  }
}