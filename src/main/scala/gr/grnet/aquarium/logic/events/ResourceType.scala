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

/**
 * This is an object representation for a resource name, which provides convenient querying methods.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
sealed abstract class ResourceType(_name: String) {
  def resourceName = _name

  def isKnownType = true
  def isDiskSpace = false
  def isVMTime = false
  def isBandwidthUpload = false
  def isBandwidthDownload = false
}

/**
 * Companion object used to parse a resource name and provide an object representation in the form
 * of a `ResourceType`.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object ResourceType {
  def fromName(name: String): ResourceType = {
    name match {
      case ResourceNames.bnddown ⇒ BandwidthDown
      case ResourceNames.bndup   ⇒ BandwidthUp
      case ResourceNames.vmtime  ⇒ VMTime
      case _                     ⇒ UnknownResourceType(name)
    }
  }
}

case object BandwidthDown extends ResourceType(ResourceNames.bnddown) {
  override def isBandwidthDownload = true
}

case object BandwidthUp extends ResourceType(ResourceNames.bndup) {
  override def isBandwidthUpload = true
}

case object VMTime extends ResourceType(ResourceNames.vmtime) {
  override def isVMTime = true
}

case object DiskSpace extends ResourceType(ResourceNames.dsksp) {
  override def isDiskSpace = true
}

case class UnknownResourceType(originalName: String) extends ResourceType(ResourceNames.unknown) {
  override def isKnownType = false
}
