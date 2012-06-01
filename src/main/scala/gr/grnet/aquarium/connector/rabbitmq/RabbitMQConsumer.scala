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
import gr.grnet.aquarium.Aquarium
import gr.grnet.aquarium.connector.rabbitmq.eventbus.RabbitMQError
import gr.grnet.aquarium.service.event.BusEvent
import java.util.concurrent.atomic.{AtomicReference, AtomicBoolean}
import com.ckkloverdos.maybe.{Maybe, Just, Failed, MaybeEither}
import gr.grnet.aquarium.connector.handler.{HandlerResultResend, HandlerResult, PayloadHandlerExecutor, HandlerResultPanic, HandlerResultRequeue, HandlerResultReject, HandlerResultSuccess, PayloadHandler}
import gr.grnet.aquarium.connector.rabbitmq.conf.RabbitMQKeys.{RabbitMQConKeys, RabbitMQChannelKeys, RabbitMQExchangeKeys, RabbitMQQueueKeys}

/**
 * A basic `RabbitMQ` consumer. Sufficiently generalized, sufficiently tied to Aquarium.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class RabbitMQConsumer(val conf: RabbitMQConsumerConf,

                       /**
                        * Specifies what we do with the message payload.
                        */
                       handler: PayloadHandler,

                       /**
                        * Specifies how we execute the handler
                        */
                       executor: PayloadHandlerExecutor,

                       /**
                        * After the payload is processed, we call this function with ourselves and the result.
                        */
                       notifier: (RabbitMQConsumer, Maybe[HandlerResult]) ⇒ Unit
) extends Loggable with Lifecycle { consumerSelf ⇒

  private[this] var _factory: ConnectionFactory = _
  private[this] var _connection: Connection = _
  private[this] var _channel: Channel = _
  private[this] val _state = new AtomicReference[State](Shutdown)
  private[this] val _pingIsScheduled = new AtomicBoolean(false)

  /**
   * Reconnects are allowed unless some very specific condition within the application prohibits so.
   */
  @volatile private[this] var _allowReconnects = true

  def isAllowingReconnects(): Boolean = _allowReconnects

  def setAllowReconnects(allowReconnects: Boolean) = {
    _allowReconnects = allowReconnects
    doSchedulePing()
  }

  def isAlive() = {
    val isChannelOpen = MaybeEither((_channel ne null) && _channel.isOpen) match {
      case failed @ Failed(e) ⇒
        logger.error("isChannelOpen", e)
        false

      case Just(x) ⇒
        x
    }

    val isConnectionOpen = MaybeEither((_connection ne null) && _connection.isOpen) match {
      case failed @ Failed(e) ⇒
        logger.error("isConnectionOpen", e)
        false

      case Just(x) ⇒
        x
    }

    _state.get().isStarted && isChannelOpen && isConnectionOpen
  }

  sealed trait State {
    def isStarted: Boolean = false
  }
  case object StartupSequence extends State
  case object Started  extends State {
    override def isStarted = true
  }
  case object ShutdownSequence extends State
  case object Shutdown extends State

  sealed trait StartReason
  case object LifecycleStartReason extends StartReason
  case object PingStartReason extends StartReason

  private[this] def timerService = Aquarium.Instance.timerService

  private[this] lazy val servers = {
    conf.connectionConf(RabbitMQConKeys.servers)
  }

  private[this] lazy val reconnectPeriodMillis = {
    conf.connectionConf(RabbitMQConKeys.reconnect_period_millis)
  }

  private[this] lazy val serversToDebugStrings = {
    servers.map(address ⇒ "%s:%s".format(address.getHost, address.getPort)).toList
  }

  private[this] def infoList(what: String = ""): List[String] = {
    (what match {
      case "" ⇒ List()
      case _  ⇒ List(what)
    }) ++
    List(serversToDebugStrings.mkString("(", ", ", ")")) ++
    List("%s:%s:%s".format(
      conf.exchangeName,
      conf.routingKey,
      conf.queueName
    ))
  }

  private[this] def infoString(what: String) = infoList(what).mkString("[", ", ", "]")

  private[this] def doSafeStartupSequence(startReason: StartReason): Unit = {
    import this.conf._

    if(isAlive() || !isAllowingReconnects() || aquarium.isStopping()) {
      // In case of re-entrance
      return
    }

    try {
      _state.set(StartupSequence)

      val factory = new ConnectionFactory
      factory.setConnectionTimeout(connectionConf(RabbitMQConKeys.reconnect_period_millis).toInt)
      factory.setUsername(connectionConf(RabbitMQConKeys.username))
      factory.setPassword(connectionConf(RabbitMQConKeys.password))
      factory.setVirtualHost(connectionConf(RabbitMQConKeys.vhost))
      factory.setRequestedHeartbeat(connectionConf(RabbitMQConKeys.reconnect_period_millis).toInt)

      val connection = factory.newConnection(servers)

      val channel = connection.createChannel()

      channel.addShutdownListener(RabbitMQShutdownListener)

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

      _channel.basicConsume(
        queueName,
        false, // We send explicit acknowledgements to RabbitMQ
        RabbitMQMessageConsumer
      )

      _state.set(Started)

      logger.info("Connected %s".format(infoString("Start")))
    }
    catch {
      case e: Exception ⇒
        val info = infoString(startReason.toString)
        startReason match {
          case LifecycleStartReason ⇒
            logger.error("While connecting %s".format(info), e)

          case PingStartReason ⇒
            logger.warn("Could not reconnect %s".format(info))
        }

        // Shutdown on failure
        safeStop()
    }
    finally {
      if(!_pingIsScheduled.get()) {
        // Schedule periodic pings
        logger.info("Scheduling %s".format(infoString("Ping")))
        doSchedulePing()
        _pingIsScheduled.getAndSet(true)
      }
    }
  }

  def start(): Unit = {
    safeStart()
  }

  def safeStart(): Unit = {
    doSafeStartupSequence(LifecycleStartReason)
  }

  def safeStop() = {
    _state.set(ShutdownSequence)
    safeUnit(_channel.removeShutdownListener(RabbitMQShutdownListener))
    safeUnit(_channel.close())
    safeUnit(_connection.close())
    _state.set(Shutdown)
  }

  def stop() = {
    safeStop()
  }

  private[this] def aquarium = Aquarium.Instance

  private[this] def postBusError(event: BusEvent): Unit = {
    aquarium.eventBus ! event
  }

  private[this] def doSchedulePing(): Unit = {
    val info = infoString("Ping")

    timerService.scheduleOnce(
      info,
      {
        if(!aquarium.isStopping()) {
          if(isAllowingReconnects()) {
            if(!isAlive()) {
              safeStop()
              doSafeStartupSequence(PingStartReason)
            }
            // Reschedule the ping
            doSchedulePing()
          }
        }
      },
      reconnectPeriodMillis,
      true
    )
  }

  private[this] def doWithChannel[A](f: Channel ⇒ A): Unit = {
    try f(_channel)
    catch {
      case e: Exception ⇒
        logger.error("While using channel %s".format(this._channel), e)
        // FIXME: What is this?
        postBusError(RabbitMQError(e))

//        safeStop()
    }
  }

  object RabbitMQMessageConsumer extends Consumer with Loggable {
    def handleConsumeOk(consumerTag: String) = {
    }

    def handleCancelOk(consumerTag: String) = {
    }

    def handleCancel(consumerTag: String) = {
    }

    def handleShutdownSignal(consumerTag: String, sig: ShutdownSignalException) = {
    }


    def handleRecoverOk(consumerTag: String) = {
    }

    def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]) = {
      val onErrorF: PartialFunction[Throwable, Unit] = {
        case error: Throwable ⇒
          safeUnit(notifier(consumerSelf, Failed(error)))
      }

      try {
        val deliveryTag = envelope.getDeliveryTag

        // nice little composeable functions
        val notifierF = (handlerResult: HandlerResult) ⇒ {
          safeUnit(notifier(consumerSelf, Just(handlerResult)))
          handlerResult
        }

        val onSuccessBasicStepF: (HandlerResult ⇒ HandlerResult) = {
          case result @ HandlerResultSuccess ⇒
            doWithChannel(_.basicAck(deliveryTag, false))
            result

          case result @ HandlerResultResend ⇒
            doWithChannel(_.basicNack(deliveryTag, false, true))
            logger.debug("Got {}", result)
            result

          case result @ HandlerResultReject(reason) ⇒
            doWithChannel(_.basicReject(deliveryTag, false))
            logger.info("Got {}", result)
            result

          case result @ HandlerResultRequeue(reason) ⇒
            doWithChannel(_.basicReject(deliveryTag, true))
            logger.info("Got {}", result)
            result

          case result @ HandlerResultPanic(reason) ⇒
            // Just inform RabbitMQ and subsequent actions will be made by the notifier.
            // So, this is a `HandlerResultResend` with extra semantics.
            doWithChannel(_.basicNack(deliveryTag, false, true))
            logger.info("Got {}", result)
            result
        }

        val onSuccessF = onSuccessBasicStepF andThen notifierF

        executor.exec(body, handler) (onSuccessF) (onErrorF)
      }
      catch (onErrorF)
    }
  }

  object RabbitMQShutdownListener extends ShutdownListener {
    def shutdownCompleted(cause: ShutdownSignalException) = {
      cause.getReason
      logger.info("Got shutdown (%sisHardError) %s".format(
        if(cause.isHardError) "" else "!",
        cause.toString))

      // Now, let's see what happened
      if(cause.isHardError) {
      } else {
      }

      safeStop()
    }
  }

  override def toString = {
    "%s%s".format(shortClassNameOf(this), infoString(""))
  }
}
