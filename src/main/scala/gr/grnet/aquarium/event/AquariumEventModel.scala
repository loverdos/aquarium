/*
 * Copyright 2011-2012 GRNET S.A. All rights reserved.
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

package gr.grnet.aquarium.event

import gr.grnet.aquarium.util.json.JsonSupport
import gr.grnet.aquarium.util.makeBytes
import gr.grnet.aquarium.util.xml.XmlSupport

/**
 * The base model for all events coming from external systems.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

trait AquariumEventModel extends JsonSupport with XmlSupport {
  /**
   * The id at the sender side
   */
  def id: String

  /**
   * When it occurred at the sender side
   */
  def occurredMillis: Long

  /**
   * When it was received by Aquarium
   */
  def receivedMillis: Long
  def userID: String
  def eventVersion: String
  def details: Map[String, String]

  /**
   * The ID given to this event if/when persisted to a store.
   * The exact type of the id is store-specific.
   */
  def storeID: Option[AnyRef] = None

  def toBytes: Array[Byte] = makeBytes(toJsonString)

  def withReceivedMillis(newReceivedMillis: Long): AquariumEventModel
}

object AquariumEventModel {
  trait NamesT {
    final val id = "id"
    final val occurredMillis = "occurredMillis"
    final val receivedMillis = "receivedMillis"
    final val userID = "userID"
    final val eventVersion = "eventVersion"
    final val details = "details"
  }

  object Names extends NamesT
}