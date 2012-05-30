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

import com.ckkloverdos.props.Props
import com.google.common.eventbus.Subscribe
import gr.grnet.aquarium.{Aquarium, Configurable}
import gr.grnet.aquarium.converter.StdConverters
import gr.grnet.aquarium.actor.RouterRole
import gr.grnet.aquarium.connector.rabbitmq.RabbitMQConsumer
import gr.grnet.aquarium.util.{Tags, Loggable, Lifecycle}
import gr.grnet.aquarium.util.sameTags
import gr.grnet.aquarium.service.event.{StoreIsAliveBusEvent, StoreIsDeadBusEvent}
import gr.grnet.aquarium.connector.handler.{ResourceEventPayloadHandler, IMEventPayloadHandler}
import gr.grnet.aquarium.connector.rabbitmq.service.{PayloadHandlerFutureExecutor, PayloadHandlerPostNotifier}
import gr.grnet.aquarium.connector.rabbitmq.conf.RabbitMQKeys.RabbitMQConfKeys
import gr.grnet.aquarium.connector.rabbitmq.conf.RabbitMQKeys

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class RabbitMQService extends Loggable with Lifecycle with Configurable {
  @volatile private[this] var _props: Props = Props()(StdConverters.AllConverters)
  @volatile private[this] var _consumers = List[RabbitMQConsumer]()

  def propertyPrefix = Some(RabbitMQKeys.PropertiesPrefix)

  def aquarium = Aquarium.Instance

  def eventBus = aquarium.eventBus

  def resourceEventStore = aquarium.resourceEventStore

  def imEventStore = aquarium.imEventStore

  def converters = aquarium.converters

  def router = aquarium.actorProvider.actorForRole(RouterRole)

  /**
   * Configure this instance with the provided properties.
   *
   * If `propertyPrefix` is defined, then `props` contains only keys that start with the given prefix.
   */
  def configure(props: Props) = {
    this._props = props

    doConfigure()
  }

  private[this] def doConfigure(): Unit = {
    val postNotifier = new PayloadHandlerPostNotifier(logger)

    val rcHandler = new ResourceEventPayloadHandler(aquarium, logger)

    val imHandler = new IMEventPayloadHandler(aquarium, logger)

    val futureExecutor = new PayloadHandlerFutureExecutor

    // (e)xchange:(r)outing key:(q)

    // These two are to trigger an error if the property does not exist
    locally(_props(RabbitMQConfKeys.rcevents_queues))
    locally(_props(RabbitMQConfKeys.imevents_queues))

    val all_rc_ERQs = _props.getTrimmedList(RabbitMQConfKeys.rcevents_queues)

    val rcConsumerConfs_ = for(oneERQ ← all_rc_ERQs) yield {
      RabbitMQKeys.makeRabbitMQConsumerConf(Tags.ResourceEventTag, _props, oneERQ)
    }
    val rcConsumerConfs = rcConsumerConfs_.toSet.toList
    if(rcConsumerConfs.size != rcConsumerConfs_.size) {
      logger.warn(
        "Duplicate %s consumer info in %s=%s".format(
          RabbitMQKeys.PropertiesPrefix,
          RabbitMQConfKeys.rcevents_queues,
          _props(RabbitMQConfKeys.rcevents_queues)))
    }

    val all_im_ERQs = _props.getTrimmedList(RabbitMQConfKeys.imevents_queues)
    val imConsumerConfs_ = for(oneERQ ← all_im_ERQs) yield {
      RabbitMQKeys.makeRabbitMQConsumerConf(Tags.IMEventTag, _props, oneERQ)
    }
    val imConsumerConfs = imConsumerConfs_.toSet.toList
    if(imConsumerConfs.size != imConsumerConfs_.size) {
      logger.warn(
        "Duplicate %s consumer info in %s=%s".format(
          RabbitMQKeys.PropertiesPrefix,
          RabbitMQConfKeys.imevents_queues,
          _props(RabbitMQConfKeys.imevents_queues)))
    }

    val rcConsumers = for(rccc ← rcConsumerConfs) yield {
      logger.info("Declaring %s consumer {exchange=%s, routingKey=%s, queue=%s}".format(
        RabbitMQKeys.PropertiesPrefix,
        rccc.exchangeName,
        rccc.routingKey,
        rccc.queueName
      ))
      new RabbitMQConsumer(
        rccc,
        rcHandler,
        futureExecutor,
        postNotifier
      )
    }

    val imConsumers = for(imcc ← imConsumerConfs) yield {
      logger.info("Declaring %s consumer {exchange=%s, routingKey=%s, queue=%s}".format(
        RabbitMQKeys.PropertiesPrefix,
        imcc.exchangeName,
        imcc.routingKey,
        imcc.queueName
      ))
      new RabbitMQConsumer(
        imcc,
        imHandler,
        futureExecutor,
        postNotifier
      )
    }

    this._consumers = rcConsumers ++ imConsumers

    val lg: (String ⇒ Unit) = if(this._consumers.size == 0) logger.warn(_) else logger.debug(_)
    lg("Got %s consumers".format(this._consumers.size))

    this._consumers.foreach(logger.debug("Configured {}", _))
  }

  def start() = {
    aquarium.eventBus.addSubscriber(this)

    safeStart()
  }

  def safeStart() = {
    for(consumer ← this._consumers) {
      logStartingF(consumer.toString) {
        consumer.safeStart()
      } {}
    }

    for(consumer ← this._consumers) {
      if(!consumer.isAlive()) {
        logger.warn("Consumer not started yet %s".format(consumer))
      }
    }
  }

  def stop() = {
    safeStop()
  }

  def safeStop() = {
    for(consumer ← this._consumers) {
      logStoppingF(consumer.toString) {
        consumer.safeStop()
      } {}
    }
  }

  @Subscribe
  def handleStoreFailure(event: StoreIsDeadBusEvent): Unit = {
    val eventTag = event.tag

    val consumersForTag = this._consumers.filter(consumer ⇒ sameTags(consumer.conf.tag, eventTag))
    for(consumer ← consumersForTag) {
      if(consumer.isAlive()) {
        // Our store is down, so we cannot accept messages anymore
        logger.info("Shutting down %s, since store for %s is down".format(consumer, eventTag))
        consumer.setAllowReconnects(false)
        consumer.safeStop()
      }
    }
  }

  @Subscribe
  def handleStoreRevival(event: StoreIsAliveBusEvent): Unit = {
    val eventTag = event.tag

    val consumersForTag = this._consumers.filter(consumer ⇒ sameTags(consumer.conf.tag, eventTag))
    for(consumer ← consumersForTag) {
      if(!consumer.isAlive() && !aquarium.isStopping()) {
        // out store is up, so we can again accept messages
        logger.info("Starting up %s, since store for %s is alive".format(consumer, eventTag))
        consumer.setAllowReconnects(true)
        consumer.safeStart()
      }
    }
  }
}
