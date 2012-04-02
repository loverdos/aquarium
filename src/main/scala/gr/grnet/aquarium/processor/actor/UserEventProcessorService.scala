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

package gr.grnet.aquarium.processor.actor

import gr.grnet.aquarium.logic.events.UserEvent
import gr.grnet.aquarium.actor.DispatcherRole
import gr.grnet.aquarium.Configurator.Keys
import gr.grnet.aquarium.store.LocalFSEventStore
import com.ckkloverdos.maybe.{Maybe, NoVal, Failed, Just}

/**
 * An event processor service for user events coming from the IM system
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class UserEventProcessorService extends EventProcessorService[UserEvent] {

  override def decode(data: Array[Byte]) = Maybe { UserEvent.fromBytes(data) }

  override def forward(event: UserEvent) =
    _configurator.actorProvider.actorForRole(DispatcherRole) ! ProcessUserEvent(event)

  override def exists(event: UserEvent) =
    _configurator.userEventStore.findUserEventById(event.id).isJust

  override def persist(event: UserEvent, initialPayload: Array[Byte]) = {
    LocalFSEventStore.storeUserEvent(_configurator, event, initialPayload)

    _configurator.userEventStore.storeUserEvent(event) match {
      case Just(x) => true
      case x: Failed =>
        logger.error("Could not save user event: %s".format(event))
        false
      case NoVal => false
    }
  }

  protected def persistUnparsed(initialPayload: Array[Byte]): Unit = {
    Maybe { logger.warn("Saving unparsed\n%s".format(new String(initialPayload, "UTF-8"))) }
    LocalFSEventStore.storeUserEvent(_configurator, null, initialPayload)
  }

  override def queueReaderThreads: Int = 1
  override def persisterThreads: Int = numCPUs
  protected def numQueueActors = 2 * queueReaderThreads
  protected def numPersisterActors = 2 * persisterThreads
  override def name = "usrevtproc"

  lazy val persister = new PersisterManager
  lazy val queueReader = new QueueReaderManager

  override def persisterManager   = persister
  override def queueReaderManager = queueReader

  def start() {
    logger.info("Starting user event processor service")
    declareQueues(Keys.amqp_userevents_queues)
  }

  def stop() {
    queueReaderManager.stop()
    persisterManager.stop()

    logger.info("Stopping user event processor service")
  }
}