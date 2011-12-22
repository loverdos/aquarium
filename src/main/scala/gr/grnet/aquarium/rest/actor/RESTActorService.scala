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

package gr.grnet.aquarium.rest
package actor

import gr.grnet.aquarium.MasterConf
import gr.grnet.aquarium.actor.RESTRole
import _root_.akka.actor._
import cc.spray.can.{ClientConfig, HttpClient, ServerConfig, HttpServer}
import gr.grnet.aquarium.util.Lifecycle

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class RESTActorService extends Lifecycle {
  private[this] var _port: Int = 8080
  private[this] var _restActor: ActorRef = _
  private[this] var _serverActor: ActorRef = _
  private[this] var _clientActor: ActorRef = _

  def start(): Unit = {
    val mc = MasterConf.MasterConf
    this._port = mc.props.getInt(MasterConf.Keys.rest_port).getOr(this._port)
    this._restActor = mc.actorProvider.actorForRole(RESTRole)
    // Start Spray subsystem
    val serverConfig = ServerConfig(port = _port)
    val clientConfig = ClientConfig()
    this._serverActor = Actor.actorOf(new HttpServer(serverConfig)).start()
    this._clientActor = Actor.actorOf(new HttpClient(clientConfig)).start()
  }

  def stop(): Unit = {
    this._serverActor ! PoisonPill
    this._clientActor ! PoisonPill
  }
}