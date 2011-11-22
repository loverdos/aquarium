/*
 * Copyright 2011 GRNET S.A. All rights reserved.
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

package gr.grnet.aquarium.messaging

import amqp.AMQPDeliveryHandler
import amqp.rabbitmq.v091.confmodel._
import amqp.rabbitmq.v091.RabbitMQConfigurations.{PropFiles, RCFolders}
import amqp.rabbitmq.v091.{RabbitMQConsumer, RabbitMQConfigurations}
import org.junit.Test
import org.junit.Assert._
import com.ckkloverdos.resource.DefaultResourceContext
import gr.grnet.aquarium.util.xstream.XStreamHelpers
import gr.grnet.aquarium.util.Loggable
import com.ckkloverdos.props.Props
import com.ckkloverdos.maybe.{Failed, NoVal, Just}

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class MessagingTest extends Loggable {

  val baseRC = DefaultResourceContext
  val rabbitmqRC = baseRC / RCFolders.rabbitmq

  object Names {
    val consumer1 = "consumer1"
    val producer1 = "producer1"
    val queue1 = "queue1"
    val routing_key_all = "routing.key.all"
    val local_connection = "local_connection"
    val aquarium_exchange = "aquarium_exchange"
    val direct = "direct"
    val localhost_aquarium = "localhost_aquarium"
    val aquarium = "aquarium"
    val localhost = "localhost"
  }

  private def _genTestConf: String = {
    val consmod1 = new RabbitMQConsumerModel("consumer1", "queue1", "routing.key.all", true, true, false, false)
    val prodmod1 = new RabbitMQProducerModel("producer1", "routing.key.all")
    val conn1 = new RabbitMQConnectionModel(
      "local_connection",
    "aquarium_exchange",
    "direct",
    true,
    List(prodmod1),
    List(consmod1)
    )
    val conf1 = new RabbitMQConfigurationModel(
    "localhost_aquarium",
    "aquarium",
    "aquarium",
    "localhost",
    5672,
    Nil,
    "/",
    List(conn1)
    )

    val model = new RabbitMQConfigurationsModel(List(conf1))
    val xs = XStreamHelpers.newXStream
    val xml = xs.toXML(model)

    xml
  }

  @Test
  def testConfigurationsExist {
    assertTrue(rabbitmqRC.getResource(PropFiles.configurations).isJust)
  }

  @Test
  def testLocalProducer {
    val maybeConfs = RabbitMQConfigurations(baseRC)
    assertTrue(maybeConfs.isJust)
    val maybeProducer = for {
      confs    <- maybeConfs
      conf     <- confs.findConfiguration(Names.localhost_aquarium)
      conn     <- conf.findConnection(Names.local_connection)
      producer <- conn.findProducer(Names.producer1)
    } yield {
      producer
    }

    maybeProducer match {
      case Just(producer) =>
        logger.debug("Publishing a message from %s".format(producer))
        producer.publishString("Test")
      case NoVal =>
        fail("No producer named %s".format(Names.producer1))
      case Failed(e, m) =>
        fail("%s: %s".format(m, e.getMessage))
    }
  }

  @Test
  def testLocalConsumer {
    val maybeConfs = RabbitMQConfigurations(baseRC)
    assertTrue(maybeConfs.isJust)

    val maybeConsumer = for {
      confs    <- maybeConfs
      conf     <- confs.findConfiguration(Names.localhost_aquarium)
      conn     <- conf.findConnection(Names.local_connection)
      consumer <- conn.findConsumer(Names.consumer1)
    } yield {
      consumer
    }

    maybeConsumer match {
      case Just(consumer) =>
        logger.debug("Receiving a message from %s".format(consumer))
        consumer.newDeliveryAgent(new AMQPDeliveryHandler {
          def handleStringDelivery(envelope: Props, headers: Props, content: String) = {
            logger.debug("Received message with")
            logger.debug("  envelope: %s".format(envelope))
            logger.debug("  headers : %s".format(headers))
            logger.debug("  body    : %s".format(content))
          }
        })
      case NoVal =>
        fail("No consumer named %s".format(Names.consumer1))
      case Failed(e, m) =>
        fail("%s: %s".format(m, e.getMessage))
    }
  }

}