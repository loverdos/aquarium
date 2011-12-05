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

import org.junit.Test
import cc.spray.can.HttpClient._
import akka.actor.{PoisonPill, Actor}
import cc.spray.can.{HttpClient, HttpResponse, HttpRequest, HttpServer}

import cc.spray.can.HttpMethods.GET
import gr.grnet.aquarium.util.Loggable
import org.junit.Assume._
import gr.grnet.aquarium.LogicTestsAssumptions

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class PingSprayServiceTest extends Loggable {
  def show(response: HttpResponse) {
      println(
        """|Result from host:
           |status : {}
           |headers: {}
           |body   : {}""".stripMargin,
        Array(response.status: java.lang.Integer, response.headers, response.bodyAsString)
      )
    }
  @Test
  def testPing: Unit = {
    assumeTrue(LogicTestsAssumptions.EnableSprayTests)
    
    val server = Actor.actorOf(new HttpServer()).start()
    val client = Actor.actorOf(new HttpClient()).start()

    val dialog = HttpDialog("localhost", 8080)
    val result = dialog.send(HttpRequest(method = GET, uri = "/ping")).end
    result onComplete { future =>
      future.value match {
        case Some(Right(response)) =>
          show(response)
        case other =>
          logger.error("Error: %s".format(other))
      }

      server ! PoisonPill
      client ! PoisonPill
    }

    Thread.sleep(10000)
  }
}