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

package gr.grnet.aquarium.connector.rabbitmq

import gr.grnet.aquarium.connector.rabbitmq.conf.RabbitMQConsumerConf
import gr.grnet.aquarium.util.{Lifecycle, Loggable}
import gr.grnet.aquarium.util.tryUnit
import gr.grnet.aquarium.connector.rabbitmq.service.RabbitMQService.{RabbitMQQueueKeys, RabbitMQExchangeKeys}
import java.util.concurrent.atomic.AtomicBoolean
import com.rabbitmq.client.{Envelope, Consumer, ShutdownSignalException, ShutdownListener, ConnectionFactory, Channel, Connection}
import com.rabbitmq.client.AMQP.BasicProperties

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class RabbitMQConsumer(val conf: RabbitMQConsumerConf) extends Loggable with Lifecycle {
  private[this] var _factory: ConnectionFactory = _
  private[this] var _connection: Connection = _
  private[this] var _channel: Channel = _
  private[this] val _isAlive = new AtomicBoolean(false)

  def isAlive() = {
    _isAlive.get()
  }

  def start() = {
    import service.RabbitMQService.RabbitMQConKeys
    import service.RabbitMQService.RabbitMQChannelKeys
    import conf._

    val factory = new ConnectionFactory
    factory.setUsername(connectionConf(RabbitMQConKeys.username))
    factory.setPassword(connectionConf(RabbitMQConKeys.password))
    factory.setVirtualHost(connectionConf(RabbitMQConKeys.vhost))

    val connection = factory.newConnection(connectionConf(RabbitMQConKeys.servers))

    val channel = connection.createChannel()

    channel.basicQos(
      channelConf(RabbitMQChannelKeys.qosPrefetchSize),
      channelConf(RabbitMQChannelKeys.qosPrefetchCount),
      channelConf(RabbitMQChannelKeys.qosGlobal)
    )

    channel.exchangeDeclare(
      exchangeName,
      exchangeConf(RabbitMQExchangeKeys.`type`).name,
      exchangeConf(RabbitMQExchangeKeys.durable),
      exchangeConf(RabbitMQExchangeKeys.autoDelete),
      exchangeConf(RabbitMQExchangeKeys.arguments).toJavaMap.asInstanceOf[java.util.Map[String, AnyRef]]
    )

    this._factory = factory
    this._connection = connection
    this._channel = channel

    val declareOK = channel.queueDeclare(
      queueName,
      queueConf(RabbitMQQueueKeys.durable),
      queueConf(RabbitMQQueueKeys.exclusive),
      queueConf(RabbitMQQueueKeys.autoDelete),
      queueConf(RabbitMQQueueKeys.arguments).toJavaMap.asInstanceOf[java.util.Map[String, AnyRef]]
    )

    logger.info("Queue declaration: {}", declareOK)

    val bindOK = channel.queueBind(queueName, exchangeName, routingKey)
    channel.addShutdownListener(RabbitMQShutdownListener)

    if(_channel.isOpen) {
      _isAlive.getAndSet(true)
    }

    _channel.basicConsume(
      queueName,
      false, // We send explicit acknowledgements to RabbitMQ
      RabbitMQMessageConsumer
    )

    logger.info("Queue binding {}", bindOK)
  }

  object RabbitMQMessageConsumer extends Consumer {
    def handleConsumeOk(consumerTag: String) = {
    }

    def handleCancelOk(consumerTag: String) = {
    }

    def handleCancel(consumerTag: String) = {
    }

    def handleShutdownSignal(consumerTag: String, sig: ShutdownSignalException) = {
    }

    def handleRecoverOk() = {
    }

    def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]) = {

    }
  }

  object RabbitMQShutdownListener extends ShutdownListener {
    @inline def isConnectionError(cause: ShutdownSignalException) = cause.isHardError
    @inline def isChannelError(cause: ShutdownSignalException) = !cause.isHardError

    def shutdownCompleted(cause: ShutdownSignalException) = {
      tryUnit { _channel.close() }
      _isAlive.getAndSet(false)

      // Now, let's see what happened
      if(isConnectionError(cause)) {
      } else if(isChannelError(cause)) {
      }
    }
  }

  def stop() = {

  }
}
