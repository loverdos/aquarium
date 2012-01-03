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
import gr.grnet.aquarium.messaging.{AkkaAMQP}
import akka.amqp._
import java.util.concurrent.{ConcurrentHashMap, ConcurrentSkipListSet}
import gr.grnet.aquarium.logic.events.AquariumEvent

/**
 * An abstract service that retrieves Aquarium events from a queue,
 * stores them persistently and forwards them for further processing.
 * The processing happens among two load-balanced actor clusters
 * asynchronously. The number of employed actors is always equal to
 * the number of processors. The number of threads per load-balanced
 * cluster is configurable by subclasses.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
abstract class EventProcessorService extends AkkaAMQP with Loggable
with Lifecycle {

  /* Messages exchanged between the persister and the queuereader */
  case class AckData(msgId: String, deliveryTag: Long, queue: ActorRef)
  case class Persist(event: AquariumEvent, sender: ActorRef, ackData: AckData)
  case class PersistOK(ackData: AckData)
  case class PersistFailed(ackData: AckData)
  case class Duplicate(ackData: AckData)

  /* Short term storage for delivery tags to work around AMQP
   * limitation with redelivering rejected messages to same host.
   */
  private val redeliveries = new ConcurrentSkipListSet[String]()

  /* Temporarily keeps track of messages while being processed */
  private val inFlightEvents = new ConcurrentHashMap[Long, AquariumEvent](200, 0.9F, 4)

  /* Supervisor actor for each event processing operation */
  private lazy val supervisor = Supervisor(SupervisorConfig(
    OneForOneStrategy(
      List(classOf[Exception]), //What exceptions will be handled
      5, // maximum number of restart retries
      5000 // within time in millis
    ), Nil
  ))

  protected def _configurator: Configurator = Configurator.MasterConfigurator

  protected def decode(data: Array[Byte]): AquariumEvent
  protected def forward(resourceEvent: AquariumEvent): Unit
  protected def exists(event: AquariumEvent): Boolean
  protected def persist(event: AquariumEvent): Boolean

  protected def queueReaderThreads: Int
  protected def persisterThreads: Int
  protected def name: String

  protected def persisterManager: PersisterManager
  protected def queueReaderManager: QueueReaderManager
  
  def start(): Unit
  def stop() : Unit

  class QueueReader extends Actor {

    def receive = {
      case Delivery(payload, _, deliveryTag, isRedeliver, _, queue) =>
        val event = decode(payload)
        inFlightEvents.put(deliveryTag, event)

        if (isRedeliver) {
          //Message could not be processed 3 times, just ignore it
          if (redeliveries.contains(event.id)) {
            logger.warn("Event[%s] msg[%d] redelivered >2 times. Rejecting".format(event, deliveryTag))
            queue ! Reject(deliveryTag, false)
            redeliveries.remove(event.id)
            inFlightEvents.remove(deliveryTag)
          } else {
            //Redeliver, but keep track of the message
            redeliveries.add(event.id)
            persisterManager.lb ! Persist(event, queueReaderManager.lb, AckData(event.id, deliveryTag, queue.get))
          }
        } else {
          val eventWithReceivedMillis = event.setRcvMillis(System.currentTimeMillis())
          persisterManager.lb ! Persist(eventWithReceivedMillis, queueReaderManager.lb, AckData(event.id, deliveryTag, queue.get))
        }

      case PersistOK(ackData) =>
        logger.debug("Stored event[%s] msg[%d] - %d left".format(ackData.msgId, ackData.deliveryTag, inFlightEvents.size))
        ackData.queue ! Acknowledge(ackData.deliveryTag)

      case PersistFailed(ackData) =>
        //Give the message a chance to be processed by other processors
        logger.debug("Storing event[%s] msg[%d] failed".format(ackData.msgId, ackData.deliveryTag))
        inFlightEvents.remove(ackData.deliveryTag)
        ackData.queue ! Reject(ackData.deliveryTag, true)

      case Duplicate(ackData) =>
        logger.debug("Event[%s] msg[%d] is setRcvMillis".format(ackData.msgId, ackData.deliveryTag))
        inFlightEvents.remove(ackData.deliveryTag)
        ackData.queue ! Reject(ackData.deliveryTag, false)

      case Acknowledged(deliveryTag) =>
        logger.debug("Msg with tag [%d] acked. Forwarding...".format(deliveryTag))
        forward(inFlightEvents.remove(deliveryTag))

      case Rejected(deliveryTag) =>
        logger.debug("Msg with tag [%d] rejected".format(deliveryTag))

      case _ => logger.warn("Unknown message")
    }

    self.dispatcher = queueReaderManager.dispatcher
  }

  class Persister extends Actor {

    def receive = {
      case Persist(event, sender, ackData) =>
        if (exists(event))
          sender ! Duplicate(ackData)
        else if (persist(event)) {
          sender ! PersistOK(ackData)
        } else
          sender ! PersistFailed(ackData)
      case _ => logger.warn("Unknown message")
    }

    self.dispatcher = persisterManager.dispatcher
  }

  class QueueReaderManager {
    val numCPUs = Runtime.getRuntime.availableProcessors
    lazy val lb = loadBalancerActor(new CyclicIterator(actors))

    lazy val dispatcher =
      Dispatchers.newExecutorBasedEventDrivenWorkStealingDispatcher(name + "-queuereader")
        .withNewThreadPoolWithLinkedBlockingQueueWithCapacity(1000)
        .setMaxPoolSize(queueReaderThreads)
        .setCorePoolSize(1)
        .setKeepAliveTimeInMillis(60000)
        .setRejectionPolicy(new CallerRunsPolicy).build

    lazy val actors =
      for (i <- 0 until numCPUs) yield {
        val actor = actorOf(new QueueReader)
        supervisor.link(actor)
        actor.start()
        actor
      }

    def stop() = dispatcher.stopAllAttachedActors
  }

  class PersisterManager {
    val numCPUs = Runtime.getRuntime.availableProcessors
    lazy val lb = loadBalancerActor(new CyclicIterator(actors))

    val dispatcher =
      Dispatchers.newExecutorBasedEventDrivenWorkStealingDispatcher(name + "-persister")
        .withNewThreadPoolWithLinkedBlockingQueueWithCapacity(1000)
        .setMaxPoolSize(persisterThreads)
        .setCorePoolSize(1)
        .setKeepAliveTimeInMillis(60000)
        .setRejectionPolicy(new CallerRunsPolicy).build

    lazy val actors =
      for (i <- 0 until numCPUs) yield {
        val actor = actorOf(new Persister)
        supervisor.link(actor)
        actor.start()
        actor
      }

    def stop() = dispatcher.stopAllAttachedActors
  }
}