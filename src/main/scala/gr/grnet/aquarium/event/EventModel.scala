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

/**
 * Basic properties for all events.
 * An event represents some state change, where state is specific to the use-case.
 *
 *@author Christos KK Loverdos <loverdos@gmail.com>
 */

trait EventModel {
  /**
   * The unique event id. The responsibility for the id generation is to the event generator.
   */
  def id: String

  /**
   * The Unix time of the state change occurrence that this event represents.
   */
  def occurredMillis: Long

  /**
   * The ID given to this event if/when persisted to a store.
   * The exact type of the id is store-specific.
   */
  def storeID: Option[AnyRef] = None

  def eventVersion: String

  /**
   * An extension point that provides even more properties.
   */
  def details: Map[String, String]

  def withDetails(newDetails: Map[String, String], newOccurredMillis: Long): EventModel
}


object EventModel {
  trait NamesT {
    final val id = "id"
    final val occurredMillis = "occurredMillis"
    final val storeID = "storeID"
    final val eventVersion = "eventVersion"
    final val details = "details"
  }

  object Names extends NamesT
}