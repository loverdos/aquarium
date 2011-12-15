package gr.grnet.aquarium.processor.actor

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

import gr.grnet.aquarium.messaging.AkkaAMQP
import akka.actor.{ActorRef, Actor}
import gr.grnet.aquarium.logic.events.ResourceEvent
import akka.amqp.{Reject, Acknowledge, Acknowledged, Delivery}
import com.ckkloverdos.maybe.{NoVal, Failed, Just}
import gr.grnet.aquarium.{MasterConf}
import gr.grnet.aquarium.util.{Lifecycle, Loggable}

/**
 * An actor that gets events from the queue, stores them persistently
 * and forwards them for further processing.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class ResourceEventProcessorService extends AkkaAMQP with Loggable
  with Lifecycle {

  private var persister: ActorRef = _
  private var queueReader: ActorRef = _

  case class AckData(deliveryTag: Long, queue: ActorRef)
  case class Persist(event: ResourceEvent, sender: ActorRef, ackData: AckData)
  case class PersistOK(ackData: AckData)
  case class PersistFailed(ackData: AckData)
  case class Duplicate(ackData: AckData)

  class QueueReader(persister: ActorRef) extends Actor {

    def receive = {
      case Delivery(payload, _, deliveryTag, isRedeliver, _, queue) =>
        val event = ResourceEvent.fromBytes(payload)
        persister ! Persist(event, queueReader, AckData(deliveryTag, queue.get))

      case PersistOK(ackData) =>
        logger.debug("Stored res event:%s".format(ackData.deliveryTag))
        ackData.queue ! Acknowledge(ackData.deliveryTag)

      case PersistFailed(ackData) =>
        logger.debug("Storing res event:%s failed".format(ackData.deliveryTag))
        ackData.queue ! Reject(ackData.deliveryTag, true)

      case Duplicate(ackData) =>
        logger.debug("Res event:%s is duplicate".format(ackData.deliveryTag))
        ackData.queue ! Reject(ackData.deliveryTag, false)

      case Acknowledged(deliveryTag) =>
        //Forward to the dispatcher

      case _ => logger.warn("Unknown message")
    }
  }

  class Persister extends Actor {

    def receive = {
      case Persist(event, sender, ackData) =>
        if (exists(event))
          sender ! Duplicate(ackData)
        else if (persist(event))
          sender ! PersistOK(ackData)
        else
          sender ! PersistFailed(ackData)
      case _ => logger.warn("Unknown message")
    }

    def exists(event: ResourceEvent): Boolean =
      MasterConf.MasterConf.eventStore.findEventById(event.id).isEmpty

    def persist(event: ResourceEvent): Boolean = {
      MasterConf.MasterConf.eventStore.storeEvent(event) match {
          case Just(x) => true
          case x: Failed =>
            logger.error("Could not save event: %s".format(event))
            false
          case NoVal => false
        }
    }
  }

  def start() {
    logger.info("Starting resource event processor service")

    persister = Actor.actorOf(new Persister)
    queueReader = Actor.actorOf(new QueueReader(persister))

    queueReader.startLink(persister)

    queueReader.start()
    persister.start()

    consumer("event.#", "resource-events", "aquarium", queueReader, false)
  }

  def stop() {
    logger.info("Stopping resource event processor service")

    persister.stop()
    queueReader.stop()
  }
}