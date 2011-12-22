package gr.grnet.aquarium.rest.actor

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

import cc.spray.can.HttpMethods.{GET, POST}
import cc.spray.can._
import gr.grnet.aquarium.util.Loggable
import net.liftweb.json.JsonAST.JValue
import net.liftweb.json.{JsonAST, Printer}
import gr.grnet.aquarium.MasterConf
import akka.actor.{ActorRef, Actor}
import gr.grnet.aquarium.actor.{RESTRole, AquariumActor, DispatcherRole}
import RESTPaths.{UserBalance}
import gr.grnet.aquarium.processor.actor.{UserRequestGetBalance, DispatcherMessage}

/**
 * Spray-based REST service. This is the outer-world's interface to Aquarium functionality.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class RESTActor(_id: String) extends AquariumActor with Loggable {
  def this() = this("spray-root-service")

  self.id = _id

  private def jsonResponse200(body: JValue, pretty: Boolean = false): HttpResponse = {
    val stringBody = Printer.pretty(JsonAST.render(body))
    stringResponse200(stringBody, "application/json")
  }
  
  private def stringResponse(status: Int, stringBody: String, contentType: String = "application/json"): HttpResponse = {
    HttpResponse(
      status,
      HttpHeader("Content-type", "%s;charset=utf-8".format(contentType)) :: Nil,
      stringBody.getBytes("UTF-8")
    )
  }

  private def stringResponse200(stringBody: String, contentType: String = "application/json"): HttpResponse = {
    stringResponse(200, stringBody, contentType)
  }

  protected def receive = {
    case RequestContext(HttpRequest(GET, "/ping", _, _, _), _, responder) ⇒
      responder.complete(stringResponse200("{\"pong\": %s}".format(System.currentTimeMillis())))

    case RequestContext(HttpRequest(GET, "/stats", _, _, _), _, responder) ⇒ {
      (serverActor ? GetStats).mapTo[Stats].onComplete { future =>
          future.value.get match {
            case Right(stats) => responder.complete {
              stringResponse200 (
                "Uptime              : " + (stats.uptime / 1000.0) + " sec\n" +
                  "Requests dispatched : " + stats.requestsDispatched + '\n' +
                  "Requests timed out  : " + stats.requestsTimedOut + '\n' +
                  "Requests open       : " + stats.requestsOpen + '\n' +
                  "Open connections    : " + stats.connectionsOpen + '\n'
              )
            }
            case Left(ex) => responder.complete(stringResponse(500, "Couldn't get server stats due to " + ex, "text/plain"))
          }
      }
    }

    case RequestContext(HttpRequest(GET, uri, headers, body, protocol), _, responder) ⇒
      //+ Main business logic REST URIs are matched here
      uri match {
        case UserBalance(userId) ⇒
          callDispatcher(UserRequestGetBalance(userId, System.currentTimeMillis()), responder)
        case _ ⇒
          responder.complete(stringResponse(404, "Unknown resource!", "text/plain"))
      }
      //- Main business logic REST URIs are matched here

    case RequestContext(HttpRequest(_, _, _, _, _), _, responder) ⇒
      responder.complete(stringResponse(404, "Unknown resource!", "text/plain"))

    case Timeout(method, uri, _, _, _, complete) ⇒ complete {
      HttpResponse(status = 500).withBody("The " + method + " request to '" + uri + "' has timed out...")
    }
  }


  def callDispatcher(message: DispatcherMessage, responder: RequestResponder): Unit = {
    val masterConf = MasterConf.MasterConf
    val actorProvider = masterConf.actorProvider
    val dispatcher = actorProvider.actorForRole(DispatcherRole)
    val futureResponse = dispatcher ask message

    futureResponse onComplete { future ⇒
        future.value match {
          case None ⇒
          // TODO: Will this ever happen??
          case Some(Left(error)) ⇒
            logger.error("Error serving %s: %s".format(message, error))
            responder.complete(stringResponse(500, "Internal Server Error", "text/plain"))
          case Some(Right(actualResponse)) ⇒
            actualResponse match {
              case dispatcherResponse: DispatcherMessage if(!dispatcherResponse.isError) ⇒
                responder.complete(HttpResponse(status = 200, body = dispatcherResponse.toJson.getBytes("UTF-8"), headers = HttpHeader("Content-type", "application/json;charset=utf-8") :: Nil))
              case dispatcherResponse: DispatcherMessage ⇒
                logger.error("Error serving %s: Dispatcher response is: %s".format(message, actualResponse))
                responder.complete(stringResponse(500, "Internal Server Error", "text/plain"))
              case _ ⇒
                logger.error("Error serving %s: Dispatcher response is: %s".format(message, actualResponse))
                responder.complete(stringResponse(500, "Internal Server Error", "text/plain"))
            }
        }
    }
  }
  ////////////// helpers //////////////

  val defaultHeaders = List(HttpHeader("Content-Type", "text/plain"))

  lazy val serverActor = Actor.registry.actorsFor("spray-can-server").head

  def role = RESTRole
}