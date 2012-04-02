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
import com.ckkloverdos.maybe.{NoVal, Just, Failed, Maybe}
import gr.grnet.aquarium.{AquariumException, Configurator}

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
abstract class EventProcessorService[E <: AquariumEvent] extends AkkaAMQP with Loggable with Lifecycle {

  /* Messages exchanged between the persister and the queuereader */
  case class AckData(msgId: String, deliveryTag: Long, queue: ActorRef)
  case class Persist(event: E, initialPayload: Array[Byte], sender: ActorRef, ackData: AckData)
  case class PersistOK(ackData: AckData)
  case class PersistFailed(ackData: AckData)
  case class Duplicate(ackData: AckData)

  /* Short term storage for delivery tags to work around AMQP
   * limitation with redelivering rejected messages to same host.
   */
  private val redeliveries = new ConcurrentSkipListSet[String]()

  /* Temporarily keeps track of messages while being processed */
  private val inFlightEvents = new ConcurrentHashMap[Long, E](200, 0.9F, 4)

  /* Supervisor actor for each event processing operation */
  private lazy val supervisor = Supervisor(SupervisorConfig(
    OneForOneStrategy(
      List(classOf[Exception]), //What exceptions will be handled
      5, // maximum number of restart retries
      5000 // within time in millis
    ), Nil
  ))

  protected def _configurator: Configurator = Configurator.MasterConfigurator

  protected def decode(data: Array[Byte]): Maybe[E]
  protected def forward(event: E): Unit
  protected def exists(event: E): Boolean
  protected def persist(event: E, initialPayload: Array[Byte]): Boolean
  protected def persistUnparsed(initialPayload: Array[Byte]): Unit

  protected def queueReaderThreads: Int
  protected def persisterThreads: Int
  protected def numQueueActors: Int
  protected def numPersisterActors: Int
  protected def name: String

  protected def persisterManager: PersisterManager
  protected def queueReaderManager: QueueReaderManager

  protected val numCPUs = Runtime.getRuntime.availableProcessors

  def start(): Unit
  def stop() : Unit

  protected def declareQueues(conf: String) = {
    val decl = _configurator.get(conf)
    decl.split(";").foreach {
      q =>
        val i = q.split(":")

        if (i.size < 3)
          throw new Exception("Queue declaration \"%s\" not correct".format(q))

        val exchange = i(0)
        val route = i(1)
        val qname = i(2)
        logger.info("Declaring queue %s (%s -> %s)".format(qname, exchange, route))
        consumer(route, qname, exchange, queueReaderManager.lb, false)
    }
  }

  class QueueReader extends Actor {

