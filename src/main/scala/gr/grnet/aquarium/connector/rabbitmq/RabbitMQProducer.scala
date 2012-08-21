package gr.grnet.aquarium.connector.rabbitmq

import conf.{RabbitMQKeys, RabbitMQConsumerConf}
import conf.RabbitMQKeys.{RabbitMQConfKeys, RabbitMQConKeys}
import gr.grnet.aquarium._
import com.rabbitmq.client._
import com.ckkloverdos.props.Props
import gr.grnet.aquarium.converter.StdConverters
import util.{Loggable, Lock}
import gr.grnet.aquarium.store.memory.MemStoreProvider
import java.io.File
import com.ckkloverdos.resource.FileStreamResource
import scala.Some
import collection.immutable.{TreeMap, TreeSet}
import java.util.concurrent.atomic.AtomicLong
import akka.actor.{Actor, ActorRef}
import com.google.common.eventbus.Subscribe
import gr.grnet.aquarium.service.event.AquariumCreatedEvent
import collection.mutable


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

private class RabbitMQProducerActor extends Actor {
  def receive = {
    case sendMessage:(() => Unit) =>
      //Console.err.println("Executing msg ... " + sendMessage.hashCode)
      sendMessage()
    case x  : AnyRef     =>
      //Console.err.println("Dammit  ..." + x.getClass.getSimpleName)
      ()
  }
}

/**
 *
 * @author Prodromos Gerakios <pgerakios@grnet.gr>
 */

class RabbitMQProducer extends AquariumAwareSkeleton with Configurable with Loggable  {
  private[this] var _conf: RabbitMQConsumerConf = _
  private[this] var _factory: ConnectionFactory = _
  private[this] var _connection: Connection = _
  private[this] var _channel: Channel = _
  private[this] var _servers : Array[Address] = _
  private[this] final val lock = new Lock()

  def propertyPrefix: Option[String] = Some(RabbitMQKeys.PropertiesPrefix)
  //  Some(RabbitMQConfKeys.imevents_credit)


  @volatile private[this] var _unsentMessages = mutable.Queue[()=>Unit]()
  @volatile private[this] var _unconfirmedSet = new TreeSet[Long]()
  @volatile private[this] var _unconfirmedMessages = new TreeMap[Long,()=>Unit]()


  @volatile private[this] var _actorRef : ActorRef = _
  private[this] var _resendPeriodMillis = 1000L


  @Subscribe
  override def awareOfAquarium(event: AquariumCreatedEvent) = {
    super.awareOfAquarium(event)
    assert(aquarium!=null && aquarium.akkaService != null)
    resendMessages        // start our daemon
  }

  private[this] def resendMessages() : Unit = {
    aquarium.timerService.scheduleOnce(
    "RabbitMQProducer.resendMessages",
    {
      //Console.err.println("RabbitMQProducer Timer ...")
      if(_actorRef==null) {
        _actorRef =  aquarium.akkaService.createNamedActor[RabbitMQProducerActor]("RabbitMQProducerActor")
      }
      if(_actorRef != null){
       //Console.err.println("RabbitMQProducer Timer --> messages ...")
       var msgs : mutable.Queue[()=>Unit] = null
       lock.withLock {
          if(isChannelOpen) {
            msgs  = _unsentMessages
            _unsentMessages = mutable.Queue[()=>Unit]()
          }
       }
       if(msgs!=null){
         //if(msgs.length>0) Console.err.println("RabbitMQProducer Timer --> messages ..." + msgs.length)
         for {msg <- msgs} {
          // Console.err.println("RabbitMQProducer Timer sending message .." + msg.hashCode)
           _actorRef ! msg
         }
       }
      } else {
        //Console.err.println("Akka ActorSystem is null. Waiting ...")
      }
      resendMessages()
    },
    this._resendPeriodMillis,
    true
    )
    ()
  }

  def configure(props: Props): Unit = {
    val connectionConf = RabbitMQKeys.makeConnectionConf(props)
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

      private [this] def cutSubset(seqNo:Long,multiple:Boolean) : TreeMap[Long,()=>Unit] =
        lock.withLock {
         val set = if (multiple)
                    _unconfirmedSet.range(0,seqNo+1)
                   else
                    _unconfirmedSet.range(seqNo,seqNo)
         _unconfirmedSet = _unconfirmedSet -- set
         val ret : TreeMap[Long,()=>Unit] = set.foldLeft(TreeMap[Long,()=>Unit]())({(map,seq)=>
           _unconfirmedMessages.get(seq) match{
             case None => map
             case Some(s) => map + ((seq,s))
         }})
         _unconfirmedMessages = _unconfirmedMessages -- set
        ret
       }


      def handleAck(seqNo:Long,multiple:Boolean) = {
        //Console.err.println("Received ack for  " + seqNo)
        cutSubset(seqNo,multiple)
      }

      def handleNack(seqNo:Long,multiple:Boolean) = {
        //Console.err.println("Received Nack for msg for " + seqNo)
        for {msg <- cutSubset(seqNo,multiple)} _actorRef ! msg
      }
    })
  }

  private[this] def isChannelOpen: Boolean =
    lock.withLock{
    if (_connection == null ||_connection.isOpen == false )
      _connection =_factory.newConnection(_servers)
    if (_channel == null ||_channel.isOpen == false )
      _channel = _connection.createChannel
    _connection.isOpen && _channel.isOpen
    }

  def fun(s:String)  : () => Unit = {
     () => {}
  }

  def sendMessage(exchangeName:String,routingKey:String,payload:String) = {
    def msg () : Unit =
      lock.withLock {
        try {
             if(isChannelOpen) {
              var seq : Long = _channel.getNextPublishSeqNo()
              _unconfirmedSet += seq
              _unconfirmedMessages += ((seq,msg))
              _channel.basicPublish(exchangeName,routingKey,
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                payload.getBytes)
               //Console.err.println("####Sent message " + payload + " with seqno=" + seq)
            } else {
              _unsentMessages += msg
               //Console.err.println("####Channel closed!")
             }
        } catch {
            case e: Exception =>
              _unsentMessages += msg
              //e.printStackTrace
        }
      }
    if(_actorRef != null)
      _actorRef ! msg
    else
      lock.withLock(_unsentMessages += msg)
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

    aquarium.start()

    //RabbitMQProducer.wait(1000)
    val propName = RabbitMQConfKeys.imevents_credit
    def exn () =
      throw new AquariumInternalError(new Exception, "While obtaining value for key %s in properties".format(propName))
    val prop = _props.get(propName).getOr(exn())
    if (prop.isEmpty) exn()
    val Array(exchangeName, routingKey) = prop.split(":")
    aquarium(Aquarium.EnvKeys.rabbitMQProducer).sendMessage(exchangeName,routingKey,"Test string !!!!")
    Console.err.println("Message sent")
    ()
  }
}