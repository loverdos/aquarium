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

package gr.grnet.aquarium.service

import gr.grnet.aquarium.util._
import gr.grnet.aquarium.{ResourceLocator, Aquarium, Configurable, AquariumAwareSkeleton}
import com.ckkloverdos.props.Props
import com.twitter.finagle.{Service, SimpleFilter}
import org.jboss.netty.handler.codec.http.{HttpResponseStatus ⇒ THttpResponseStatus, DefaultHttpResponse ⇒ TDefaultHttpResponse, HttpResponse ⇒ THttpResponse, HttpRequest ⇒ THttpRequest}
import com.twitter.util.{Future ⇒ TFuture, FuturePool ⇒ TFuturePool, Promise ⇒ TPromise, Return ⇒ TReturn, Throw ⇒ TThrow, Duration}
import com.twitter.finagle.builder.ServerBuilder
import com.twitter.finagle.http.Http
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion._
import org.jboss.netty.buffer.ChannelBuffers._
import org.jboss.netty.util.CharsetUtil._
import java.net.InetSocketAddress
import java.util.concurrent.{Executors, TimeUnit}
import gr.grnet.aquarium.util.date.TimeHelpers
import org.joda.time.format.ISODateTimeFormat
import gr.grnet.aquarium.actor.message.{UserActorRequestMessage, GetUserStateRequest, GetUserBalanceRequest, UserActorResponseMessage}
import com.ckkloverdos.resource.StreamResource
import com.ckkloverdos.maybe.{Just, Failed}
import gr.grnet.aquarium.event.model.ExternalEventModel
import akka.util.{Timeout ⇒ ATimeout, Duration ⇒ ADuration}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class FinagleRESTService extends Lifecycle with AquariumAwareSkeleton with Configurable with Loggable {
  final val TEXT_PLAIN       = "text/plain"
  final val APPLICATION_JSON = "application/json"

  @volatile private[this] var _port: Int = 8080
  @volatile private[this] var _shutdownTimeoutMillis: Long = 2000
  @volatile private[this] var _threadPoolSize: Int = 4
  @volatile private[this] var _threadPool: TFuturePool = _

  def propertyPrefix = Some(RESTService.Prefix)

  /**
   * Configure this instance with the provided properties.
   *
   * If `propertyPrefix` is defined, then `props` contains only keys that start with the given prefix.
   */
  def configure(props: Props) {
    this._port = props.getIntEx(Aquarium.EnvKeys.restPort.name)
    this._shutdownTimeoutMillis = props.getLongEx(Aquarium.EnvKeys.restShutdownTimeoutMillis.name)

    this._threadPool = TFuturePool(Executors.newFixedThreadPool(this._threadPoolSize))

    logger.debug("HTTP port is %s".format(this._port))
  }

  def stringResponse(status: THttpResponseStatus, body: String, contentType: String) = {
    val response = new TDefaultHttpResponse(HTTP_1_1, status)
    response.setContent(copiedBuffer(body, UTF_8))
    response.setHeader("Content-type", "%s;charset=utf-8".format(contentType))

    TFuture.value(response)
  }

  def stringResponseOK(body: String, contentType: String): TFuture[THttpResponse] = {
    stringResponse(OK, body, contentType)
  }

  def statusResponse(status: THttpResponseStatus): TFuture[THttpResponse] = {
    stringResponse(status, status.getReasonPhrase, TEXT_PLAIN)
  }

  def resourceInfoResponse(resource: StreamResource, contentType: String): TFuture[THttpResponse] = {
    val fmt = (body: String) ⇒ "%s\n\n%s".format(resource.url, body)

    resource.stringContent.toMaybeEither match {
      case Just(body) ⇒
        stringResponseOK(fmt(body), contentType)

      case Failed(e) ⇒
        throw e
    }
  }

  def eventInfoResponse[E <: ExternalEventModel](
      eventID: String,
      getter: String ⇒ Option[E]
  ): TFuture[THttpResponse] = {
    getter(eventID) match {
      case Some(event) ⇒
        stringResponseOK(event.toJsonString, APPLICATION_JSON)

      case None ⇒
        statusResponse(NOT_FOUND)
    }
  }

  final case class ExceptionHandler() extends SimpleFilter[THttpRequest, THttpResponse] {
    def apply(request: THttpRequest, service: Service[THttpRequest, THttpResponse]): TFuture[THttpResponse] = {
      service(request) handle {
        case error ⇒
          logger.error("While serving %s".format(request), error)
          val statusCode = error match {
            case _: IllegalArgumentException ⇒
              FORBIDDEN
            case _ ⇒
              INTERNAL_SERVER_ERROR
          }

          val errorResponse = new TDefaultHttpResponse(HTTP_1_1, statusCode)
          errorResponse.setContent(copiedBuffer(error.getStackTraceString, UTF_8))

          errorResponse
      }
    }
  }

  final case class AdminChecker() extends SimpleFilter[THttpRequest, THttpResponse] {
    def apply(request: THttpRequest, service: Service[THttpRequest, THttpResponse]): TFuture[THttpResponse] = {
      if(request.getUri.startsWith(RESTPaths.AdminPrefix)) {
        val headerValue = request.getHeader(Aquarium.HTTP.RESTAdminHeaderName)
        aquarium.adminCookie match {
          case Some(`headerValue`) ⇒
            service(request)

          case None ⇒
            statusResponse(FORBIDDEN)
        }
      } else {
        service(request)
      }
    }
  }

  final case class MainService() extends Service[THttpRequest, THttpResponse] {
    final case class UserActorService() extends Service[UserActorRequestMessage, UserActorResponseMessage[_]] {
      def apply(request: UserActorRequestMessage): TFuture[UserActorResponseMessage[_]] = {
        // We want to asynchronously route the message via akka and get the whole computation as a
        // twitter future.
        val actorRef = aquarium.akkaService.getOrCreateUserActor(request.userID)
        val promise = new TPromise[UserActorResponseMessage[_]]()

        val actualWork = akka.pattern.ask(actorRef, request)(ATimeout(ADuration(500, TimeUnit.MILLISECONDS))).
          asInstanceOf[TFuture[UserActorResponseMessage[_]]]

        actualWork.
          onSuccess(promise.setValue).
          onFailure(promise.setException).
          onCancellation(promise.setException(new Exception("Processing of %s has been cancelled".format(request))))

        promise
      }
    }

    final val actorRouterService = UserActorService()

    def callUserActor(requestMessage: UserActorRequestMessage): TFuture[THttpResponse] = {
      actorRouterService(requestMessage).transform { tryResponse ⇒
        tryResponse match {
          case TReturn(responseMessage: UserActorResponseMessage[_]) ⇒
            val statusCode = responseMessage.suggestedHTTPStatus
            val status = THttpResponseStatus.valueOf(statusCode)

            responseMessage.response match {
              case Left(errorMessage) ⇒
                logger.error("Error %s '%s' serving %s. Internal response: %s".format(
                  statusCode,
                  errorMessage,
                  requestMessage,
                  responseMessage))

                stringResponse(status, errorMessage, TEXT_PLAIN)

              case Right(_) ⇒
                stringResponse(status, responseMessage.toJsonString, APPLICATION_JSON)
            }

          case TThrow(throwable) ⇒
            val status = INTERNAL_SERVER_ERROR
            logger.error("Error %s serving %s: %s".format(
              status.getReasonPhrase,
              requestMessage,
              gr.grnet.aquarium.util.shortInfoOf(throwable)
            ))

            statusResponse(status)
        }
      }
    }

    def apply(request: THttpRequest): TFuture[THttpResponse] = {
      val millis = TimeHelpers.nowMillis()
      val uri = request.getUri
      val method = request.getMethod
      logger.debug("%s %s %s".format(method, request.getProtocolVersion, uri))

      uri match {
        case RESTPaths.PingPath() ⇒
          val now = TimeHelpers.nowMillis()
          val nowFormatted = ISODateTimeFormat.dateTime().print(now)
          stringResponseOK("PONG\n%s\n%s".format(now, nowFormatted), TEXT_PLAIN)

//        case RESTPaths.ResourcesPath() ⇒
//          stringResponseOK("%s\n%s\n%s\n" .format(
//            ResourceLocator.ResourceNames.AQUARIUM_PROPERTIES,
//            ResourceLocator.ResourceNames.LOGBACK_XML,
//            ResourceLocator.ResourceNames.POLICY_YAML),
//          TEXT_PLAIN)

        case RESTPaths.UserBalancePath(userID) ⇒
          // /user/(.+)/balance/?
          callUserActor(GetUserBalanceRequest(userID, millis))

        case RESTPaths.UserStatePath(userId) ⇒
          // /user/(.+)/state/?
          callUserActor(GetUserStateRequest(userId, millis))

        case RESTPaths.ResourcesAquariumPropertiesPath() ⇒
          resourceInfoResponse(ResourceLocator.Resources.AquariumPropertiesResource, TEXT_PLAIN)

        case RESTPaths.ResourcesLogbackXMLPath() ⇒
          resourceInfoResponse(ResourceLocator.Resources.LogbackXMLResource, TEXT_PLAIN)

        case RESTPaths.ResourcesPolicyYAMLPath() ⇒
          resourceInfoResponse(ResourceLocator.Resources.PolicyYAMLResource, TEXT_PLAIN)

        case RESTPaths.ResourceEventPath(id) ⇒
          eventInfoResponse(id, aquarium.resourceEventStore.findResourceEventByID)

        case RESTPaths.IMEventPath(id) ⇒
          eventInfoResponse(id, aquarium.imEventStore.findIMEventByID)

        case _ ⇒
          statusResponse(NOT_FOUND)
      }
    }
  }

  val service = ExceptionHandler() andThen AdminChecker() andThen MainService()
  lazy val server = ServerBuilder().
    codec(Http()).
    bindTo(new InetSocketAddress(this._port)).
    name("HttpServer").
    build(service)

  def start(): Unit = {
    logger.info("Starting HTTP on port %s".format(this._port))
    // Just for the side effect
    assert(server ne null)
  }

  def stop(): Unit = {
    logger.info("Stopping HTTP on port %s, waiting for at most %s ms".format(this._port, this._shutdownTimeoutMillis))
    server.close(Duration(this._shutdownTimeoutMillis, TimeUnit.MILLISECONDS))
  }
}
