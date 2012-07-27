package gr.grnet.aquarium.connector.rabbitmq

import conf.RabbitMQConsumerConf
import conf.{RabbitMQKeys, RabbitMQConsumerConf}
import conf.RabbitMQKeys.{RabbitMQConfKeys, RabbitMQConKeys}
import gr.grnet.aquarium.{Configurable, ResourceLocator, AquariumBuilder, Aquarium}
import com.rabbitmq.client._
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import com.ckkloverdos.props.Props
import gr.grnet.aquarium.converter.StdConverters
import gr.grnet.aquarium.util.{Lock, Tags}
import gr.grnet.aquarium.store.memory.MemStoreProvider
import java.io.File
import com.ckkloverdos.resource.FileStreamResource


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

class RabbitMQProducer extends Configurable {
  private[this] var _conf: RabbitMQConsumerConf = _
  private[this] var _factory: ConnectionFactory = _
  private[this] var _connection: Connection = _
  private[this] var _channel: Channel = _
  private[this] var _servers : Array[Address] = _
  private[this] final val lock = new Lock()

  def propertyPrefix: Option[String] = Some(RabbitMQKeys.PropertiesPrefix)
  //  Some(RabbitMQConfKeys.imevents_credit)


  def configure(props: Props): Unit = {
    var prop = props.get(RabbitMQConfKeys.imevents_credit).getOr("")
    // Console.println("Prop: " + prop)
    val Array(exchange, routing) = prop.split(":")
    //Console.println("ex: " + exchange + " routing: " + routing)
     _conf = RabbitMQConsumerConf(
      tag = Tags.IMEventTag,
      exchangeName = exchange,
      routingKey = routing,
      queueName = "",
      connectionConf = RabbitMQKeys.makeConnectionConf(props),
      exchangeConf = RabbitMQKeys.DefaultExchangeConf,
      channelConf = RabbitMQKeys.DefaultChannelConf,
      queueConf = RabbitMQKeys.DefaultQueueConf
    )
    _factory = new ConnectionFactory
    _factory.setConnectionTimeout(_conf.connectionConf(RabbitMQConKeys.reconnect_period_millis).toInt)
    _factory.setUsername(_conf.connectionConf(RabbitMQConKeys.username))
    _factory.setPassword(_conf.connectionConf(RabbitMQConKeys.password))
    _factory.setVirtualHost(_conf.connectionConf(RabbitMQConKeys.vhost))
    _factory.setRequestedHeartbeat(_conf.connectionConf(RabbitMQConKeys.reconnect_period_millis).toInt)
    _servers = _conf.connectionConf(RabbitMQConKeys.servers)
  }

  private[this] def withChannel[A]( next : => A) = {
    try {
      lock.withLock {
      if (_connection == null ||_connection.isOpen == false )
         _connection =_factory.newConnection(_servers)
      if (_channel == null ||_channel.isOpen == false )
        _channel = _connection.createChannel
        assert(_connection.isOpen && _channel.isOpen)
        next
     }
    } catch {
        case e: Exception =>
          e.printStackTrace
    }
  }

  def sendMessage(payload:String) =
    withChannel {
      _channel.basicPublish(_conf.exchangeName, _conf.routingKey,
        MessageProperties.PERSISTENT_TEXT_PLAIN,
        payload.getBytes)
    }
}

object RabbitMQProducer {
  val propsfile = new FileStreamResource(new File("aquarium.properties"))
  @volatile private[this] var _props: Props = Props(propsfile)(StdConverters.AllConverters).getOr(Props()(StdConverters.AllConverters))
  val aquarium = new AquariumBuilder(_props, ResourceLocator.DefaultPolicyModel).
                update(Aquarium.EnvKeys.storeProvider, new MemStoreProvider).
                update(Aquarium.EnvKeys.eventsStoreFolder,Some(new File(".."))).
                build()


  def main(args: Array[String]) = {
    aquarium(Aquarium.EnvKeys.rabbitMQProducer).
    sendMessage("{\"userid\": \"pgerakios@grnet.gr\", \"state\":true}")
    Console.err.println("Message sent")
    aquarium.stop()
    ()
  }
}