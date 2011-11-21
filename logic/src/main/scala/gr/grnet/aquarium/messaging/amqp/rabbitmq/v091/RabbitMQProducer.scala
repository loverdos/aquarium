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

package gr.grnet.aquarium.messaging.amqp
package rabbitmq
package v091

import confmodel.RabbitMQProducerModel
import gr.grnet.aquarium.messaging.amqp.AMQPProducer
import com.rabbitmq.client.{Channel => JackRabbitChannel, Connection => JackRabbitConnection, ConnectionFactory => JackRabbitConnectionFactory}
import com.rabbitmq.client.AMQP.BasicProperties

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class RabbitMQProducer(owner: RabbitMQConnection, val confModel: RabbitMQProducerModel) extends AMQPProducer {
  private val underlyingChannel = owner.underlyingConnection.createChannel()

  def name = confModel.name

  private def _publish[A](message: String, headers: Map[String, String])(pre: JackRabbitChannel => Any)(post: JackRabbitChannel => A): A = {
    import scala.collection.JavaConversions._
    val jrChannel = underlyingChannel
    val exchange = owner.confModel.exchange
    val routingKey = confModel.routingKey
    val jrProps = new BasicProperties.Builder().headers(headers).build()

    pre(jrChannel)
    jrChannel.basicPublish(exchange, routingKey, jrProps, message.getBytes("UTF-8"))
    post(jrChannel)
  }

  def publish(message: String, headers: Map[String, String] = Map()) = {
    _publish(message, headers){_ =>}{_ => ()}
  }

  def publishWithConfirm(message: String, headers: Map[String, String] = Map()) = {
    _publish(message, headers){_.confirmSelect()}{_.waitForConfirms()}
  }
}