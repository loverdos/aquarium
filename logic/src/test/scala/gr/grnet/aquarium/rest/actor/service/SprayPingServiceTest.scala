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

import cc.spray.can.HttpMethods.GET
import gr.grnet.aquarium.util.Loggable
import org.junit.Assume._
import gr.grnet.aquarium.LogicTestsAssumptions
import cc.spray.can._
import akka.actor.{Actor, PoisonPill}
import org.slf4j.LoggerFactory

/**
 * This class is heavily based on the Spray samples.
 * Copyright is what copyright Spray has.
 */
class SprayPingService(_id: String = "spray-root-service") extends Actor {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  self.id = _id

  protected def receive = {
    case RequestContext(HttpRequest(GET, "/", _, _, _), _, responder) =>
      responder.complete(index)

    case RequestContext(HttpRequest(GET, "/ping", _, _, _), _, responder) =>
      responder.complete(response("PONG!"))

    case RequestContext(HttpRequest(GET, "/stats", _, _, _), _, responder) => {
      (serverActor ? GetStats).mapTo[Stats].onComplete {
        future =>
          future.value.get match {
            case Right(stats) => responder.complete {
              response {
                "Uptime              : " + (stats.uptime / 1000.0) + " sec\n" +
                  "Requests dispatched : " + stats.requestsDispatched + '\n' +
                  "Requests timed out  : " + stats.requestsTimedOut + '\n' +
                  "Requests open       : " + stats.requestsOpen + '\n' +
                  "Open connections    : " + stats.connectionsOpen + '\n'
              }
            }
            case Left(ex) => responder.complete(response("Couldn't get server stats due to " + ex, status = 500))
          }
      }
    }

    case RequestContext(HttpRequest(_, _, _, _, _), _, responder) =>
      responder.complete(response("Unknown resource!", 404))

    case Timeout(method, uri, _, _, _, complete) => complete {
      HttpResponse(status = 500).withBody("The " + method + " request to '" + uri + "' has timed out...")
    }
  }

  ////////////// helpers //////////////

  val defaultHeaders = List(HttpHeader("Content-Type", "text/plain"))

  lazy val serverActor = Actor.registry.actorsFor("spray-can-server").head

  def response(msg: String, status: Int = 200) = HttpResponse(status, defaultHeaders, msg.getBytes("ISO-8859-1"))

  lazy val index = HttpResponse(
    headers = List(HttpHeader("Content-Type", "text/html")),
    body =
      <html>
        <body>
          <h1>Say hello to
            <i>spray-can</i>
            !</h1>
          <p>Defined resources:</p>
          <ul>
            <li>
              <a href="/ping">/ping</a>
            </li>
            <li>
              <a href="/stats">/stats</a>
            </li>
          </ul>
        </body>
      </html>.toString.getBytes("UTF-8")
  )
}

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class SprayPingServiceTest extends Loggable {
  @Test
  def testPing: Unit = {
    assumeTrue(LogicTestsAssumptions.EnableSprayTests)

    val service = Actor.actorOf(new SprayPingService("spray-root-service")).start()
    val server = Actor.actorOf(new HttpServer()).start()
    val client = Actor.actorOf(new HttpClient()).start()

    val dialog = HttpDialog("localhost", 8080)
    val result = dialog.send(HttpRequest(method = GET, uri = "/ping", headers = HttpHeader("Content-Type", "text/plain; charset=UTF-8")::Nil)).end
    result onComplete { future =>
      future.value match {
        case Some(Right(response)) =>
          logger.info("Response class  : %s".format(response.getClass))
          logger.info("Response status : %s".format(response.status))
          logger.info("Response headers: %s".format(response.headers.map(hh => (hh.name, hh.value)).mkString(", ")))
          logger.info("Response body   : %s".format(response.bodyAsString))
        case other =>
          logger.error("Error: %s".format(other))
      }

      service ! PoisonPill
      client  ! PoisonPill
    }

    Thread sleep 100
  }
}