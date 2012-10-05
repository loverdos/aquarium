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

package gr.grnet.aquarium.store

import gr.grnet.aquarium.AquariumInternalError
import gr.grnet.aquarium.message.avro.gen.ResourceEventMsg

/**
 * An abstraction for Aquarium `ResourceEvent` stores.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 * @author Georgios Gousios <gousiosg@gmail.com>.
 */
trait ResourceEventStore {
  def pingResourceEventStore(): Unit

  def insertResourceEvent(event: ResourceEventMsg): ResourceEventMsg

  def findResourceEventByID(id: String): Option[ResourceEventMsg]

  /**
   * Counts and returns the number of "out of sync" events for a billing period.
   * Note the we assume billing months for now. So, do not cross the month border in the provided time-stamps.
   */
  def countOutOfSyncResourceEventsForBillingPeriod(userID: String, startMillis: Long, stopMillis: Long): Long

  /**
   *
   * @param userID
   * @param startMillis
   * @param stopMillis
   * @param f
   * @return The number of resource events processed.
   */
  def foreachResourceEventOccurredInPeriod(
      userID: String,
      startMillis: Long,
      stopMillis: Long
  )(f: ResourceEventMsg ⇒ Unit): Long
}