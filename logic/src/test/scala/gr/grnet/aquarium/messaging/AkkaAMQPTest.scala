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

import gr.grnet.aquarium.util.RandomEventGenerator
import org.junit.Test
import akka.actor.Actor
import java.util.concurrent.{TimeUnit, CountDownLatch}
import akka.amqp.{AMQP, Message, Delivery}

/**
 * 
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */

class AkkaAMQPTest extends RandomEventGenerator {

  @Test
  def testSendReceive() = {
    val numMsg = 100
    val countDown = new CountDownLatch(numMsg)
    
    val publisher = producer("aquarium")

    class Consumer extends Actor {
      def receive = {
        case Delivery(payload, _, _, _, _, _) =>
          println("Got message: " + payload)
          countDown.countDown()
        case _ =>
      }
    }

    consumer("akka-amqp-test", "foo.*", Actor.actorOf(new Consumer))

    (1 to numMsg).foreach{
      i =>  publisher ! new Message(i.toString.getBytes(), "foo.bar")
    }

    countDown.await(10, TimeUnit.SECONDS)
    AMQP.shutdownAll
    Actor.registry.shutdownAll
  }
}