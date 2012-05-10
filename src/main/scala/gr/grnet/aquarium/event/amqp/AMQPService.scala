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

package gr.grnet.aquarium.event.amqp

import gr.grnet.aquarium.Configurable
import com.ckkloverdos.props.Props
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.event.amqp.AMQPService.AMQPKeys
import gr.grnet.aquarium.util.{ReflectHelpers, Loggable, Lifecycle}
import com.rabbitmq.client.{ConnectionFactory, Address}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class AMQPService extends Loggable with Lifecycle with Configurable {
  private[this] val props: Props = Props()
  private[this] val amqpAddresses: Array[Address] = Array()
  private[this] val amqpUsername = ""
  private[this] val amqpPassword = ""
  private[this] val amqpVHost    = ""
  private[this] val amqpCF = new ConnectionFactory()

  def propertyPrefix = Some(AMQPKeys.Prefix)

  /**
   * Configure this instance with the provided properties.
   *
   * If `propertyPrefix` is defined, then `props` contains only keys that start with the given prefix.
   */
  def configure(props: Props)  = {
    ReflectHelpers.setField(this, "props", props)
    doConfigure()
    logger.info("Configured with {}", this.props)
  }

  private[this] def doConfigure(): Unit = {
    val servers = props.getTrimmedList(AMQPKeys.servers)
    val port    = props.getIntEx(AMQPKeys.port)
    val amqpAddresses = servers.map(new Address(_, port)).toArray
    ReflectHelpers.setField(this, "amqpAddresses", amqpAddresses)

    val amqpUsername = props.getEx(AMQPKeys.username)
    ReflectHelpers.setField(this, "amqpUsername", amqpUsername)

    val amqpPassword = props.getEx(AMQPKeys.password)
    ReflectHelpers.setField(this, "amqpPassword", amqpPassword)

    val amqpVHost = props.getEx(AMQPKeys.vhost)
    ReflectHelpers.setField(this, "amqpVHost", amqpVHost)

    val amqpExchange = props.getEx(AMQPKeys.exchange)

    // (e)xchange:(r)outing key:(q)
    val erq_res = props.getTrimmedList(AMQPKeys.resevents_queues)
    val resConsumerConfs = for(erq ← erq_res) yield {
      val (exchange, routingKey, queue) = erq.split(':')
      ConsumerConf(exchange, routingKey, queue)
    }
    val erq_im = props.getTrimmedList(AMQPKeys.userevents_queues)
    val imConsumerConfs = for(erq ← erq_im) yield {
      val (exchange, routingKey, queue) = erq.split(':')
      ConsumerConf(exchange, routingKey, queue)
    }
  }

  def start() = {
    logStarted(TimeHelpers.nowMillis(), TimeHelpers.nowMillis())
    System.exit(1)
  }

  def stop() = {
    logStopped(TimeHelpers.nowMillis(), TimeHelpers.nowMillis())
  }

  case class ConsumerConf(exchange: String, routingKey: String, queue: String)
  case class ProducerConf(exchange: String, routingKey: String)
}

object AMQPService {
  object AMQPKeys {
    final val Prefix = "amqp"
    final val PrefixAndDot = Prefix + "."

    private[this] def p(name: String) = PrefixAndDot + name
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
     */
    final val exchange = p("exchange")
    final val amqp_exchange = exchange

    /**
     * Queues for retrieving resource events from. Multiple queues can be
     * declared, separated by semicolon
     *
     * Format is `exchange:routing.key:queue-name;...`
     */
    final val resevents_queues = p("resevents.queues")
    final val amqp_resevents_queues = resevents_queues

    /**
     * Queues for retrieving user events from. Multiple queues can be
     * declared, separated by semicolon
     *
     * Format is `exchange:routing.key:queue-name;...`
     */
    final val userevents_queues = p("userevents.queues")
    final val amqp_userevents_queues = userevents_queues
  }
}
