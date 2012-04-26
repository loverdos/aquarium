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

package gr.grnet.aquarium.util

import akka.amqp._
import util.Random
import scopt.OptionParser
import gr.grnet.aquarium.messaging.AkkaAMQP
import java.lang.StringBuffer
import gr.grnet.aquarium.logic.accounting.Policy
import gr.grnet.aquarium.event.im.{StdIMEvent, IMEventModel}
import gr.grnet.aquarium.event.resource.{StdResourceEvent, ResourceEventModel}

/**
 *  Generates random resource events to use as input for testing and
 *  injects them to the specified exchange.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait RandomEventGenerator extends AkkaAMQP {

  val userIds = 1 to 1000
  val clientIds = 1 to 4
  val vmIds = 1 to 4000
  val resources = Policy.policy.resources.map{r => r.name}
  val tsFrom = 1293840000000L //1/1/2011 0:00:00 GMT
  val tsTo = 1325376000000L   //1/1/2012 0:00:00 GMT
  val eventVersion = 1 to 4

  private val seed = 0xdeadbeef
  private lazy val rnd = new Random(seed)

  /**
   * Generate a random resource event
   */
  def nextUserEvent(): IMEventModel = {
    val ts = tsFrom + (scala.math.random * ((tsTo - tsFrom) + 1)).asInstanceOf[Long]

    new StdIMEvent(
      id = CryptoUtils.sha1(genRndAsciiString(35)),
      occurredMillis = ts.toLong,
      receivedMillis = ts.toLong,
      userID = userIds(rnd.nextInt(100)).toString,
      clientID = "defclient",
      isActive = rnd.nextBoolean,
      role = Array("PROF", "STUDENT", "ADMIN").apply(rnd.nextInt(3)),
      eventVersion = 1.toString,
      eventType = Array("ACTIVE", "SUSPENDED").apply(rnd.nextInt(2)),
      details = Map()
    )
  }

  /**
   * Generate a random resource event
   */
  def genPublishUserEvents(num: Int) = {
    val publisher = producer(im_exchanges(0))

    (1 to num).foreach {
      n =>
        var event = nextUserEvent()
        publisher ! Message(event.toJsonString.getBytes, "astakos.user")
    }
  }

  /**
   * Generete and publish create events for test users
   */
  def initUsers(num: Int) = {
    val publisher = producer(im_exchanges(0))

    userIds.filter(_ < num).foreach {
      i =>
        val ts = tsFrom + (scala.math.random * ((tsTo - tsFrom) + 1)).asInstanceOf[Long]
        val user = new StdIMEvent(
          id = CryptoUtils.sha1(genRndAsciiString(35)),
          occurredMillis = ts.toLong,
          receivedMillis = ts.toLong,
          userID = i.toString,
          clientID = "defclient",
          isActive = rnd.nextBoolean,
          role = Array("PROF", "STUDENT", "ADMIN").apply(rnd.nextInt(3)),
          eventVersion = 1.toString,
          eventType = "CREATE",
          details = Map()
        )
        publisher ! Message(user.toJsonString.getBytes, "astakos.user")
    }
  }

  /**
   * Get the next random resource event
   */
  def nextResourceEvent() : ResourceEventModel = {
    val res = rnd.shuffle(resources).head

    val extra = res match {
      case "vmtime" => Map("vmid" -> rnd.nextInt(vmIds.max).toString)
      case _ => Map[String, String]()
    }

    val value = res match {
      case "vmtime" => rnd.nextInt(1)
      case _ => rnd.nextInt(5000)
    }

    val ts = tsFrom + (scala.math.random * ((tsTo - tsFrom) + 1)).asInstanceOf[Long]
    val str = genRndAsciiString(35)

    new StdResourceEvent(
      CryptoUtils.sha1(str),
      ts, ts,
      rnd.nextInt(userIds.max).toString,
      rnd.nextInt(clientIds.max).toString,
      res, "1", value, 1.toString, extra)
  }

  def genRndAsciiString(size: Int): String = {
    (1 to size).map{
      i => rnd.nextPrintableChar()
    }.foldLeft(new StringBuffer()){
      (a, b) => a.append(b)
    }.toString
  }

  /**
   * Generate resource events and publish them to the queue
   */
  def genPublishResEvents(num: Int) = {

    assert(num > 0)
    val publisher = producer(resevent_exchanges(0))

    (1 to num).foreach {
      n =>
        var event = nextResourceEvent
        publisher ! Message(event.toBytes,
          "%s.%s.%s".format("",event.clientID, event.resource))
    }
  }
}

object RandomEventGen extends RandomEventGenerator {

  case class Config(var i: Boolean = false,
                    var u: Boolean = false,
                    var r: Boolean = false,
                    var nummsg: Int = 100)

  val config = new Config

  private val parser = new OptionParser("RandomEventGen") {
    opt("i", "im-events", "Generate IM events", {config.i = true})
    opt("u", "user-create", "Generate IM events that create users", {config.u = true})
    opt("r", "resource-events", "Generate resource events", {config.r = true})
    arg("nummsgs", "Number of msgs to generate", {num: String => config.nummsg = Integer.parseInt(num)})
  }

  def main(args: Array[String]): Unit = {

    if (!parser.parse(args))
      errexit

    if (!config.i && !config.u && !config.r) {
      println("One of -i, -u, -r must be specified")
      errexit
    }

    println("Publishing %d msgs, hit Ctrl+c to stop".format(config.nummsg))
    if (config.r) genPublishResEvents(config.nummsg)
    if (config.u) initUsers(config.nummsg)
    if (config.i) genPublishUserEvents(config.nummsg)
  }
  
  private def errexit() = {
    print(parser.usage)
    System.exit(-1)
  }
}
