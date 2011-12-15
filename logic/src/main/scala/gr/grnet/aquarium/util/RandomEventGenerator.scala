package gr.grnet.aquarium.util

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

import akka.amqp._
import gr.grnet.aquarium.logic.events.ResourceEvent
import gr.grnet.aquarium.messaging.AkkaAMQP
import util.Random

/**
 *  Generates random resource events to use as input for testing and
 *  injects them to the specified exchange.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait RandomEventGenerator extends AkkaAMQP {

  val userIds = 1 to 100
  val clientIds = 1 to 4
  val vmIds = 1 to 4000
  val resources = List("bandwidthup", "bandwidthdown", "vmtime", "diskspace")
  val tsFrom = 1293840000000L //1/1/2011 0:00:00 GMT
  val tsTo = 1325376000000L   //1/1/2012 0:00:00 GMT
  val eventVersion = 1 to 4

  private val seed = 0xdeadbeef
  private lazy val rnd = new Random(seed)

  /**
   * Get the next random message
   */
  def nextResourceEvent() : ResourceEvent = {
    val res = rnd.shuffle(resources).head

    val extra = res match {
      case "vmtime" => Map("vmid" -> rnd.nextInt(vmIds.max).toString)
      case _ => Map[String, String]()
    }

    val ts = tsFrom + (scala.math.random * ((tsTo - tsFrom) + 1)).asInstanceOf[Long]

    ResourceEvent(
      rnd.nextString(35),
      rnd.nextInt(userIds.max),
      rnd.nextInt(clientIds.max),
      res,ts,1,extra)
  }

  /**
   * Generate resource events and publish them to the queue
   */
  def genPublish(num: Int) = {

    assert(num > 0)
    val publisher = producer("aquarium")

    (1 to num).foreach {
      val event = nextResourceEvent
      n => publisher ! Message(event.toBytes,
        "event.%d.%s".format(event.cliendId, event.resource))
    }
  }
}

object RandomEventGenerator extends RandomEventGenerator {

  def main(args: Array[String]): Unit = {

    var num = 100

    if (args.size > 0)
      num = args.head.toInt

    println("Publishing %d messages. Hit Ctrl+c to stop".format(num))

    genPublish(num)
  }
}
