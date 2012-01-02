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

package gr.grnet.aquarium.processor.actor

import gr.grnet.aquarium.logic.events.ResourceEvent
import com.ckkloverdos.maybe.{NoVal, Failed, Just}
import gr.grnet.aquarium.{Configurator}
import gr.grnet.aquarium.util.{Lifecycle, Loggable}

import akka.actor._
import akka.actor.Actor._
import akka.routing.CyclicIterator
import akka.routing.Routing._
import akka.dispatch.Dispatchers
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy
import akka.config.Supervision.SupervisorConfig
import akka.config.Supervision.OneForOneStrategy
import java.util.concurrent.ConcurrentSkipListSet
import gr.grnet.aquarium.messaging.{MessagingNames, AkkaAMQP}
import akka.amqp._
import gr.grnet.aquarium.actor.DispatcherRole

/**
 * An actor that gets events from the queue, stores them persistently
 * and forwards them for further processing.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class ResourceEventProcessorService extends AkkaAMQP with Loggable
with Lifecycle {

  case class AckData(msgId: String, deliveryTag: Long, queue: ActorRef)
  case class Persist(event: ResourceEvent, sender: ActorRef, ackData: AckData)
  case class PersistOK(ackData: AckData)
  case class PersistFailed(ackData: AckData)
  case class Duplicate(ackData: AckData)

  val redeliveries = new ConcurrentSkipListSet[String]()

  private[this] def _configurator: Configurator = Configurator.MasterConfigurator
  private[this] def _calcStateChanges(resourceEvent: ResourceEvent): Unit = {
    val businessLogicDispacther = _configurator.actorProvider.actorForRole(DispatcherRole)
    businessLogicDispacther ! ProcessResourceEvent(resourceEvent) // all state change, credit calc etc will happen there
  }

  class QueueReader extends Actor {

    def receive = {
      case Delivery(payload, _, deliveryTag, isRedeliver, _, queue) =>
        val event = ResourceEvent.fromBytes(payload)
        if (isRedeliver) {
          //Message could not be processed 3 times, just
          if (redeliveries.contains(event.id)) {
            logger.warn("Event[%s] msg[%d] redelivered >2 times. Rejecting".format(event, deliveryTag))
            queue ! Reject(deliveryTag, false)
            redeliveries.remove(event.id)
          } else {
            //Redeliver, but keep track of the message
            redeliveries.add(event.id)
            PersisterManager.lb ! Persist(event, QueueReaderManager.lb, AckData(event.id, deliveryTag, queue.get))
          }
        } else {
          val eventWithReceivedMillis = event.copy(receivedMillis = System.currentTimeMillis())
          PersisterManager.lb ! Persist(eventWithReceivedMillis, QueueReaderManager.lb, AckData(event.id, deliveryTag, queue.get))
        }

      case PersistOK(ackData) =>
        logger.debug("Stored event[%s] msg[%d]".format(ackData.msgId, ackData.deliveryTag))
        ackData.queue ! Acknowledge(ackData.deliveryTag)

      case PersistFailed(ackData) =>
        //Give the message a chance to be processed by other processors
        logger.debug("Storing event[%s] msg[%d] failed".format(ackData.msgId, ackData.deliveryTag))
        ackData.queue ! Reject(ackData.deliveryTag, true)

      case Duplicate(ackData) =>
        logger.debug("Event[%s] msg[%d] is duplicate".format(ackData.msgId, ackData.deliveryTag))
        ackData.queue ! Reject(ackData.deliveryTag, false)

      case Acknowledged(deliveryTag) =>
        logger.debug("Msg with tag [%d] acked".format(deliveryTag))
      //TODO: Forward to the dispatcher

      case Rejected(deliveryTag) =>
        logger.debug("Msg with tag [%d] rejected".format(deliveryTag))

      case _ => logger.warn("Unknown message")
    }

    self.dispatcher = QueueReaderManager.dispatcher
  }

  class Persister extends Actor {

    def receive = {
      case Persist(event, sender, ackData) =>
        if (exists(event))
          sender ! Duplicate(ackData)
        else if (persist(event)) {
          sender ! PersistOK(ackData)
          // TODO: Move to some proper place (after ACK?)
          _calcStateChanges(event)
        } else
          sender ! PersistFailed(ackData)
      case _ => logger.warn("Unknown message")
    }

    def exists(event: ResourceEvent): Boolean =
      _configurator.resourceEventStore.findResourceEventById(event.id).isJust

    def persist(event: ResourceEvent): Boolean = {
      _configurator.resourceEventStore.storeResourceEvent(event) match {
        case Just(x) => true
        case x: Failed =>
          logger.error("Could not save event: %s".format(event))
          false
        case NoVal => false
      }
    }

    self.dispatcher = PersisterManager.dispatcher
  }

  lazy val supervisor = Supervisor(SupervisorConfig(
    OneForOneStrategy(
      List(classOf[Exception]), //What exceptions will be handled
      3, // maximum number of restart retries
      5000 // within time in millis
    ), Nil

  ))

  object QueueReaderManager {
    val numCPUs = Runtime.getRuntime.availableProcessors
    var actors: List[ActorRef] = _
    lazy val lb = loadBalancerActor(new CyclicIterator(actors))

    lazy val dispatcher =
      Dispatchers.newExecutorBasedEventDrivenWorkStealingDispatcher("QueueReaderDispatcher")
        .withNewThreadPoolWithLinkedBlockingQueueWithCapacity(100)
        .setMaxPoolSize(numCPUs)
        .setCorePoolSize(2)
        .setKeepAliveTimeInMillis(60000)
        .setRejectionPolicy(new CallerRunsPolicy).build

    def start() = {
      actors = {
        for (i <- 0 until numCPUs) yield {
          val actor = actorOf(new QueueReader)
          supervisor.link(actor)
          actor.start()
          actor
        }
      }.toList
    }

    def stop() = dispatcher.stopAllAttachedActors
  }

  object PersisterManager {
    val numCPUs = Runtime.getRuntime.availableProcessors
    var actors: List[ActorRef] = _

    lazy val lb = loadBalancerActor(new CyclicIterator(actors))

    val dispatcher =
      Dispatchers.newExecutorBasedEventDrivenWorkStealingDispatcher("PersisterDispatcher")
        .withNewThreadPoolWithLinkedBlockingQueueWithCapacity(100)
        .setMaxPoolSize(numCPUs)
        .setCorePoolSize(2)
        .setKeepAliveTimeInMillis(60000)
        .setRejectionPolicy(new CallerRunsPolicy).build

    def start() = {
      actors = {
        for (i <- 0 until numCPUs) yield {
          val actor = actorOf(new Persister)
          supervisor.link(actor)
          actor.start()
          actor
        }
      }.toList
    }

    def stop() = dispatcher.stopAllAttachedActors
  }

  def start() {
    logger.info("Starting resource event processor service")

    QueueReaderManager.start()
    PersisterManager.start()

    consumer("%s.#".format(MessagingNames.RES_EVENT_KEY),
      MessagingNames.RESOURCE_EVENT_QUEUE, MessagingNames.AQUARIUM_EXCHANGE,
      QueueReaderManager.lb, false)
  }

  def stop() {
    QueueReaderManager.stop()
    PersisterManager.stop()

    logger.info("Stopping resource event processor service")
  }
}