package gr.grnet.aquarium.logic.events

import net.liftweb.json.Serialization.write
import java.util.Date

/**Generic event */
abstract class Event(eventid: Long, w: Date, u: Long) {

  def id() = eventid
  def when() = w
  def who() = u

  implicit val formats = net.liftweb.json.DefaultFormats

  def toJson() : String = {
    write(this)
  }
}

/**Synnefo messages*/
case class VMCreated(i: Long, w: Date, u: Long, vmid: Long) extends Event(i, w, u)
case class VMStarted(i: Long, w: Date, u: Long, vmid: Long) extends Event(i, w, u)
case class VMStopped(i: Long, w: Date, u: Long, vmid: Long) extends Event(i, w, u)

/**Pithos messages*/
case class DiskSpaceChanged(i: Long, w: Date, u: Long, bytes: Long) extends Event(i, w, u)

/**Networking resource usage messages*/
case class DataUploaded(i: Long, w: Date, u: Long, bytes: Long) extends Event(i, w, u)
case class DataDownloaded(i: Long, w: Date, u: Long, bytes: Long) extends Event(i, w, u)

/**SSaaS messages*/
case class SSaasVMCreated(i: Long, w: Date, u: Long, vmid: Long, licenceid: Long) extends Event(i, w, u)
case class SSaasVMStarted(i: Long, w: Date, u: Long, vmid: Long, licenceid: Long) extends Event(i, w, u)
case class SSaasVMStopped(i: Long, w: Date, u: Long, vmid: Long, licenceid: Long) extends Event(i, w, u)
