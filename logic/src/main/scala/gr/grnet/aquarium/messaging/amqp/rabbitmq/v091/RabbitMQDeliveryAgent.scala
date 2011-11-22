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

import scala.collection.JavaConversions._
import java.lang.String
import com.rabbitmq.client.{Envelope, DefaultConsumer}
import com.rabbitmq.client.AMQP.BasicProperties
import com.ckkloverdos.props.Props

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class RabbitMQDeliveryAgent(consumer: RabbitMQConsumer, handler: AMQPDeliveryHandler) extends AMQPDeliveryAgent {
  import RabbitMQDeliveryAgent.{EnvelopeKeys, BasicPropsKeys}

  val underlyingHandler = new DefaultConsumer(consumer._rabbitChannel) {
    override def handleDelivery(
        consumerTag: String,
        envelope: Envelope,
        properties: BasicProperties,
        body: Array[Byte]) = {

      val propsEnvelope = new Props(
        Map(
          EnvelopeKeys.consumerTag -> consumerTag,
          EnvelopeKeys.deliveryTag -> envelope.getDeliveryTag.toString,
          EnvelopeKeys.exchange    -> envelope.getExchange,
          EnvelopeKeys.routingKey  -> envelope.getRoutingKey,
          EnvelopeKeys.redeliver   -> envelope.isRedeliver.toString
        )
      )

      val propsHeader = new Props(
        Map(
          BasicPropsKeys.contentType -> properties.getContentType,
          BasicPropsKeys.contentEncoding -> properties.getContentEncoding,
//          BasicPropsKeys.headers -> properties.get,
          BasicPropsKeys.deliveryMode -> properties.getDeliveryMode.toString,
          BasicPropsKeys.priority -> properties.getPriority.toString,
          BasicPropsKeys.correlationId -> properties.getCorrelationId,
          BasicPropsKeys.replyTo -> properties.getReplyTo,
          BasicPropsKeys.expiration -> properties.getExpiration,
          BasicPropsKeys.messageId -> properties.getMessageId,
          BasicPropsKeys.timestamp -> properties.getTimestamp.toString,
          BasicPropsKeys.`type` -> properties.getType,
          BasicPropsKeys.userId -> properties.getUserId,
          BasicPropsKeys.appId -> properties.getAppId,
          BasicPropsKeys.clusterId -> properties.getClusterId
        )
      )
      handler.handleStringDelivery(propsEnvelope, propsHeader, new String(body, "UTF-8"))
    }
  }
  def deliverNext = {
    val queue = consumer.confModel.queue
    val autoAck = consumer.confModel.autoAck

    consumer._rabbitChannel.basicConsume(queue, autoAck, underlyingHandler)
  }
}
object RabbitMQDeliveryAgent {
  object EnvelopeKeys {
    val consumerTag = "consumerTag"
    val deliveryTag = "deliveryTag"
    val redeliver   = "redeliver"
    val exchange    = "exchange"
    val routingKey  = "routingKey"

  }

  object BasicPropsKeys {
    val contentType = "contentType"
    val contentEncoding = "contentEncoding"
//    val headers = "headers"
    val deliveryMode = "deliveryMode"
    val priority = "priority"
    val correlationId = "correlationId"
    val replyTo = "replyTo"
    val expiration = "expiration"
    val messageId = "messageId"
    val timestamp = "timestamp"
    val `type` = "type"
    val userId = "userId"
    val appId = "appId"
    val clusterId = "clusterId"
  }
}