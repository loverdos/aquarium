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

package gr.grnet.aquarium.connector.rabbitmq.conf

import com.ckkloverdos.props.Props
import com.rabbitmq.client.Address
import gr.grnet.aquarium.util._
import com.ckkloverdos.env.{EnvKey, Env}
import com.ckkloverdos.key.{IntKey, BooleanKey, TypedKeySkeleton, LongKey, ArrayKey, StringKey}

/**
 * Provides configuration keys and utility methods for setting up the RabbitMQ infrastructure.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

object RabbitMQKeys {
  final val PropertiesPrefix = "rabbitmq"
  final val PropertiesPrefixAndDot = PropertiesPrefix + "."

  @inline private[this] def p(name: String) = PropertiesPrefixAndDot + name

  final val DefaultExchangeConfArguments = Env()

  final val DefaultExchangeConf = Env() +
    (RabbitMQExchangeKeys.`type`, TopicExchange) +
    (RabbitMQExchangeKeys.autoDelete, false) +
    (RabbitMQExchangeKeys.durable, true) +
    (RabbitMQExchangeKeys.arguments, DefaultExchangeConfArguments)


  final val DefaultQueueConfArguments = Env() +
    (RabbitMQQueueKeys.Args.xHAPolixy, "all")

  final val DefaultQueueConf = Env() +
    (RabbitMQQueueKeys.autoDelete, false) +
    (RabbitMQQueueKeys.durable, true) +
    (RabbitMQQueueKeys.exclusive, false) +
    (RabbitMQQueueKeys.arguments, DefaultQueueConfArguments)

  final val DefaultChannelConf = Env() +
    (RabbitMQChannelKeys.qosPrefetchCount, 1) +
    (RabbitMQChannelKeys.qosPrefetchSize, 0) +
    (RabbitMQChannelKeys.qosGlobal, false)

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
      (RabbitMQConKeys.vhost, props(RabbitMQConfKeys.vhost)) +
      (RabbitMQConKeys.servers, addresses) +
      (RabbitMQConKeys.reconnect_period_millis, props.getLongEx(RabbitMQConfKeys.reconnect_period_millis))
  }

  def makeRabbitMQConsumerConf(tag: Tag, props: Props, oneERQ: String) = {
    val Array(exchange, routing, queue) = oneERQ.split(':')

    RabbitMQConsumerConf(
      tag = tag,
      exchangeName = exchange,
      routingKey = routing,
      queueName = queue,
      connectionConf = makeConnectionConf(props),
      exchangeConf = DefaultExchangeConf,
      channelConf = DefaultChannelConf,
      queueConf = DefaultQueueConf
    )
  }

  object RabbitMQConKeys {
    final val username = StringKey(p("username"))
    final val password = StringKey(p("password"))
    final val vhost = StringKey(p("vhost"))
    final val servers = ArrayKey[Address](p("servers"))

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
    final val `type` = RabbitMQExchangeTypedKey
    final val durable = BooleanKey(p("durable"))
    final val autoDelete = BooleanKey(p("autoDelete"))
    final val arguments = EnvKey(p("arguments"))
  }

  /**
   * Configuration keys for a `RabbitMQ` exchange.
   *
   * @author Christos KK Loverdos <loverdos@gmail.com>
   */
  object RabbitMQQueueKeys {
    final val durable = BooleanKey(p("durable"))
    final val autoDelete = BooleanKey(p("autoDelete"))
    final val exclusive = BooleanKey(p("exclusive"))
    final val arguments = EnvKey(p("arguments"))

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
    final val qosPrefetchSize = IntKey(p("qosPrefetchSize"))
    final val qosGlobal = BooleanKey(p("qosGlobal"))
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
