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
import amqp.rabbitmq.v091.RabbitMQConfigurations
import amqp.rabbitmq.v091.RabbitMQConfigurations.{PropFiles, RCFolders}
import org.junit.Test
import org.junit.Assert._
import com.ckkloverdos.resource.DefaultResourceContext
import gr.grnet.aquarium.util.xstream.XStreamHelpers
import com.ckkloverdos.props.Props
import com.ckkloverdos.maybe.{Failed, NoVal, Just}
import org.junit.Assume._
import gr.grnet.aquarium.{PropertyNames, LogicTestsAssumptions}
import com.ckkloverdos.sys.SysProp
import gr.grnet.aquarium.util.{LogUtils, Loggable}
import java.util.Random

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class RabbitMQTest extends Loggable {

  val xs = XStreamHelpers.DefaultXStream
  val baseRC = DefaultResourceContext
  val rabbitmqRC = baseRC / RCFolders.rabbitmq

  def getProp(logger: org.slf4j.Logger, msg: String, name: String, default: String): String = {
    SysProp(name).value match {
      case Just(value) =>
        logger.debug("[%s] Found value '%s' for system property '%s'".format(msg, value, name))
        value
      case Failed(e, m) =>
        logger.error("[%s][%s]: %s".format(m, e.getClass.getName, e.getMessage))
        logger.error("[%s] Error loading system property '%s'. Using default value '%s'".format(msg, name, default))
        default
      case NoVal =>
        logger.debug("[%s] Found no value for system property '%s'. Using default '%s'".format(msg, name, default))
        default
    }
  }

  // The configuration file we use for the rabbitmq tests
  lazy val RabbitMQPropFile = {
    val filename = LogUtils.getSysProp(logger, "Loading rabbitmq configurations", PropertyNames.RabbitMQConfFile, PropFiles.configurations)
    logger.debug("Using rabbitmq configurations from %s".format(filename))
    filename
  }

  // The specific setup we use.
  // This is defined in the configuration file
  lazy val RabbitMQSpecificConfName = {
    val confname = LogUtils.getSysProp(logger, "Getting specific rabbitmq configuration", PropertyNames.RabbitMQSpecificConf, Names.aquarium_dev_grnet_gr)
    logger.debug("Using specific rabbitmq configuration: %s".format(confname))
    confname
  }

  // The connection we use for testing
  lazy val RabbitMQConnectionName = {
    val conname = LogUtils.getSysProp(logger, "Getting rabbitmq connection", PropertyNames.RabbitMQConnection, Names.test_connection)
    logger.debug("Using rabbitmq connection %s".format(conname))
    conname
  }

  // The producer we use for testing
  lazy val RabbitMQProducerName = {
    val pname = LogUtils.getSysProp(logger, "Getting rabbitmq producer", PropertyNames.RabbitMQProducer, Names.test_producer)
    logger.debug("Using rabbitmq producer %s".format(pname))
    pname
  }

  // The producer we use for testing
  lazy val RabbitMQConsumerName = {
    val cname = LogUtils.getSysProp(logger, "Getting rabbitmq consumer", PropertyNames.RabbitMQConsumer, Names.test_consumer)
    logger.debug("Using rabbitmq producer %s".format(cname))
    cname
  }

  object Names {
//    val default_connection = "default_connection"
    val test_connection = "test_connection"
    val aquarium_dev_grnet_gr = "aquarium.dev.grnet.gr"
    val test_producer = "test_producer"
    val test_consumer = "test_consumer"
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
    assertTrue(rabbitmqRC.getResource(RabbitMQPropFile).isJust)
  }

  @Test
  def testProducer {
    assumeTrue(LogicTestsAssumptions.EnableRabbitMQTests)

    val maybeResource = rabbitmqRC.getResource(RabbitMQPropFile)
    assertTrue(maybeResource.isJust)
    val maybeProducer = for {
      resource <- maybeResource
      confs    <- RabbitMQConfigurations(resource, xs)
      conf     <- confs.findConfiguration(RabbitMQSpecificConfName)
      conn     <- conf.findConnection(RabbitMQConnectionName)
      producer <- conn.findProducer(RabbitMQProducerName)
    } yield {
      producer
    }
    
    logger.debug("Found producer %s".format(maybeProducer))

    maybeProducer match {
      case Just(producer) =>
        logger.debug("Using %s to publish a message".format(producer))
        val message = "Test message " + new java.util.Random(System.currentTimeMillis()).nextInt
        producer.publishString(message)
        logger.debug("Used %s to publish message %s".format(producer, message))
      case NoVal =>
        fail("No producer named %s".format(RabbitMQProducerName))
      case Failed(e, m) =>
        fail("%s: %s".format(m, e.getMessage))
    }
  }

  @Test
  def testConsumer {
    assumeTrue(LogicTestsAssumptions.EnableRabbitMQTests)

    val maybeResource = rabbitmqRC.getResource(RabbitMQPropFile)
    assertTrue(maybeResource.isJust)

    val maybeConsumer = for {
      resource <- maybeResource
      confs    <- RabbitMQConfigurations(resource, xs)
      conf     <- confs.findConfiguration(RabbitMQSpecificConfName)
      conn     <- conf.findConnection(RabbitMQConnectionName)
      consumer <- conn.findConsumer(RabbitMQConsumerName)
    } yield {
      consumer
    }

    maybeConsumer match {
      case Just(consumer) =>
        logger.debug("Receiving a message from %s".format(consumer))
        val agent = consumer.newDeliveryAgent(new AMQPDeliveryHandler {
          def handleStringDelivery(envelope: Props, headers: Props, content: String): Boolean = {
            logger.debug("Received message with")
            logger.debug("  envelope: %s".format(envelope))
            logger.debug("  headers : %s".format(headers))
            logger.debug("  body    : %s".format(content))

            true
          }
        })
        // wait until delivery
        agent.deliverNext
      case NoVal =>
        fail("No consumer named %s".format(RabbitMQConsumerName))
      case Failed(e, m) =>
        fail("%s: %s".format(m, e.getMessage))
    }
  }

}