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

import com.rabbitmq.client.{Channel => JackRabbitChannel, Connection => JackRabbitConnection, ConnectionFactory => JackRabbitConnectionFactory}
import confmodel.RabbitMQConfigurationModel
import gr.grnet.aquarium.util.Loggable

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class RabbitMQConfiguration(val confModel: RabbitMQConfigurationModel) extends AMQPConfiguration with Loggable {
  private[v091] val _rabbitConnectionFactory = {
    val _cf = new JackRabbitConnectionFactory

    _cf.setUsername(confModel.username)
    _cf.setPassword(confModel.password)
    _cf.setHost(confModel.host)
    _cf.setPort(confModel.port)
    _cf.setVirtualHost(confModel.virtualHost)

    _cf
  }

  private val _name = confModel.name
  def name = _name
  
  private lazy val _connections = confModel.connections.map(new RabbitMQConnection(this, _))
  def connections = _connections

  override def toString = {
    "RabbitMQConfiguration(%s)".format(name)
  }
}


object RabbitMQConfiguration {
  object RCFolders {
    val rabbitmq = "rabbitmq"
  }
}