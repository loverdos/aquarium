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

import confmodel.RabbitMQConnectionModel
import gr.grnet.aquarium.messaging.amqp.AMQPConnection

import com.rabbitmq.client.{Channel => JackRabbitChannel, Connection => JackRabbitConnection, ConnectionFactory => JackRabbitConnectionFactory}


/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class RabbitMQConnection(private[v091] val owner: RabbitMQConfiguration, val confModel: RabbitMQConnectionModel) extends AMQPConnection {
  private[v091] val _rabbitConnection: JackRabbitConnection = owner._rabbitConnectionFactory.newConnection()

  private val _name = confModel.name
  def name = _name

  private lazy val _producers = confModel.producers.map(new RabbitMQProducer(this, _))
  def producers = _producers

  private lazy val _consumers = confModel.consumers.map(new RabbitMQConsumer(this, _))
  def consumers = _consumers

  override def toString = {
    "RabbitMQConnection(%s/%s)".format(owner.name, name)
  }
}