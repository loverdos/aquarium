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

import confmodel.RabbitMQConsumerModel
import com.rabbitmq.client.{Channel => JackRabbitChannel, Connection => JackRabbitConnection, ConnectionFactory => JackRabbitConnectionFactory}
import gr.grnet.aquarium.util.Loggable

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class RabbitMQConsumer(private[v091] val owner: RabbitMQConnection, val confModel: RabbitMQConsumerModel) extends AMQPConsumer with Loggable  {
  private[v091] val _rabbitChannel = {
    val _ch = owner._rabbitConnection.createChannel()
    logger.info("Created rabbit channel %s for %s".format(_ch, this.toString))
    val exchange = owner.confModel.exchange
    val exchangeType = owner.confModel.exchangeType
    val exchangeIsDurable = owner.confModel.isDurable
    val queue = confModel.queue
    val routingKey = confModel.routingKey
    val queueIsDurable = confModel.queueIsDurable
    val queueIsAutoDelete = confModel.queueIsAutoDelete
    val queueIsExclusive = confModel.queueIsExclusive

    val ed = _ch.exchangeDeclare(exchange, exchangeType, exchangeIsDurable)
    logger.info("Declared exchange %s for %s with result %s".format(exchange, this, ed))

    _ch.queueDeclare(queue, queueIsDurable, queueIsExclusive, queueIsAutoDelete, null)

    _ch.queueBind(queue, exchange, routingKey)
    _ch
  }

  def name = confModel.name

  def newDeliveryAgent(handler: AMQPDeliveryHandler) = new RabbitMQDeliveryAgent(this, handler)

  override def toString = {
    val connName = owner.name
    val confName = owner.owner.name
    "RabbitMQConsumer(%s/%s/%s)".format(confName, connName, name)
  }
}