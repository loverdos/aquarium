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

import com.ckkloverdos.maybe.Maybe
import gr.grnet.aquarium.events.ResourceEvent

/**
 * An abstraction for Aquarium `ResourceEvent` stores.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 * @author Georgios Gousios <gousiosg@gmail.com>.
 */
trait ResourceEventStore {
  def clearResourceEvents(): Unit = {
    throw new Exception("Unsupported operation")
  }

  def storeResourceEvent(event: ResourceEvent): Maybe[RecordID]

  def findResourceEventById(id: String): Maybe[ResourceEvent]

  def findResourceEventsByUserId(userId: String)(sortWith: Option[(ResourceEvent, ResourceEvent) => Boolean]): List[ResourceEvent]

  /**
   * Returns the events for the given User after (or equal) the given timestamp.
   *
   * The events are returned in ascending timestamp order.
   */
  def findResourceEventsByUserIdAfterTimestamp(userId: String, timestamp: Long): List[ResourceEvent]
  
  def findResourceEventHistory(userId: String, resName: String,
                               instid: Option[String], upTo: Long) : List[ResourceEvent]

  def findResourceEventsForReceivedPeriod(userId: String, startTimeMillis: Long, stopTimeMillis: Long): List[ResourceEvent]

  /**
   * Count and return the number of "out of sync" events for a billing month.
   */
  def countOutOfSyncEventsForBillingPeriod(userId: String, startMillis: Long, stopMillis: Long): Maybe[Long]

  /**
   * Finds all relevant resource events for the billing period.
   * The relevant events are those:
   * a) whose `occurredMillis` is within the given billing period or
   * b) whose `receivedMillis` is within the given billing period.
   *
   * Order them by `occurredMillis`
   * FIXME: implement
   */
  def findAllRelevantResourceEventsForBillingPeriod(userId: String,
                                                    startMillis: Long,
                                                    stopMillis: Long): List[ResourceEvent]
}