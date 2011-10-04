package gr.grnet.aquarium.logic.events

import net.liftweb.json.Serialization.write
import java.util.Date

/**Generic event type*/
trait Event {

  implicit val formats = net.liftweb.json.DefaultFormats

  def toJson() : String = {
    write(this)
  }
}

/**Synnefo messages*/
case class VMCreated(eventid: Long, when: Date, who: Long, vmid: Long) extends Event
case class VMStarted(eventid: Long, when: Date, who: Long, vmid: Long) extends Event
case class VMStopped(eventid: Long, when: Date, who: Long, vmid: Long) extends Event

/**Pithos messages*/
case class FileCreated(eventid: Long, when: Date, who: Long, fname: String, bytes: Long) extends Event
case class FileUpdated(eventid: Long, when: Date, who: Long, fname: String, bytes: Long) extends Event
case class FileDeleted(eventid: Long, when: Date, who: Long, fname: String, bytes: Long) extends Event
case class FileModified(eventid: Long, when: Date, who: Long, fname: String, bytes: Long) extends Event

/**Networking resource usage messages*/
case class DataUploaded(eventid: Long, when: Date, who: Long, bytes: Long) extends Event
case class DataDownloaded(eventid: Long, when: Date, who: Long, bytes: Long) extends Event

/**SSaaS messages*/
case class SSaasVMCreated(eventid: Long, when: Date, who: Long, vmid: Long, licenceid: Long) extends Event
case class SSaasVMStarted(eventid: Long, when: Date, who: Long, vmid: Long, licenceid: Long) extends Event
case class SSaasVMStopped(eventid: Long, when: Date, who: Long, vmid: Long, licenceid: Long) extends Event
