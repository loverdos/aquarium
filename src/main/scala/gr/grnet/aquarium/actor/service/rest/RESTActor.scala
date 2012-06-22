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

package gr.grnet.aquarium.actor
package service
package rest

import cc.spray.can.HttpMethods.GET
import cc.spray.can._
import gr.grnet.aquarium.util.Loggable
import gr.grnet.aquarium.util.shortInfoOf
import akka.actor.Actor
import gr.grnet.aquarium.actor.{RESTRole, RoleableActor, RouterRole}
import RESTPaths._
import gr.grnet.aquarium.util.date.TimeHelpers
import org.joda.time.format.ISODateTimeFormat
import gr.grnet.aquarium.actor.message.{RouterResponseMessage, GetUserStateRequest, RouterRequestMessage, GetUserBalanceRequest}
import gr.grnet.aquarium.{ResourceLocator, Aquarium}
import com.ckkloverdos.resource.StreamResource
import com.ckkloverdos.maybe.Failed
import java.net.InetAddress
import gr.grnet.aquarium.event.model.ExternalEventModel

/**
 * Spray-based REST service. This is the outer-world's interface to Aquarium functionality.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class RESTActor private(_id: String) extends RoleableActor with Loggable {
  def this() = this("spray-root-service")

  self.id = _id

  final val TEXT_PLAIN       = "text/plain"
  final val APPLICATION_JSON = "application/json"

  private def stringResponse(status: Int, stringBody: String, contentType: String): HttpResponse = {
    HttpResponse(
      status,
      HttpHeader("Content-type", "%s;charset=utf-8".format(contentType)) :: Nil,
      stringBody.getBytes("UTF-8")
    )
  }

  private def resourceInfoResponse(
      uri: String,
      responder: RequestResponder,
      resource: StreamResource,
      contentType: String
  ): Unit = {

    val fmt = (body: String) ⇒ "%s\n\n%s".format(resource.url, body)
    val res = resource.mapString(body ⇒ responder.complete(stringResponse(200, fmt(body), contentType)))

    res match {
      case Failed(e) ⇒
        logger.error("While serving %s".format(uri), e)
        responder.complete(stringResponse(501, "Internal Server Error: %s".format(shortInfoOf(e)), TEXT_PLAIN))

      case _ ⇒

    }
  }

  private def eventInfoResponse[E <: ExternalEventModel](
      uri: String,
      responder: RequestResponder,
      getter: String ⇒ Option[E],
      eventID: String
  ): Unit = {

    val toSend = getter.apply(eventID) match {
     case Some(event) ⇒
       (200, event.toJsonString, APPLICATION_JSON)

     case None ⇒
       (404, "Event not found", TEXT_PLAIN)
   }

   responder.complete(stringResponse(toSend._1, toSend._2, toSend._3))
  }

  def withAdminCookie(
      uri: String,
      responder: RequestResponder,
      headers: List[HttpHeader],
      remoteAddress: InetAddress
  )(  f: RequestResponder ⇒ Unit): Unit = {

    aquarium.adminCookie match {
      case Some(adminCookie) ⇒
        headers.find(_.name.toLowerCase == Aquarium.HTTP.RESTAdminHeaderNameLowerCase) match {
          case Some(cookieHeader) if(cookieHeader.value == adminCookie) ⇒
            try f(responder)
            catch {
              case e: Throwable ⇒
                logger.error("While serving %s".format(uri), e)
                responder.complete(stringResponse(501, "Internal Server Error: %s".format(shortInfoOf(e)), TEXT_PLAIN))
            }

          case Some(cookieHeader) ⇒
            logger.warn("Admin request %s with bad cookie '%s' from %s".format(uri, cookieHeader.value, remoteAddress))
            responder.complete(stringResponse(401, "Unauthorized!", TEXT_PLAIN))

          case None ⇒
            logger.warn("Admin request %s with no cookie from %s".format(uri, remoteAddress))
            responder.complete(stringResponse(401, "Unauthorized!", TEXT_PLAIN))
        }

      case None ⇒
        responder.complete(stringResponse(403, "Forbidden!", TEXT_PLAIN))
    }
  }

  private def stringResponse200(stringBody: String, contentType: String): HttpResponse = {
    stringResponse(200, stringBody, contentType)
  }

  protected def receive = {
    case RequestContext(HttpRequest(GET, "/ping", _, _, _), _, responder) ⇒
      val now = TimeHelpers.nowMillis()
      val nowFormatted = ISODateTimeFormat.dateTime().print(now)
      responder.complete(stringResponse200("PONG\n%s\n%s".format(now, nowFormatted), TEXT_PLAIN))

    case RequestContext(HttpRequest(GET, "/stats", _, _, _), _, responder) ⇒ {
      (serverActor ? GetStats).mapTo[Stats].onComplete {
        future =>
          future.value.get match {
            case Right(stats) => responder.complete {
              stringResponse200(
                "Uptime              : " + (stats.uptime / 1000.0) + " sec\n" +
                "Requests dispatched : " + stats.requestsDispatched + '\n' +
                "Requests timed out  : " + stats.requestsTimedOut + '\n' +
                "Requests open       : " + stats.requestsOpen + '\n' +
                "Open connections    : " + stats.connectionsOpen + '\n',
                TEXT_PLAIN
              )
            }
            case Left(ex) => responder.complete(stringResponse(500, "Couldn't get server stats due to " + ex, TEXT_PLAIN))
          }
      }
    }

    case RequestContext(HttpRequest(GET, uri, headers, body, protocol), remoteAddress, responder) ⇒
      def withAdminCookieHelper(f: RequestResponder ⇒ Unit): Unit = {
        withAdminCookie(uri, responder, headers, remoteAddress)(f)
      }

      //+ Main business logic REST URIs are matched here
      val millis = TimeHelpers.nowMillis()
      uri match {
        case UserBalancePath(userID) ⇒
          // /user/(.+)/balance/?
          callRouter(GetUserBalanceRequest(userID, millis), responder)

        case UserStatePath(userId) ⇒
          // /user/(.+)/state/?
          callRouter(GetUserStateRequest(userId, millis), responder)

//        case AdminPingAllPath() ⇒
//          withAdminCookieHelper { responder ⇒
//            callRouter(PingAllRequest(), responder)
//          }

        case ResourcesPath() ⇒
          withAdminCookieHelper { responder ⇒
            responder.complete(
              stringResponse200("%s\n%s\n%s\n" .format(
                  ResourceLocator.ResourceNames.AQUARIUM_PROPERTIES,
                  ResourceLocator.ResourceNames.LOGBACK_XML,
                  ResourceLocator.ResourceNames.POLICY_YAML),
                TEXT_PLAIN
              )
            )
          }

        case ResourcesAquariumPropertiesPath() ⇒
          withAdminCookieHelper { responder ⇒
            resourceInfoResponse(uri, responder, ResourceLocator.Resources.AquariumPropertiesResource, TEXT_PLAIN)
          }

        case ResourcesLogbackXMLPath() ⇒
          withAdminCookieHelper { responder ⇒
            resourceInfoResponse(uri, responder, ResourceLocator.Resources.LogbackXMLResource, TEXT_PLAIN)
          }

        case ResourcesPolicyYAMLPath() ⇒
          withAdminCookieHelper { responder ⇒
            resourceInfoResponse(uri, responder, ResourceLocator.Resources.PolicyYAMLResource, TEXT_PLAIN)
          }

        case ResourceEventPath(id) ⇒
          withAdminCookieHelper { responder ⇒
            eventInfoResponse(uri, responder, aquarium.resourceEventStore.findResourceEventByID, id)
          }

        case IMEventPath(id) ⇒
          withAdminCookieHelper { responder ⇒
            eventInfoResponse(uri, responder, aquarium.imEventStore.findIMEventById, id)
          }

        case _ ⇒
          responder.complete(stringResponse(404, "Unknown resource!", TEXT_PLAIN))
      }
    //- Main business logic REST URIs are matched here

    case RequestContext(HttpRequest(_, _, _, _, _), _, responder) ⇒
      responder.complete(stringResponse(404, "Unknown resource!", TEXT_PLAIN))

    case Timeout(method, uri, _, _, _, complete) ⇒ complete {
      HttpResponse(status = 500).withBody("The " + method + " request to '" + uri + "' has timed out...")
    }
  }

  private[this]
  def callRouter(message: RouterRequestMessage, responder: RequestResponder): Unit = {
    val actorProvider = aquarium.actorProvider
    val router = actorProvider.actorForRole(RouterRole)
    val futureResponse = router ask message

    futureResponse onComplete {
      future ⇒
        future.value match {
          case None ⇒
            // TODO: Will this ever happen??
            logger.warn("Future did not complete for %s".format(message))
            val statusCode = 500
            responder.complete(stringResponse(statusCode, "Internal Server Error", TEXT_PLAIN))

          case Some(Left(error)) ⇒
            val statusCode = 500
            logger.error("Error %s serving %s: %s".format(statusCode, message, error))
            responder.complete(stringResponse(statusCode, "Internal Server Error", TEXT_PLAIN))

          case Some(Right(actualResponse)) ⇒
            actualResponse match {
              case routerResponse: RouterResponseMessage[_] ⇒
                routerResponse.response match {
                  case Left(errorMessage) ⇒
                    val statusCode = routerResponse.suggestedHTTPStatus

                    logger.error("Error %s '%s' serving %s. Internal response: %s".format(
                      statusCode,
                      errorMessage,
                      message,
                      actualResponse))

                    responder.complete(stringResponse(statusCode, errorMessage, TEXT_PLAIN))

                  case Right(response) ⇒
                    responder.complete(
                      HttpResponse(
                        routerResponse.suggestedHTTPStatus,
                        body = routerResponse.responseToJsonString.getBytes("UTF-8"),
                        headers = HttpHeader("Content-type", APPLICATION_JSON+";charset=utf-8") ::
                          Nil))
                }

              case _ ⇒
                val statusCode = 500
                logger.error("Error %s serving %s: Response is: %s".format(statusCode, message, actualResponse))
                responder.complete(stringResponse(statusCode, "Internal Server Error", TEXT_PLAIN))
            }
        }
    }
  }

  ////////////// helpers //////////////

  final val defaultHeaders = List(HttpHeader("Content-Type", TEXT_PLAIN))

  lazy val serverActor = Actor.registry.actorsFor("spray-can-server").head

  def role = RESTRole
}