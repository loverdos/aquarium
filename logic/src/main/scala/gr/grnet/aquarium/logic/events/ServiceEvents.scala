/*
 * Copyright 2011 GRNET S.A. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 *   1. Redistributions of source code must retain the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer.
 *
 *   2. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials
 *      provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY GRNET S.A. ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL GRNET S.A OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and
 * documentation are those of the authors and should not be
 * interpreted as representing official policies, either expressed
 * or implied, of GRNET S.A.
 */

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
