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

import com.rabbitmq.client.Address
import akka.actor._
import akka.amqp.{Topic, AMQP}
import akka.amqp.AMQP._

/**
 * Functionality for working with queues.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */

trait AkkaAMQP {

  private lazy val connection = AMQP.newConnection(
    ConnectionParameters(
      Array(new Address("1.2.3.4",5672)),
      "foob",
      "bar",
      "/",
      5000,
      None))

  //Queues and exchnages are by default durable and persistent
  val decl = ActiveDeclaration(durable = true, autoDelete = false)

  //TODO: Methods to load configuration from {file, other resource}

  def consumer(routekey: String, queue: String, receiver: ActorRef) =
    AMQP.newConsumer(
      connection = connection,
      consumerParameters = ConsumerParameters(
        routingKey = routekey,
        deliveryHandler = receiver,
        queueName = Some(queue),
        queueDeclaration = decl
        ))

  def producer(exchange: String) = AMQP.newProducer(
      connection = connection,
      producerParameters = ProducerParameters(
        exchangeParameters = Some(ExchangeParameters(exchange, Topic, decl)),
        channelParameters = Some(ChannelParameters(prefetchSize = 1))))
}