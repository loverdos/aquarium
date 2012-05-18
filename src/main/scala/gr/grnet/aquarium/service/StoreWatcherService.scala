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

package gr.grnet.aquarium.service

import gr.grnet.aquarium.Aquarium
import gr.grnet.aquarium.util.{Tags, Loggable, Lifecycle, Tag}
import gr.grnet.aquarium.util.safeUnit
import java.util.concurrent.atomic.AtomicBoolean
import gr.grnet.aquarium.service.event.{StoreIsAliveBusEvent, StoreIsDeadBusEvent}

/**
 * Watches for liveliness of stores.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

final class StoreWatcherService extends Lifecycle with Loggable {
  private[this] val _pingIsScheduled = new AtomicBoolean(false)
  private[this] val _stopped = new AtomicBoolean(false)
  private[this] val _rcIsAlive = new AtomicBoolean(true)
  private[this] val _imIsAlive = new AtomicBoolean(true)


  def aquarium = Aquarium.Instance

  private[this] def safePingStore(tag: Tag,
                                  pinger: () ⇒ Any,
                                  getStatus: () ⇒ Boolean,
                                  setStatus: (Boolean) ⇒ Any): Unit = {
    try {
      val wasAlive = getStatus()
      pinger()
      // No exception happened, so we are alive
      setStatus(true)
      if(!wasAlive) {
        logger.info("Reconnected %s store".format(tag))
        aquarium.eventBus ! StoreIsAliveBusEvent(tag)
      }
    }
    catch {
      case e: Throwable ⇒
        setStatus(false)
        logger.info("Store %s detected down".format(tag))
        aquarium.eventBus ! StoreIsDeadBusEvent(tag)
    }
  }

  private[this] def doSchedulePing(tag: Tag,
                                   info: String,
                                   pinger: () ⇒ Any,
                                   getStatus: () ⇒ Boolean,
                                   setStatus: (Boolean) ⇒ Any): Unit = {
    aquarium.timerService.scheduleOnce(
      info,
      {
        if(!aquarium.isStopping() && !_stopped.get()) {
//          logger.debug("Pinging %s store".format(tag))
          safePingStore(tag, pinger, getStatus, setStatus)

          doSchedulePing(tag, info, pinger, getStatus, setStatus)
        }
      },
      1000, // TODO: Get value from configuration
      true
    )
  }

  def pingResourceEventStore(): Unit = {
    val tag = Tags.ResourceEventTag

    logger.info("Scheduling ping for %s store".format(tag))

    doSchedulePing(
      tag,
      aquarium.resourceEventStore.toString,
      () ⇒ aquarium.resourceEventStore.pingResourceEventStore(),
      () ⇒ _rcIsAlive.get(),
      alive ⇒ _rcIsAlive.set(alive)
    )
  }

  def pingIMEventStore(): Unit = {
    val tag = Tags.IMEventTag

    logger.info("Scheduling ping for %s store".format(tag))

    doSchedulePing(
      tag,
      aquarium.imEventStore.toString,
      () ⇒ aquarium.imEventStore.pingIMEventStore(),
      () ⇒ _imIsAlive.get(),
      alive ⇒ _imIsAlive.set(alive)
    )
  }

  def start(): Unit = {
    if(!_pingIsScheduled.get()) {
      // First time pings (once)
      safeUnit {
        safePingStore(
          Tags.ResourceEventTag,
          () ⇒ aquarium.resourceEventStore.pingResourceEventStore(),
          () ⇒ _rcIsAlive.get(),
          alive ⇒ _rcIsAlive.set(alive)
        )
      }

      safeUnit {
        safePingStore(
          Tags.IMEventTag,
          () ⇒ aquarium.resourceEventStore.pingResourceEventStore(),
          () ⇒ _imIsAlive.get(),
          alive ⇒ _imIsAlive.set(alive)
        )
      }

      pingResourceEventStore()
      pingIMEventStore()

      _stopped.set(false)
    }
  }

  def stop(): Unit = {
    _stopped.set(true)
  }
}
