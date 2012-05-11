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
import gr.grnet.aquarium.connector.rabbitmq.service.RabbitMQService.{RabbitMQQueueKeys, RabbitMQExchangeKeys}
import com.rabbitmq.client.{ShutdownSignalException, ShutdownListener, ConnectionFactory, Channel, Connection}
import java.util.concurrent.atomic.AtomicBoolean
import com.ckkloverdos.maybe.Maybe

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
    import conf._

    val factory = new ConnectionFactory
    factory.setUsername(connectionConf(RabbitMQConKeys.username))
    factory.setPassword(connectionConf(RabbitMQConKeys.password))
    factory.setVirtualHost(connectionConf(RabbitMQConKeys.vhost))

    val connection = factory.newConnection(connectionConf(RabbitMQConKeys.servers))

    val channel = connection.createChannel()

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

    if(_channel.isOpen) {
      _isAlive.getAndSet(true)
    }

    logger.info("Queue binding {}", bindOK)
  }

  object EventListener extends ShutdownListener {
    def shutdownCompleted(cause: ShutdownSignalException) = {
      Maybe {
        _channel.close()
      }
      _isAlive.getAndSet(false)
    }
  }

  def stop() = {

  }
}
