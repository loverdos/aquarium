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
import gr.grnet.aquarium.util.safeUnit
import gr.grnet.aquarium.connector.rabbitmq.service.RabbitMQService.{RabbitMQQueueKeys, RabbitMQExchangeKeys}
import com.rabbitmq.client.{Envelope, Consumer, ShutdownSignalException, ShutdownListener, ConnectionFactory, Channel, Connection}
import com.rabbitmq.client.AMQP.BasicProperties
import gr.grnet.aquarium.Configurator
import gr.grnet.aquarium.connector.rabbitmq.eventbus.RabbitMQError
import java.util.concurrent.atomic.{AtomicReference, AtomicBoolean}
import gr.grnet.aquarium.service.event.BusEvent
import gr.grnet.aquarium.connector.handler.{HandlerResultPanic, HandlerResultRequeue, HandlerResultReject, HandlerResultSuccess, PayloadHandler}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class RabbitMQConsumer(val conf: RabbitMQConsumerConf, handler: PayloadHandler) extends Loggable with Lifecycle {
  private[this] var _factory: ConnectionFactory = _
  private[this] var _connection: Connection = _
  private[this] var _channel: Channel = _
  private[this] val _isAlive = new AtomicBoolean(false)
  private[this] val _state = new AtomicReference[State](Shutdown)

  def isAlive() = {
    _isAlive.get() && _state.get().isStarted
  }

  sealed trait State {
    def isStarted: Boolean = false
  }
  case object StartupSequence extends State
  case class  BadStart(e: Exception) extends State
  case object Started  extends State { override def isStarted = true }
  case object ShutdownSequence extends State
  case object Shutdown extends State

  private[this] def doFullShutdownSequence(): Unit = {
    _state.set(ShutdownSequence)
    safeUnit(_channel.close())
    safeUnit(_connection.close())
    _isAlive.set(false)
    _state.set(Shutdown)
  }

  private[this] def doSafeFullShutdownSequence(rescheduleStartup: Boolean): Unit = {
    safeUnit(doFullShutdownSequence())
    _state.set(Shutdown)
    if(rescheduleStartup) {
      doRescheduleStartup()
    }
  }

  private[this] def doFullStartupSequence() = {
    import service.RabbitMQService.RabbitMQConKeys
    import service.RabbitMQService.RabbitMQChannelKeys
    import conf._

    _state.set(StartupSequence)

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
      exchangeConf(RabbitMQExchangeKeys.arguments).toJavaMap
    )

    this._factory = factory
    this._connection = connection
    this._channel = channel

    val declareOK = channel.queueDeclare(
      queueName,
      queueConf(RabbitMQQueueKeys.durable),
      queueConf(RabbitMQQueueKeys.exclusive),
      queueConf(RabbitMQQueueKeys.autoDelete),
      queueConf(RabbitMQQueueKeys.arguments).toJavaMap
    )

    logger.info("Queue declaration: {}", declareOK)

    val bindOK = channel.queueBind(queueName, exchangeName, routingKey)

    logger.info("Queue binding {}", bindOK)

    channel.addShutdownListener(RabbitMQShutdownListener)

    if(_channel.isOpen) {
      _isAlive.getAndSet(true)
    }

    _channel.basicConsume(
      queueName,
      false, // We send explicit acknowledgements to RabbitMQ
      RabbitMQMessageConsumer
    )
  }

  def start(): Unit = {
    try {
      doFullStartupSequence()
      _state.set(Started)
    } catch {
      case e: Exception ⇒
        doSafeFullShutdownSequence(true)
    }
  }

  private[this] def postBusError(event: BusEvent): Unit = {
    Configurator.MasterConfigurator.eventBus ! event
  }

  private[this] def doRescheduleStartup(): Unit = {
  }

  private[this] def doWithChannel[A](f: Channel ⇒ A): Unit = {
    try f(_channel)
    catch {
      case e: Exception ⇒
        postBusError(RabbitMQError(e))
        doSafeFullShutdownSequence(true)
    }
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
      try {
        val deliveryTag = envelope.getDeliveryTag
        val hresult = handler.handlePayload(body)
        hresult match {
          case HandlerResultSuccess ⇒
            doWithChannel(_.basicAck(deliveryTag, false))

          case HandlerResultReject(_) ⇒
            doWithChannel(_.basicReject(deliveryTag, false))

          case HandlerResultRequeue(_) ⇒
            doWithChannel(_.basicReject(deliveryTag, true))

          case HandlerResultPanic ⇒
            // The other end is crucial to the overall operation and it is in panic mode,
            // so we stop delivering messages until further notice
            doSafeFullShutdownSequence(true)
        }
      } catch {
        case e: Exception ⇒
          logger.warn("Unexpected error", e)
      }
    }
  }

  object RabbitMQShutdownListener extends ShutdownListener {
    @inline def isConnectionError(cause: ShutdownSignalException) = cause.isHardError
    @inline def isChannelError(cause: ShutdownSignalException) = !cause.isHardError

    def shutdownCompleted(cause: ShutdownSignalException) = {
      safeUnit { _channel.close() }
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
