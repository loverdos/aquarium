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

import gr.grnet.aquarium.actor.RouterRole
import gr.grnet.aquarium.Configurator.Keys
import gr.grnet.aquarium.store.LocalFSEventStore
import com.ckkloverdos.maybe.{Maybe, Just, Failed, NoVal}
import gr.grnet.aquarium.actor.message.service.router.ProcessResourceEvent
import gr.grnet.aquarium.event.ResourceEvent
import gr.grnet.aquarium.util.date.TimeHelpers


/**
 * An event processor service for resource events
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
final class ResourceEventProcessorService extends EventProcessorService[ResourceEvent] {

  override def parseJsonBytes(data: Array[Byte]) = ResourceEvent.fromBytes(data)

  override def forward(event: ResourceEvent): Unit = {
    if(event ne null) {
      val businessLogicDispacther = _configurator.actorProvider.actorForRole(RouterRole)
      businessLogicDispacther ! ProcessResourceEvent(event)
    }
  }

  override def existsInStore(event: ResourceEvent): Boolean =
    _configurator.resourceEventStore.findResourceEventById(event.id).isJust

  override def storeParsedEvent(event: ResourceEvent, initialPayload: Array[Byte]): Unit = {
    // 1. Store to local FS for debugging purposes.
    //    BUT be resilient to errors, since this is not critical
    if(_configurator.eventsStoreFolder.isJust) {
      Maybe {
        LocalFSEventStore.storeResourceEvent(_configurator, event, initialPayload)
      }
    }

    // 2. Store to DB
    _configurator.resourceEventStore.storeResourceEvent(event)
  }


  protected def storeUnparsedEvent(initialPayload: Array[Byte], exception: Throwable): Unit = {
    // TODO: Also save to DB, just like we do for UserEvents
    LocalFSEventStore.storeUnparsedResourceEvent(_configurator, initialPayload, exception)
  }

  override def queueReaderThreads: Int = 1

  override def persisterThreads: Int = numCPUs + 4

  override def numQueueActors: Int = 1 * queueReaderThreads

  override def numPersisterActors: Int = 2 * persisterThreads

  override def name = "resevtproc"

  lazy val persister = new PersisterManager
  lazy val queueReader = new QueueReaderManager

  override def persisterManager = persister

  override def queueReaderManager = queueReader

  def start() {
    logStarting()
    val (ms0, ms1, _) = TimeHelpers.timed {
      declareQueues(Keys.amqp_resevents_queues)
    }
    logStarted(ms0, ms1)
  }

  def stop() {
    logStopping()
    val (ms0, ms1, _) = TimeHelpers.timed {
      queueReaderManager.stop()
      persisterManager.stop()
    }
    logStopped(ms0, ms1)
  }
}