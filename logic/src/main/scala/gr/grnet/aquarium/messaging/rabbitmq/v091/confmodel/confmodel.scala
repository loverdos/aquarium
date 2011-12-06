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
package confmodel

import gr.grnet.aquarium.util.ConfModel
import gr.grnet.aquarium.util.ConfModel.ConfModelError

sealed trait RabbitMQConfModel extends ConfModel {
  def validateConfModel: List[ConfModelError] = Nil
}

case class RabbitMQConfigurationsModel(configurations: List[RabbitMQConfigurationModel]) extends RabbitMQConfModel

case class RabbitMQConfigurationModel(
    name: String,
    username: String,
    password: String,
    host: String,
    port: Int,
    addresses: List[String],
    virtualHost: String,
    connections: List[RabbitMQConnectionModel]
) extends RabbitMQConfModel {
  
  override def validateConfModel = {
    ConfModel.applyValidations(
      (() => host.size > 0 || addresses.size > 0, "At least one of host, addresses must be provided"),
      (() => connections.size > 0               , "At least one connection must be specified")
    )
  }
}

case class RabbitMQConnectionModel(
    name: String,
    exchange: String,
    exchangeType: String,
    isDurable: Boolean,
    producers: List[RabbitMQProducerModel],
    consumers: List[RabbitMQConsumerModel]
) extends RabbitMQConfModel {

}

case class RabbitMQProducerModel(
    name: String,
    routingKey: String) extends RabbitMQConfModel

case class RabbitMQConsumerModel(
    name: String,
    queue: String,
    routingKey: String,
    autoAck: Boolean,
    queueIsDurable: Boolean,
    queueIsExclusive: Boolean,
    queueIsAutoDelete: Boolean) extends RabbitMQConfModel
