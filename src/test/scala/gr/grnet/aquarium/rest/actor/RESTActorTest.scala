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

package gr.grnet.aquarium.rest.actor

import org.junit.Test
import org.junit.Assert._
import org.junit.Assume.assumeTrue

import gr.grnet.aquarium.actor.RESTRole
import cc.spray.can.HttpMethods.{GET, POST}
import cc.spray.can.HttpClient._
import cc.spray.can.HttpClient.{HttpDialog ⇒ SprayHttpDialog}
import cc.spray.can.{HttpResponse, HttpHeader, HttpRequest, HttpServer ⇒ SprayHttpServer, HttpClient ⇒ SprayHttpClient}
import akka.actor.{PoisonPill, Actor}
import gr.grnet.aquarium.logic.events.ResourceEvent
import net.liftweb.json.JsonAST.JInt
import gr.grnet.aquarium.util.json.JsonHelpers
import gr.grnet.aquarium.{LogicTestsAssumptions, Configurator}

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class RESTActorTest {
  @Test
  def testPing: Unit = {
    assumeTrue(LogicTestsAssumptions.EnableSprayTests)
    
    // Initialize configuration subsystem
    val mc = Configurator.MasterConfigurator
    mc.startServices()
    val port = mc.props.getInt(Configurator.Keys.rest_port).getOr(8080)
    val dialog = SprayHttpDialog("localhost", port)

    val pingReq = HttpRequest(method = GET, uri = "/ping", headers = HttpHeader("Content-Type", "text/plain; charset=UTF-8")::Nil)
    dialog.send(pingReq).end onComplete { futureResp ⇒
      futureResp.value match {
        case Some(Right(HttpResponse(status, _, bytesBody, _))) ⇒
          assertTrue("Status 200 OK", status == 200)
          val stringBody = new String(bytesBody, "UTF-8")
          println("!! Got stringBody = %s".format(stringBody))
          // Note that the response is in JSON format, so must parse it
          implicit val formats = JsonHelpers.DefaultJsonFormats
          val jValue = net.liftweb.json.parse(stringBody)
          println("!! ==> jValue = %s".format(jValue))
          val pongValue = jValue \ "pong"
          println("!! ==> pongValue = %s".format(pongValue))
          assertTrue("pong Int in response", pongValue.isInstanceOf[JInt])
        case Some(Left(error)) ⇒
          fail("Got error: %s".format(error.getMessage))
        case None ⇒
          fail("Got nothing")
      }
    }

    mc.stopServicesWithDelay(1000)
  }
}