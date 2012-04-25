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
import gr.grnet.aquarium.actor.message.service.dispatcher.ProcessIMEvent
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.util.makeString
import com.ckkloverdos.maybe._
import gr.grnet.aquarium.event.im.IMEventModel
import gr.grnet.aquarium.store.memory.MemStore

/**
 * An event processor service for user events coming from the IM system
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class IMEventProcessorService extends EventProcessorService[IMEventModel] {

  override def parseJsonBytes(data: Array[Byte]) = {
    MemStore.createIMEventFromJsonBytes(data)
  }

  override def forward(event: IMEventModel) = {
    if(event ne null) {
      _configurator.actorProvider.actorForRole(RouterRole) ! ProcessIMEvent(event)
    }
  }

  override def existsInStore(event: IMEventModel) =
    _configurator.imEventStore.findIMEventById(event.id).isJust

  override def storeParsedEvent(event: IMEventModel, initialPayload: Array[Byte]) = {
    // 1. Store to local FS for debugging purposes.
    //    BUT be resilient to errors, since this is not critical
    if(_configurator.eventsStoreFolder.isJust) {
      Maybe {
        LocalFSEventStore.storeIMEvent(_configurator, event, initialPayload)
      }
    }

    // 2. Store to DB
    _configurator.imEventStore.insertIMEvent(event)
  }

  protected def storeUnparsedEvent(initialPayload: Array[Byte], exception: Throwable): Unit = {
    val json = makeString(initialPayload)

    LocalFSEventStore.storeUnparsedIMEvent(_configurator, initialPayload, exception)
  }

  override def queueReaderThreads: Int = 1

  override def persisterThreads: Int = numCPUs

  protected def numQueueActors = 2 * queueReaderThreads

  protected def numPersisterActors = 2 * persisterThreads

  override def name = "usrevtproc"

  lazy val persister = new PersisterManager
  lazy val queueReader = new QueueReaderManager

  override def persisterManager = persister

  override def queueReaderManager = queueReader

  def start() {
    logStarting()
    val (ms0, ms1, _) = TimeHelpers.timed {
      declareQueues(Keys.amqp_userevents_queues)
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