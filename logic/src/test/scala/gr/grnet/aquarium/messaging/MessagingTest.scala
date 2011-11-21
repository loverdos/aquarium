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

import amqp.rabbitmq.confmodel._
import org.junit.Test
import org.junit.Assert._
import com.ckkloverdos.resource.DefaultResourceContext
import com.ckkloverdos.props.Props
import com.ckkloverdos.maybe.Just
import com.thoughtworks.xstream.XStream
import gr.grnet.aquarium.util.xstream.XStreamHelpers

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class MessagingTest {
  object RCFolders {
    val rabbitmq = "rabbitmq"

    val aquarium_dev = "aquarium_dev"

    val producers = "producers"
    val consumers = "consumers"
  }
  
  object PropFiles {
    val configurations = "configuration.properties"

    val aquarium_dev = "aquarium_dev.properties"

    val main_producer = "main_producer.properties"

    val main_consumer = "main_consumer.properties"
  }

  object PropKeys {
    val configurations = "configurations"

    // configuration
    val addresses = "addresses"
    val username  = "username"
    val password = "password"
    val host = "host"
    val virtualHost = "virtualHost"
    val port = "port"
    val connections = "connections"

    // connection
    val exchange = "exchange"
    val exchangeType = "exchangeType"
    val exchangeIsDurable = "exchangeIsDurable"
    val producers = "producers"
    val consumers = "consumers"

    // producer
    val routingKey = "routingKey"

    // consumer
    val queue = "queue"
  }
  
  object PropValues {
    val aquarium_dev = "aquarium_dev"
    val localhost = "localhost"
  }

  val baseRC = DefaultResourceContext
  val rabbitmqRC = baseRC / RCFolders.rabbitmq

  @Test
  def testConfigurationsExist {
    assertTrue(rabbitmqRC.getResource(PropFiles.configurations).isJust)
  }
  
  @Test
  def testConfigurations {
    val props = Props(PropFiles.configurations, rabbitmqRC).getOr(Props.empty)
    val confList = props.getTrimmedList(PropKeys.configurations)
    assertEquals(List(PropValues.aquarium_dev, PropValues.localhost), confList)

    val xs = XStreamHelpers.newXStream

    val cm1 = new RabbitMQConsumerModel("queue1")
    val pm1 = new RabbitMQProducerModel("routing.key.1")
    val con1 = new RabbitMQConnectionModel("exchnage1", "direct", true, List(pm1), List(cm1))
    val conf1 = new RabbitMQConfigurationModel("aquarium", "aquarium", "localhost", 5672, Nil, "/", List(con1))
    val confs = new RabbitMQConfigurationsModel2(List(conf1))
    
    val xml = xs.toXML(confs)
    println(xml)

    val xml2 = """<gr.grnet.aquarium.messaging.amqp.rabbitmq.confmodel.RabbitMQConfigurationsModel2>
      <configurations class="List">
        <RabbitMQConfigurationModel>
          <username>aquarium</username>
          <password>aquarium</password>
          <host>localhost</host>
          <port>5672</port>
          <addresses class="Nil"/>
          <virtualHost>/</virtualHost>
          <connections class="List">
            <RabbitMQConnectionModel>
              <exchange>exchnage1</exchange>
              <exchangeType>direct</exchangeType>
              <isDurable>true</isDurable>
              <producers class="List">
                <RabbitMQProducerModel>
                  <routingKey>routing.key.1</routingKey>
                </RabbitMQProducerModel>
              </producers>
              <consumers class="List">
                <RabbitMQConsumerModel>
                  <queue>queue1</queue>
                </RabbitMQConsumerModel>
              </consumers>
            </RabbitMQConnectionModel>
          </connections>
        </RabbitMQConfigurationModel>
      </configurations>
    </gr.grnet.aquarium.messaging.amqp.rabbitmq.confmodel.RabbitMQConfigurationsModel2>"""
    for(model2 <- XStreamHelpers.parseType[RabbitMQConfigurationsModel2](xml2, xs)) {
      println(model2.configurations(0).addresses)
    }
  }

  @Test
  def testConfigurationAquarium_DevExists {
    val aquariumDevRC = rabbitmqRC / RCFolders.aquarium_dev
    assertTrue(aquariumDevRC.getResource(PropFiles.aquarium_dev).isJust)
  }

  @Test
  def testConfigurationAquarium_Dev {
    val props = Props(PropFiles.aquarium_dev, rabbitmqRC / RCFolders.aquarium_dev).getOr(Props.empty)

    assertTrue(props.get(PropKeys.username).isJust)
    assertTrue(props.get(PropKeys.password).isJust)
    assertTrue(props.get(PropKeys.addresses).isJust)
    assertTrue(props.get(PropKeys.host).isJust)
    assertTrue(props.get(PropKeys.virtualHost).isJust)
    assertTrue(props.get(PropKeys.port).isJust)
    assertTrue(props.get(PropKeys.connections).isJust)
    
    assertEquals(Just(PropValues.aquarium_dev), props.get(PropKeys.connections))
  }

  @Test
  def testConnectionAquarium_DevExists {
    val aquariumDevRC2 = rabbitmqRC / RCFolders.aquarium_dev / RCFolders.aquarium_dev
    assertTrue(aquariumDevRC2.getResource(PropFiles.aquarium_dev).isJust)
  }
  
  @Test
  def testConnectionAquarium_Dev {
    val props = Props(PropFiles.aquarium_dev, rabbitmqRC / RCFolders.aquarium_dev / RCFolders.aquarium_dev).getOr(Props.empty)
    
    assertTrue(props.get(PropKeys.exchange).isJust)
    assertTrue(props.get(PropKeys.exchangeType).isJust)
    assertTrue(props.get(PropKeys.exchangeIsDurable).isJust)
    assertTrue(props.get(PropKeys.producers).isJust)
    assertTrue(props.get(PropKeys.consumers).isJust)

    assertEquals(List("main_producer"), props.getTrimmedList(PropKeys.producers))
    assertEquals(List("main_consumer"), props.getTrimmedList(PropKeys.consumers))
  }

  @Test
  def testProducerMainProducerExists {
    val rc = rabbitmqRC / RCFolders.aquarium_dev / RCFolders.aquarium_dev / RCFolders.producers
    assertTrue(rc.getResource(PropFiles.main_producer).isJust)
  }

  @Test
  def testProducerMainProducer {
    val props = Props(PropFiles.main_producer, rabbitmqRC / RCFolders.aquarium_dev / RCFolders.aquarium_dev / RCFolders.producers).getOr(Props.empty)

    assertTrue(props.get(PropKeys.routingKey).isJust)
  }




  @Test
  def testConsumerMainConsumerExists {
    val rc = rabbitmqRC / RCFolders.aquarium_dev / RCFolders.aquarium_dev / RCFolders.consumers
    assertTrue(rc.getResource(PropFiles.main_consumer).isJust)
  }

  @Test
  def testConsumerMainConsumer {
    val props = Props(PropFiles.main_consumer, rabbitmqRC / RCFolders.aquarium_dev / RCFolders.aquarium_dev / RCFolders.consumers).getOr(Props.empty)

    assertTrue(props.get(PropKeys.queue).isJust)
  }
}