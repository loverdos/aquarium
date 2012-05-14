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
import gr.grnet.aquarium.util.{safeUnit, shortClassNameOf}
import com.rabbitmq.client.{Envelope, Consumer, ShutdownSignalException, ShutdownListener, ConnectionFactory, Channel, Connection}
import com.rabbitmq.client.AMQP.BasicProperties
import gr.grnet.aquarium.Configurator
import gr.grnet.aquarium.connector.rabbitmq.eventbus.RabbitMQError
import java.util.concurrent.atomic.{AtomicReference, AtomicBoolean}
import gr.grnet.aquarium.service.event.BusEvent
import gr.grnet.aquarium.connector.handler.{PayloadHandlerExecutor, HandlerResultPanic, HandlerResultRequeue, HandlerResultReject, HandlerResultSuccess, PayloadHandler}
import com.ckkloverdos.maybe.MaybeEither
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.connector.rabbitmq.service.RabbitMQService.{RabbitMQConKeys, RabbitMQQueueKeys, RabbitMQExchangeKeys, RabbitMQChannelKeys}

/**
 * A basic `RabbitMQ` consumer. Sufficiently generalized, sufficiently tied to Aquarium.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class RabbitMQConsumer(conf: RabbitMQConsumerConf,
                       handler: PayloadHandler,
                       executor: PayloadHandlerExecutor) extends Loggable with Lifecycle {
  private[this] var _factory: ConnectionFactory = _
  private[this] var _connection: Connection = _
  private[this] var _channel: Channel = _
//  private[this] val _isAlive = new AtomicBoolean(false)
  private[this] val _state = new AtomicReference[State](Shutdown)

  def isStarted() = {
    _state.get().isStarted && MaybeEither(_channel.isOpen).getOr(false)
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
    _state.set(Shutdown)
  }

  private[this] def doSafeFullShutdownSequence(rescheduleStartup: Boolean): Unit = {
    safeUnit(doFullShutdownSequence())
    _state.set(Shutdown)
    if(rescheduleStartup) {
      doRescheduleStartup()
    }
  }

  private[this] def servers = {
    conf.connectionConf(RabbitMQConKeys.servers)
  }

  private[this] def serversToDebugStrings = {
    servers.map(address ⇒ "%s:%s".format(address.getHost, address.getPort))
  }

  private[this] def doFullStartupSequence() = {
    import this.conf._

    _state.set(StartupSequence)

    val factory = new ConnectionFactory
    factory.setUsername(connectionConf(RabbitMQConKeys.username))
    factory.setPassword(connectionConf(RabbitMQConKeys.password))
    factory.setVirtualHost(connectionConf(RabbitMQConKeys.vhost))

    val connection = factory.newConnection(servers)

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

    val bindOK = channel.queueBind(queueName, exchangeName, routingKey)

    channel.addShutdownListener(RabbitMQShutdownListener)

    _channel.basicConsume(
      queueName,
      false, // We send explicit acknowledgements to RabbitMQ
      RabbitMQMessageConsumer
    )
  }

  def start(): Unit = {
    try {
      logStarting()
      val (ms0, ms1, _) = TimeHelpers.timed {
        doFullStartupSequence()
        _state.set(Started)
      }
      logStarted(ms0, ms1, toDebugString)
    } catch {
      case e: Exception ⇒
        doSafeFullShutdownSequence(true)
        logger.error("While starting", e)

      case e: Throwable ⇒
        throw e
    }
  }

  def stop() = {
    logStopping()
    val (ms0, ms1,  _) = TimeHelpers.timed(doSafeFullShutdownSequence(false))
    logStopped(ms0, ms1, toDebugString)
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
      def doError: PartialFunction[Throwable, Unit] = {
        case e: Exception ⇒
          logger.warn("Unexpected error", e)

        case e: Throwable ⇒
          throw e
      }

      try {
        val deliveryTag = envelope.getDeliveryTag

        executor.exec(body, handler) {
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
        } (doError)
      } catch (doError)
    }
  }

  object RabbitMQShutdownListener extends ShutdownListener {
    @inline def isConnectionError(cause: ShutdownSignalException) = cause.isHardError
    @inline def isChannelError(cause: ShutdownSignalException) = !cause.isHardError

    def shutdownCompleted(cause: ShutdownSignalException) = {
      safeUnit { _channel.close() }

      // Now, let's see what happened
      if(isConnectionError(cause)) {
      } else if(isChannelError(cause)) {
      }
    }
  }

  def toDebugString = {
    "(servers=%s, exchange=%s, routingKey=%s, queue=%s)".format(
      serversToDebugStrings.mkString("[", ", ", "]"),
      conf.exchangeName,
      conf.routingKey,
      conf.queueName)
  }

  override def toString = {
    "%s%s".format(shortClassNameOf(this), toDebugString)
  }
}
