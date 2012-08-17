package gr.grnet.aquarium.connector.rabbitmq

import conf.{RabbitMQKeys, RabbitMQConsumerConf}
import conf.RabbitMQKeys.{RabbitMQConfKeys, RabbitMQConKeys}
import gr.grnet.aquarium._
import com.rabbitmq.client._
import com.ckkloverdos.props.Props
import gr.grnet.aquarium.converter.StdConverters
import gr.grnet.aquarium.util.Lock
import gr.grnet.aquarium.store.memory.MemStoreProvider
import java.io.File
import com.ckkloverdos.resource.FileStreamResource
import scala.Some
import collection.immutable.{TreeMap, TreeSet}


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

/**
 *
 * @author Prodromos Gerakios <pgerakios@grnet.gr>
 */

class RabbitMQProducer extends Configurable {
  private[this] var _conf: RabbitMQConsumerConf = _
  private[this] var _factory: ConnectionFactory = _
  private[this] var _connection: Connection = _
  private[this] var _channel: Channel = _
  private[this] var _servers : Array[Address] = _
  private[this] final val lock = new Lock()
  private[this] var _exchangeName : String = _
  private[this] var _routingKey :String = _

  def propertyPrefix: Option[String] = Some(RabbitMQKeys.PropertiesPrefix)
  //  Some(RabbitMQConfKeys.imevents_credit)


  @volatile private[this] var _unconfirmedSet = new TreeSet[Long]()
  @volatile private[this] var _unconfirmedMessages = new TreeMap[Long,String]()

  def configure(props: Props): Unit = {
    val propName = RabbitMQConfKeys.imevents_credit
    def exn () = throw new AquariumInternalError(new Exception, "While obtaining value for key %s in properties".format(propName))
    val prop = props.get(propName).getOr(exn())
    if (prop.isEmpty) exn()
    val connectionConf = RabbitMQKeys.makeConnectionConf(props)
    val Array(exchangeName, routingKey) = prop.split(":")
    _exchangeName = exchangeName
    _routingKey = routingKey
    _factory = new ConnectionFactory
    _factory.setConnectionTimeout(connectionConf(RabbitMQConKeys.reconnect_period_millis).toInt)
    _factory.setUsername(connectionConf(RabbitMQConKeys.username))
    _factory.setPassword(connectionConf(RabbitMQConKeys.password))
    _factory.setVirtualHost(connectionConf(RabbitMQConKeys.vhost))
    _factory.setRequestedHeartbeat(connectionConf(RabbitMQConKeys.reconnect_period_millis).toInt)
    _servers = connectionConf(RabbitMQConKeys.servers)
    _connection =_factory.newConnection(_servers)
    _channel = _connection.createChannel
    _channel.confirmSelect
    _channel.addConfirmListener(new ConfirmListener {

      private [this] def subset(seqNo:Long,multiple:Boolean) : TreeMap[Long,String] = {
         val set = if (multiple)
                    _unconfirmedSet.range(0,seqNo+1)
                   else
                    _unconfirmedSet.range(seqNo,seqNo)
         _unconfirmedSet = _unconfirmedSet -- set
         val ret : TreeMap[Long,String] = set.foldLeft(TreeMap[Long,String]())({(map,seq)=>
           _unconfirmedMessages.get(seq) match{
             case None => map
             case Some(s) => map + ((seq,s))
         }})
         _unconfirmedMessages = _unconfirmedMessages -- set
        ret
       }


      def handleAck(seqNo:Long,multiple:Boolean) = {
        withChannel {
          //Console.err.println("Received ack for msg " + _unconfirmedMessages.get(seqNo) )
          subset(seqNo,multiple)
        }
      }

      def handleNack(seqNo:Long,multiple:Boolean) = {
        withChannel {
          //Console.err.println("Received Nack for msg " + _unconfirmedMessages.get(seqNo) )
          for { (_,msg) <- subset(seqNo,multiple) }
            sendMessage(msg)
        }
      }
    })
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
      var seq : Long = _channel.getNextPublishSeqNo()
      _unconfirmedSet += seq
      _unconfirmedMessages += ((seq,payload))
      _channel.basicPublish(_exchangeName,_routingKey,
                            MessageProperties.PERSISTENT_TEXT_PLAIN,
                            payload.getBytes)
    }
}

object RabbitMQProducer  {
  def main(args: Array[String]) = {
    val propsfile = new FileStreamResource(new File("aquarium.properties"))
    var _props: Props = Props(propsfile)(StdConverters.AllConverters).getOr(Props()(StdConverters.AllConverters))
    val aquarium = new AquariumBuilder(_props, ResourceLocator.DefaultPolicyModel).
    update(Aquarium.EnvKeys.storeProvider, new MemStoreProvider).
    update(Aquarium.EnvKeys.eventsStoreFolder, Some(new File(".."))).
    build()
    aquarium(Aquarium.EnvKeys.rabbitMQProducer).sendMessage("Test string !!!!")
    Console.err.println("Message sent")
    //aquarium.start()
    ()
  }
}