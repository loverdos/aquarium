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

package gr.grnet.aquarium.connector.rabbitmq.service

import com.ckkloverdos.props.Props
import com.ckkloverdos.env.{EnvKey, Env}
import com.ckkloverdos.key.{LongKey, ArrayKey, IntKey, TypedKeySkeleton, BooleanKey, StringKey}
import com.google.common.eventbus.Subscribe
import com.rabbitmq.client.Address
import gr.grnet.aquarium.{Aquarium, Configurable}
import gr.grnet.aquarium.connector.rabbitmq.conf.{TopicExchange, RabbitMQConsumerConf, RabbitMQExchangeType}
import gr.grnet.aquarium.connector.rabbitmq.service.RabbitMQService.RabbitMQConfKeys
import gr.grnet.aquarium.converter.{JsonTextFormat, StdConverters}
import gr.grnet.aquarium.event.model.resource.{StdResourceEvent, ResourceEventModel}
import gr.grnet.aquarium.event.model.im.{StdIMEvent, IMEventModel}
import gr.grnet.aquarium.actor.RouterRole
import gr.grnet.aquarium.actor.message.event.{ProcessIMEvent, ProcessResourceEvent}
import gr.grnet.aquarium.store.{LocalFSEventStore, IMEventStore, ResourceEventStore}
import gr.grnet.aquarium.connector.rabbitmq.RabbitMQConsumer
import gr.grnet.aquarium.util.{Tags, Loggable, Lifecycle, Tag}
import gr.grnet.aquarium.util.shortInfoOf
import gr.grnet.aquarium.util.sameTags
import gr.grnet.aquarium.connector.handler.{HandlerResultPanic, HandlerResult}
import com.ckkloverdos.maybe.{Failed, Just, Maybe, MaybeEither}
import gr.grnet.aquarium.service.event.{StoreIsAliveBusEvent, StoreIsDeadBusEvent}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class RabbitMQService extends Loggable with Lifecycle with Configurable {
  @volatile private[this] var _props: Props = Props()(StdConverters.AllConverters)
  @volatile private[this] var _consumers = List[RabbitMQConsumer]()

  def propertyPrefix = Some(RabbitMQService.PropertiesPrefix)

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
    val jsonParser: (Array[Byte] ⇒ JsonTextFormat) = { payload ⇒
      converters.convertEx[JsonTextFormat](payload)
    }

    val rcEventParser: (JsonTextFormat ⇒ ResourceEventModel) = { jsonTextFormat ⇒
      StdResourceEvent.fromJsonTextFormat(jsonTextFormat)
    }

    val imEventParser: (JsonTextFormat ⇒ IMEventModel) = { jsonTextFormat ⇒
      StdIMEvent.fromJsonTextFormat(jsonTextFormat)
    }

    val rcForwardAction: (ResourceEventStore#ResourceEvent ⇒ Unit) = { rcEvent ⇒
      router ! ProcessResourceEvent(rcEvent)
    }

    val rcDebugForwardAction: (ResourceEventStore#ResourceEvent ⇒ Unit) = { rcEvent ⇒
      logger.info("Forwarding {}", rcEvent)
    }

    val imForwardAction: (IMEventStore#IMEvent ⇒ Unit) = { imEvent ⇒
      router ! ProcessIMEvent(imEvent)
    }

    val imDebugForwardAction: (IMEventStore#IMEvent ⇒ Unit) = { imEvent ⇒
      logger.info("Forwarding {}", imEvent)
    }

    val postNotifier = (consumer: RabbitMQConsumer, maybeResult: Maybe[HandlerResult]) ⇒ {
      maybeResult match {
        case Just(hr @ HandlerResultPanic) ⇒
          // The other end is crucial to the overall operation and it is in panic mode,
          // so we stop delivering messages until further notice
          logger.warn("Shutting down %s due to [%s]".format(consumer.toString, hr))
          consumer.setAllowReconnects(false)
          consumer.safeStop()

        case Failed(e) ⇒
          logger.warn("Shutting down %s due to [%s]".format(consumer.toString, shortInfoOf(e)))
          consumer.setAllowReconnects(false)
          consumer.safeStop()

        case _ ⇒
      }
    }

    val rcHandler = new GenericPayloadHandler[ResourceEventModel, ResourceEventStore#ResourceEvent](
      jsonParser,
      (payload, error) ⇒ {
        LocalFSEventStore.storeUnparsedResourceEvent(aquarium, payload, error)
        logger.error("Error creating JSON from %s payload".format(Tags.ResourceEventTag), error)
      },
      rcEventParser,
      (payload, error) ⇒ {
        logger.error("Error creating object model from %s payload".format(Tags.ResourceEventTag), error)
      },
      rcEvent ⇒ resourceEventStore.insertResourceEvent(rcEvent),
      rcDebugForwardAction
    )

    val imHandler = new GenericPayloadHandler[IMEventModel, IMEventStore#IMEvent](
      jsonParser,
      (payload, error) ⇒ {
        LocalFSEventStore.storeUnparsedIMEvent(aquarium, payload, error)
        logger.error("Error parsing JSON from %s payload".format(Tags.IMEventTag), error)
      },
      imEventParser,
      (payload, error) ⇒ {
        logger.error("Error creating object model from %s payload".format(Tags.IMEventTag), error)
      },
      imEvent ⇒ imEventStore.insertIMEvent(imEvent),
      imDebugForwardAction
    )

    val futureExecutor = new PayloadHandlerFutureExecutor

    // (e)xchange:(r)outing key:(q)

    // These two are to trigger an error if the property does not exist
    locally(_props(RabbitMQConfKeys.rcevents_queues))
    locally(_props(RabbitMQConfKeys.imevents_queues))

    val all_rc_ERQs = _props.getTrimmedList(RabbitMQConfKeys.rcevents_queues)

    val rcConsumerConfs_ = for(oneERQ ← all_rc_ERQs) yield {
      RabbitMQService.makeRabbitMQConsumerConf(Tags.ResourceEventTag, _props, oneERQ)
    }
    val rcConsumerConfs = rcConsumerConfs_.toSet.toList
    if(rcConsumerConfs.size != rcConsumerConfs_.size) {
      logger.warn(
        "Duplicate %s consumer info in %s=%s".format(
        RabbitMQService.PropertiesPrefix,
        RabbitMQConfKeys.rcevents_queues,
        _props(RabbitMQConfKeys.rcevents_queues)))
    }

    val all_im_ERQs = _props.getTrimmedList(RabbitMQConfKeys.imevents_queues)
    val imConsumerConfs_ = for(oneERQ ← all_im_ERQs) yield {
      RabbitMQService.makeRabbitMQConsumerConf(Tags.IMEventTag, _props, oneERQ)
    }
    val imConsumerConfs = imConsumerConfs_.toSet.toList
    if(imConsumerConfs.size != imConsumerConfs_.size) {
      logger.warn(
        "Duplicate %s consumer info in %s=%s".format(
        RabbitMQService.PropertiesPrefix,
        RabbitMQConfKeys.imevents_queues,
        _props(RabbitMQConfKeys.imevents_queues)))
    }

    val rcConsumers = for(rccc ← rcConsumerConfs) yield {
      logger.info("Declaring %s consumer {exchange=%s, routingKey=%s, queue=%s}".format(
        RabbitMQService.PropertiesPrefix,
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
        RabbitMQService.PropertiesPrefix,
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
    aquarium.eventBus.addSubsciber(this)

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

object RabbitMQService {
  final val PropertiesPrefix       = "rabbitmq"
  final val PropertiesPrefixAndDot = PropertiesPrefix + "."

  @inline private[this] def p(name: String) = PropertiesPrefixAndDot + name

  final val DefaultExchangeConfArguments = Env()

  final val DefaultExchangeConf = Env() +
    (RabbitMQExchangeKeys.`type`,     TopicExchange) +
    (RabbitMQExchangeKeys.autoDelete, false)         +
    (RabbitMQExchangeKeys.durable,    true)          +
    (RabbitMQExchangeKeys.arguments,  DefaultExchangeConfArguments)


  final val DefaultQueueConfArguments = Env() +
    (RabbitMQQueueKeys.Args.xHAPolixy, "all")

  final val DefaultQueueConf = Env() +
    (RabbitMQQueueKeys.autoDelete, false)         +
    (RabbitMQQueueKeys.durable,    true)          +
    (RabbitMQQueueKeys.exclusive,  false)         +
    (RabbitMQQueueKeys.arguments,  DefaultQueueConfArguments)

  final val DefaultChannelConf = Env() +
    (RabbitMQChannelKeys.qosPrefetchCount, 1) +
    (RabbitMQChannelKeys.qosPrefetchSize,  0) +
    (RabbitMQChannelKeys.qosGlobal,        false)

  def makeConnectionConf(props: Props) = {
    val servers = props.getTrimmedList(RabbitMQConfKeys.servers)
    val port = props.getIntEx(RabbitMQConfKeys.port)
    val addresses = servers.map(new Address(_, port)).toArray

    // TODO: Integrate the below RabbitMQConKeys and RabbitMQConfKeys
    // TODO:  Normally this means to get rid of Props and use Env everywhere.
    // TODO:  [Will be more type-safe anyway.]
    Env() +
      (RabbitMQConKeys.username, props(RabbitMQConfKeys.username)) +
      (RabbitMQConKeys.password, props(RabbitMQConfKeys.password)) +
      (RabbitMQConKeys.vhost,    props(RabbitMQConfKeys.vhost))    +
      (RabbitMQConKeys.servers,  addresses) +
      (RabbitMQConKeys.reconnect_period_millis, props.getLongEx(RabbitMQConfKeys.reconnect_period_millis))
  }

  def makeRabbitMQConsumerConf(tag: Tag, props: Props, oneERQ: String) = {
    val Array(exchange, routing, queue) = oneERQ.split(':')

    RabbitMQConsumerConf(
      tag         = tag,
      exchangeName   = exchange,
      routingKey     = routing,
      queueName      = queue,
      connectionConf = makeConnectionConf(props),
      exchangeConf   = DefaultExchangeConf,
      channelConf    = DefaultChannelConf,
      queueConf      = DefaultQueueConf
    )
  }

  object RabbitMQConKeys {
    final val username  = StringKey(p("username"))
    final val password  = StringKey(p("password"))
    final val vhost     = StringKey(p("vhost"))
    final val servers   = ArrayKey[Address](p("servers"))

    final val reconnect_period_millis = LongKey(p("reconnect.period.millis"))
  }

  /**
   * A [[com.ckkloverdos.key.TypedKey]] for the exchange type
   */
  final case object RabbitMQExchangeTypedKey extends TypedKeySkeleton[RabbitMQExchangeType]("type")

  /**
   * Configuration keys for a `RabbitMQ` exchange.
   *
   * @author Christos KK Loverdos <loverdos@gmail.com>
   */
  object RabbitMQExchangeKeys {
    final val `type`     = RabbitMQExchangeTypedKey
    final val durable    = BooleanKey(p("durable"))
    final val autoDelete = BooleanKey(p("autoDelete"))
    final val arguments  = EnvKey(p("arguments"))
  }

  /**
   * Configuration keys for a `RabbitMQ` exchange.
   *
   * @author Christos KK Loverdos <loverdos@gmail.com>
   */
  object RabbitMQQueueKeys {
    final val durable    = BooleanKey(p("durable"))
    final val autoDelete = BooleanKey(p("autoDelete"))
    final val exclusive  = BooleanKey(p("exclusive"))
    final val arguments  = EnvKey(p("arguments"))

    object Args {
      // http://www.rabbitmq.com/ha.html
      // NOTE the exact name of the key (without using p()); this will be passed directly to RabbitMQ
      final val xHAPolixy = StringKey("x-ha-policy")
    }
  }

  /**
   * Configuration keys for a `RabbitMQ` channel.
   *
   * @author Christos KK Loverdos <loverdos@gmail.com>
   */
  object RabbitMQChannelKeys {
    final val qosPrefetchCount = IntKey(p("qosPrefetchCount"))
    final val qosPrefetchSize  = IntKey(p("qosPrefetchSize"))
    final val qosGlobal        = BooleanKey(p("qosGlobal"))
  }

  object RabbitMQConfKeys {
    /**
     * How often do we attempt a reconnection?
     */
    final val reconnect_period_millis = p("reconnect.period.millis")

    /**
     * Comma separated list of AMQP servers running in active-active
     * configuration.
     */
    final val servers = p("servers")

    /**
     * Comma separated list of AMQP servers running in active-active
     * configuration.
     */
    final val port = p("port")

    /**
     * User name for connecting with the AMQP server
     */
    final val username = p("username")

    /**
     * Password for connecting with the AMQP server
     */
    final val password = p("passwd")

    /**
     * Virtual host on the AMQP server
     */
    final val vhost = p("vhost")

    /**
     * Comma separated list of exchanges known to aquarium
     * FIXME: What is this??
     */
    final val exchange = p("exchange")

    /**
     * Queues for retrieving resource events from. Multiple queues can be
     * declared, separated by comma.
     *
     * Format is `exchange:routing.key:queue-name,...`
     */
    final val rcevents_queues = p("rcevents.queues")

    /**
     * Queues for retrieving user events from. Multiple queues can be
     * declared, separated by comma.
     *
     * Format is `exchange:routing.key:queue-name,...`
     */
    final val imevents_queues = p("imevents.queues")
  }
}
