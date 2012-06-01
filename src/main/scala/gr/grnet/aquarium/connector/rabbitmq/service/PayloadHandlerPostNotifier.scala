/*
 * Copyright 2011-2012 GRNET S.A. All rights reserved.
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

package gr.grnet.aquarium.connector.rabbitmq.service

import org.slf4j.Logger
import com.ckkloverdos.maybe.{Failed, Just, Maybe}
import gr.grnet.aquarium.connector.rabbitmq.RabbitMQConsumer
import gr.grnet.aquarium.connector.handler.{HandlerResultPanic, HandlerResult}
import sun.rmi.log.LogHandler
import gr.grnet.aquarium.util.{LogHelpers, shortInfoOf}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

final class PayloadHandlerPostNotifier(logger: Logger) extends ((RabbitMQConsumer, Maybe[HandlerResult]) ⇒ Unit) {
  def apply(consumer: RabbitMQConsumer, maybeResult: Maybe[HandlerResult]) = {
    maybeResult match {
      case Just(hr @ HandlerResultPanic(reason)) ⇒
        // The other end is crucial to the overall operation and it is in panic mode,
        // so we stop delivering messages until further notice
        val errMsg = "Shutting down %s due to [%s]".format(consumer.toString, hr)
        logger.error(errMsg)
        consumer.setAllowReconnects(false)
        consumer.safeStop()

      case Failed(e) ⇒
        val errMsg = "Shutting down %s due to [%s]".format(consumer.toString, shortInfoOf(e))
        logger.warn(errMsg)
        consumer.setAllowReconnects(false)
        consumer.safeStop()

      case _ ⇒
    }
  }
}
