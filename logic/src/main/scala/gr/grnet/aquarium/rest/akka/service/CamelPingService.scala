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

package gr.grnet.aquarium.rest.akka.service

import akka.actor.Actor
import akka.camel.Consumer
import org.apache.camel.Message
import scala.collection.JavaConversions._

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class CamelPingService(val host: String, val port: Int, path: String) extends Actor with Consumer {
  def endpointUri = "jetty:http://%s:%s%s".format(host, port, path)

  protected def receive = {
    case message: Message =>
      val theReply = "pong %s %s".format(System.currentTimeMillis(), message.getClass.getName)
      println("++ MessageID: %s".format(message.getMessageId))
      println("+++ HEADERS +++")
      for((k, v) <- message.getHeaders) {
        println("  %s -> %s".format(k, v))
      }
      println("--- HEADERS ---")
      println("++ BODY: %s".format(message.getBody))
      println("++ REPLY: %s".format(theReply))
      self reply theReply

    case message: AnyRef =>
      val theReply = "pong %s %s".format(System.currentTimeMillis(), message.getClass.getName)
      println("++ REPLY: %s".format(theReply))
      self reply theReply
  }
}