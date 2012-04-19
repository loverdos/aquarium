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

package gr.grnet.aquarium.messaging

import gr.grnet.aquarium.util.RandomEventGenerator
import org.junit.Test
import akka.actor.Actor
import java.util.concurrent.{TimeUnit, CountDownLatch}
import akka.amqp._
import akka.config.Supervision.Permanent
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assume._
import gr.grnet.aquarium.{AquariumException, LogicTestsAssumptions}

/**
 *
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */

class AkkaAMQPTest extends RandomEventGenerator {

  @Test
  def testSendReceive() : Unit = {

    assumeTrue(LogicTestsAssumptions.EnableRabbitMQTests)

    val numMsg = 100
    val msgs = new AtomicInteger(0)

    val publisher = producer(aquarium_exchnage)

    class Consumer extends Actor {

      self.lifeCycle = Permanent

      def receive = {
        case Delivery(payload, routingKey, deliveryTag, isRedeliver, properties, sender) =>
          println(this + " Got message: %s (%d)".format(new String(payload), deliveryTag) +
            (if(isRedeliver) " - redelivered (%d)".format(deliveryTag) else ""))
          if (msgs.incrementAndGet() == 15) throw new AquariumException("Messed up")
          if (msgs.incrementAndGet() == 55) sender ! Reject(deliveryTag, true)
          sender ! Acknowledge(deliveryTag)
        case Acknowledged(deliveryTag) => println("Acked: " + deliveryTag)
        case _ => println("Unknown delivery")
      }

      override def preRestart(reason: Throwable) {
        println("Actor restarted: " + reason)
      }

      override def postRestart(reason: Throwable) {
        // reinit stable state after restart
      }
    }

    consumer("foo.#", "aquarium-amqp-test",
      aquarium_exchnage, Actor.actorOf(new Consumer), false)
    Thread.sleep(2000)

    (1 to numMsg).foreach{
      i =>  publisher ! new Message(i.toString.getBytes(), "foo.bar")
    }

    Thread.sleep(5000)

    //AMQP.shutdownAll
    Actor.registry.shutdownAll
  }
}