    def receive = {
      case Delivery(payload, _, deliveryTag, isRedeliver, _, queue) =>
        val eventM = decode(payload)
        val event = eventM match {
          case Just(event) ⇒
            event

          case failed @ Failed(e, m) ⇒
            persistUnparsed(payload)
            logger.error("Could not parse payload {}", new String(payload, "UTF-8"))
            throw e

          case NoVal ⇒
            persistUnparsed(payload)
            logger.error("Could not parse payload {}", new String(payload, "UTF-8"))
            throw new AquariumException("Unexpected NoVal")
        }
        inFlightEvents.put(deliveryTag, event)

        if (isRedeliver) {
          //Message could not be processed 3 times, just ignore it
          if (redeliveries.contains(event.id)) {
            logger.warn("Actor[%s] - Event[%s] msg[%d] redelivered >2 times. Rejecting".format(self.getUuid(), event, deliveryTag))
            queue ! Reject(deliveryTag, false)
            redeliveries.remove(event.id)
            inFlightEvents.remove(deliveryTag)
          } else {
            //Redeliver, but keep track of the message
            redeliveries.add(event.id)
            persisterManager.lb ! Persist(event, payload, queueReaderManager.lb, AckData(event.id, deliveryTag, queue.get))
          }
        } else {
          val eventWithReceivedMillis = event.copyWithReceivedMillis(System.currentTimeMillis()).asInstanceOf[E]
          persisterManager.lb ! Persist(eventWithReceivedMillis, payload, queueReaderManager.lb, AckData(event.id, deliveryTag, queue.get))
        }

      case PersistOK(ackData) =>
        logger.debug("Actor[%s] - Stored event[%s] msg[%d] - %d left".format(self.getUuid(), ackData.msgId, ackData.deliveryTag, inFlightEvents.size))
        ackData.queue ! Acknowledge(ackData.deliveryTag)

      case PersistFailed(ackData) =>
        //Give the message a chance to be processed by other processors
        logger.error("Actor[%s] - Storing event[%s] msg[%d] failed".format(self.getUuid(), ackData.msgId, ackData.deliveryTag))
        inFlightEvents.remove(ackData.deliveryTag)
        ackData.queue ! Reject(ackData.deliveryTag, true)

      case Duplicate(ackData) =>
        logger.debug("Actor[%s] - Event[%s] msg[%d] is duplicate".format(self.getUuid(), ackData.msgId, ackData.deliveryTag))
        inFlightEvents.remove(ackData.deliveryTag)
        ackData.queue ! Reject(ackData.deliveryTag, false)

      case Acknowledged(deliveryTag) =>
        logger.debug("Actor[%s] - Msg with tag [%d] acked. Forwarding...".format(self.getUuid(), deliveryTag))
        forward(inFlightEvents.remove(deliveryTag))

      case Rejected(deliveryTag) =>
        logger.debug("Actor[%s] - Msg with tag [%d] rejected".format(self.getUuid(), deliveryTag))

      case _ => logger.warn("Unknown message")
    }

    override def preStart = {
      logger.debug("Starting actor QueueReader-%s".format(self.getUuid()))
      super.preStart
    }

    self.dispatcher = queueReaderManager.dispatcher
  }

  class Persister extends Actor {

    def receive = {
      case Persist(event, initialPayload, sender, ackData) =>
        logger.debug("Persister-%s attempting store".format(self.getUuid()))
        //val time = System.currentTimeMillis()
        if (exists(event))
          sender ! Duplicate(ackData)
        else if (persist(event, initialPayload)) {
          //logger.debug("Persist time: %d ms".format(System.currentTimeMillis() - time))
          sender ! PersistOK(ackData)
        } else
          sender ! PersistFailed(ackData)
      case _ => logger.warn("Unknown message")
    }

    override def preStart = {
      logger.debug("Starting actor Persister-%s".format(self.getUuid()))
      super.preStart
    }

    self.dispatcher = persisterManager.dispatcher
  }

  class QueueReaderManager {
    lazy val lb = loadBalancerActor(new CyclicIterator(actors))

    lazy val dispatcher =
      Dispatchers.newExecutorBasedEventDrivenWorkStealingDispatcher(name + "-queuereader")
        .withNewThreadPoolWithLinkedBlockingQueueWithCapacity(1000)
        .setMaxPoolSize(2 * numCPUs)
        .setCorePoolSize(queueReaderThreads)
        .setKeepAliveTimeInMillis(60000)
        .setRejectionPolicy(new CallerRunsPolicy).build

    lazy val actors =
      for (i <- 0 until numQueueActors) yield {
        val actor = actorOf(new QueueReader)
        supervisor.link(actor)
        actor.start()
        actor
      }

    def stop() = dispatcher.stopAllAttachedActors
  }

  class PersisterManager {
    lazy val lb = loadBalancerActor(new CyclicIterator(actors))

    val dispatcher =
      Dispatchers.newExecutorBasedEventDrivenWorkStealingDispatcher(name + "-persister")
        .withNewThreadPoolWithLinkedBlockingQueueWithCapacity(1000)
        .setMaxPoolSize(2 * numCPUs)
        .setCorePoolSize(persisterThreads)
        .setKeepAliveTimeInMillis(60000)
        .setRejectionPolicy(new CallerRunsPolicy).build

    lazy val actors =
      for (i <- 0 until numPersisterActors) yield {
        val actor = actorOf(new Persister)
        supervisor.link(actor)
        actor.start()
        actor
      }

    def stop() = dispatcher.stopAllAttachedActors
  }
}