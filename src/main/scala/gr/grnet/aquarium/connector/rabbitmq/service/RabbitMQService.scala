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
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.util.{ReflectHelpers, Loggable, Lifecycle}
import com.rabbitmq.client.{ConnectionFactory, Address}
import gr.grnet.aquarium.{Configurator, Configurable}
import gr.grnet.aquarium.connector.rabbitmq.eventbus.RabbitMQError
import gr.grnet.aquarium.connector.rabbitmq.conf.{TopicExchange, RabbitMQConsumerConf, RabbitMQExchangeType}
import gr.grnet.aquarium.connector.rabbitmq.service.RabbitMQService.RabbitMQConfKeys
import com.ckkloverdos.key.{ArrayKey, IntKey, TypedKeySkeleton, BooleanKey, StringKey}
import com.ckkloverdos.env.{EnvKey, Env}
import gr.grnet.aquarium.converter.{JsonTextFormat, StdConverters}
import gr.grnet.aquarium.event.model.resource.{StdResourceEvent, ResourceEventModel}
import gr.grnet.aquarium.event.model.im.{StdIMEvent, IMEventModel}
import com.ckkloverdos.maybe.{MaybeEither, Failed, Just, Maybe}
import gr.grnet.aquarium.connector.handler.{HandlerResultSuccess, HandlerResultPanic, HandlerResultReject, HandlerResult, PayloadHandler}
import gr.grnet.aquarium.actor.RouterRole
import gr.grnet.aquarium.actor.message.event.{ProcessIMEvent, ProcessResourceEvent}
import gr.grnet.aquarium.store.{LocalFSEventStore, IMEventStore, ResourceEventStore}
import gr.grnet.aquarium.connector.rabbitmq.RabbitMQConsumer

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class RabbitMQService extends Loggable with Lifecycle with Configurable {
  private[this] val props: Props = Props()(StdConverters.AllConverters)

  def propertyPrefix = Some(RabbitMQService.PropertiesPrefix)

  def configurator = Configurator.MasterConfigurator

  def eventBus = configurator.eventBus

  def resourceEventStore = configurator.resourceEventStore

  def imEventStore = configurator.imEventStore

  def converters = configurator.converters

  def router = configurator.actorProvider.actorForRole(RouterRole)

  /**
   * Configure this instance with the provided properties.
   *
   * If `propertyPrefix` is defined, then `props` contains only keys that start with the given prefix.
   */
  def configure(props: Props) = {
    ReflectHelpers.setField(this, "props", props)

    try {
      doConfigure()
      logger.info("Configured with {}", this.props)
    } catch {
      case e: Exception ⇒
      // If we have no internal error, then something is bad with RabbitMQ
      eventBus ! RabbitMQError(e)
      throw e
    }
  }

  private[this] def doConfigure(): Unit = {
    val jsonParser: (Array[Byte] ⇒ MaybeEither[JsonTextFormat]) = { payload ⇒
      converters.convert[JsonTextFormat](payload)
    }

    val rcEventParser: (JsonTextFormat ⇒ ResourceEventModel) = { jsonTextFormat ⇒
      StdResourceEvent.fromJsonTextFormat(jsonTextFormat)
    }

    val imEventParser: (JsonTextFormat ⇒ IMEventModel) = { jsonTextFormat ⇒
      StdIMEvent.fromJsonTextFormat(jsonTextFormat)
    }

    val rcHandler = new GenericPayloadHandler[ResourceEventModel, ResourceEventStore#ResourceEvent](
      jsonParser,
      (payload, error) ⇒ LocalFSEventStore.storeUnparsedResourceEvent(configurator, payload, error),
      rcEventParser,
      rcEvent ⇒ resourceEventStore.insertResourceEvent(rcEvent),
      rcEvent ⇒ router ! ProcessResourceEvent(rcEvent)
    )

    val imHandler = new GenericPayloadHandler[IMEventModel, IMEventStore#IMEvent](
      jsonParser,
      (payload, error) ⇒ LocalFSEventStore.storeUnparsedIMEvent(configurator, payload, error),
      imEventParser,
      imEvent ⇒ imEventStore.insertIMEvent(imEvent),
      imEvent ⇒ router ! ProcessIMEvent(imEvent)
    )

    // (e)xchange:(r)outing key:(q)
    val all_rc_ERQs = props.getTrimmedList(RabbitMQConfKeys.rcevents_queues)
    val rcConsumerConfs = for(oneERQ ← all_rc_ERQs) yield {
      RabbitMQService.makeRabbitMQConsumerConf(props, oneERQ)
    }

    val all_im_ERQs = props.getTrimmedList(RabbitMQConfKeys.imevents_queues)
    val imConsumerConfs = for(oneERQ ← all_im_ERQs) yield {
      RabbitMQService.makeRabbitMQConsumerConf(props, oneERQ)
    }

    val rcConsumers = for(rccc ← rcConsumerConfs) yield {
      new RabbitMQConsumer(rccc, rcHandler)
    }

    val imConsumers = for(imcc ← imConsumerConfs) yield {
      new RabbitMQConsumer(imcc, imHandler)
    }
  }

  def start() = {
    logStarted(TimeHelpers.nowMillis(), TimeHelpers.nowMillis())
    System.exit(1)
  }

  def stop() = {
    logStopped(TimeHelpers.nowMillis(), TimeHelpers.nowMillis())
  }

}

object RabbitMQService {
  final val PropertiesPrefix       = "rabbitmq"
  final val PropertiesPrefixAndDot = PropertiesPrefix + "."

  @inline private[this] def p(name: String) = PropertiesPrefix + name

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
      (RabbitMQConKeys.servers,  addresses)
  }

  def makeRabbitMQConsumerConf(props: Props, oneERQ: String) = {
    val Array(exchange, routing, queue) = oneERQ.split(':')

    RabbitMQConsumerConf(
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
     * Comma separated list of AMQP servers running in active-active
     * configuration.
     */
    final val servers = p("servers")
    final val amqp_servers = servers

    /**
     * Comma separated list of AMQP servers running in active-active
     * configuration.
     */
    final val port = p("port")
    final val amqp_port = port

    /**
     * User name for connecting with the AMQP server
     */
    final val username = p("username")
    final val amqp_username = username

    /**
     * Password for connecting with the AMQP server
     */
    final val password = p("passwd")
    final val amqp_password = password

    /**
     * Virtual host on the AMQP server
     */
    final val vhost = p("vhost")
    final val amqp_vhost = vhost

    /**
     * Comma separated list of exchanges known to aquarium
     * FIXME: What is this??
     */
    final val exchange = p("exchange")
    final val amqp_exchange = exchange

    /**
     * Queues for retrieving resource events from. Multiple queues can be
     * declared, separated by comma.
     *
     * Format is `exchange:routing.key:queue-name,...`
     */
    final val rcevents_queues = p("rcevents.queues")
    final val amqp_rcevents_queues = rcevents_queues

    /**
     * Queues for retrieving user events from. Multiple queues can be
     * declared, separated by comma.
     *
     * Format is `exchange:routing.key:queue-name,...`
     */
    final val imevents_queues = p("imevents.queues")
    final val amqp_imevents_queues = imevents_queues
  }

}
