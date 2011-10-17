package gr.grnet.aquarium.logic.events

import gr.grnet.aquarium.logic.accounting.{AccountingEventType, AccountingEvent}
import java.util.Date

object EventProcessor {

  def process(events: List[Event])
             (evtFilter: (Event) => Boolean): List[AccountingEvent] = {

    val evts = events.filter(evtFilter)
    val dummy = new AccountingEvent(AccountingEventType.VMTime, new Date(), new Date(), 0, 0, List())

    evts.sortBy(_.when).map {
      e =>
        val u = e.who()
        e match {
          case v: VMStarted => //Find stop event and calculate time usage
            val stop = findVMStopEvent(evts, v)
            val time = stop match {
              case Some(x) => x.when
              case None => new Date() //Now
            }
            val stopid = stop match {
              case Some(x) => x.id
              case None => -1 
            }
            val totaltime = time.getTime - v.w.getTime
            assert(totaltime > 0)
            new AccountingEvent(AccountingEventType.VMTime, e.when, time, u, totaltime, List(v.id, stopid))
          //          case v : VMStarted =>None
          //          case v : VMStopped =>None
          case v: DiskSpaceChanged =>
            assert(v.bytes > 0)
            new AccountingEvent(AccountingEventType.DiskSpace, e.when, e.when, u, v.bytes, List(v.id()))
          case v: DataUploaded =>
            assert(v.bytes > 0)
            new AccountingEvent(AccountingEventType.NetDataUp, e.when, e.when, u, v.bytes, List(v.id()))
          case v: DataDownloaded =>
            assert(v.bytes > 0)
            new AccountingEvent(AccountingEventType.NetDataDown, e.when, e.when, u, v.bytes, List(v.id()))
          //          case v : SSaasVMCreated => None
          //          case v : SSaasVMStarted =>None
          //          case v : SSaasVMStopped =>None
          case _ => dummy
        }
    }.filter(p => p != dummy) //Remove dummy events
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