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

import gr.grnet.aquarium.Configurable
import com.ckkloverdos.props.Props
import gr.grnet.aquarium.service.event.BusEvent
import com.google.common.eventbus.{EventBus, AsyncEventBus, DeadEvent, Subscribe}
import gr.grnet.aquarium.util.{DaemonThreadFactory, Lifecycle, Loggable}
import java.util.concurrent.{ConcurrentHashMap, Executors}
import java.util.Collections


/**
 * An event bus for internal Aquarium notifications.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class EventBusService extends Loggable with Lifecycle with Configurable {
  private[this] val className = classOf[EventBusService].getName
  private[this] val asyncBus = new AsyncEventBus(
    "%s/async".format(className),
    Executors.newFixedThreadPool(1, new DaemonThreadFactory)
  )

  private[this] val syncBus = new EventBus("%s/sync")

  private[this] val subscribers = Collections.newSetFromMap[AnyRef](new ConcurrentHashMap())

  def propertyPrefix = None

  /**
   * Configure this instance with the provided properties.
   *
   * If `propertyPrefix` is defined, then `props` contains only keys that start with the given prefix.
   */
  def configure(props: Props) = {
  }

  def start() = {
    this addSubscriber this // Wow!
  }

  def stop() = synchronized {
    val iterator = subscribers.iterator()
    while(iterator.hasNext) {
      val subscriber = iterator.next()
      asyncBus.unregister(subscriber)
      syncBus.unregister(subscriber)
    }
    subscribers.clear()
  }

  /**
   * Posts an event synchronously.
   */
  def syncPost[A <: BusEvent](event: A): Unit = {
    syncBus.post(event)
  }

  /**
   * Post an event asynchronously.
   */
  def ![A <: BusEvent](event: A): Unit = {
    asyncBus.post(event)
  }

  def removeSubscriber[A <: AnyRef](subscriber: A): Unit = synchronized {
    subscribers.remove(subscriber)
    asyncBus.unregister(subscriber)
    syncBus.register(subscriber)
  }

  def addSubscriber[A <: AnyRef](subscriber: A): Unit = synchronized {
    subscribers.add(subscriber)
    asyncBus.register(subscriber)
    syncBus.register(subscriber)
  }

  @Subscribe
  def handleDeadEvent(event: DeadEvent): Unit = {
    event.getSource
    logger.warn("DeadEvent %s".format(event.getEvent))
  }
}
