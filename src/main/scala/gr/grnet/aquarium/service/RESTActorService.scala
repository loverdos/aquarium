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

import gr.grnet.aquarium.actor.RESTRole
import _root_.akka.actor._
import cc.spray.can.{ServerConfig, HttpClient, HttpServer}
import gr.grnet.aquarium.util.{Loggable, Lifecycle}
import gr.grnet.aquarium.{Configurable, AquariumAwareSkeleton, Aquarium}
import com.ckkloverdos.props.Props

/**
 * REST service based on Actors and Spray.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class RESTActorService extends Lifecycle with AquariumAwareSkeleton with Configurable with Loggable {
  private[this] var _port: Int = 8080
  private[this] var _restActor: ActorRef = _
  private[this] var _serverActor: ActorRef = _
  private[this] var _clientActor: ActorRef = _


  def propertyPrefix = Some(RESTActorService.Prefix)

  /**
   * Configure this instance with the provided properties.
   *
   * If `propertyPrefix` is defined, then `props` contains only keys that start with the given prefix.
   */
  def configure(props: Props) {
    this._port = props.getIntEx(Aquarium.EnvKeys.restPort.name)
    logger.debug("HTTP port is %s".format(this._port))
  }

  def start(): Unit = {
    logger.info("Starting HTTP on port %s".format(this._port))

    this._restActor = aquarium.actorProvider.actorForRole(RESTRole)
    // Start Spray subsystem
    this._serverActor = Actor.actorOf(new HttpServer(ServerConfig(port = this._port))).start()
    this._clientActor = Actor.actorOf(new HttpClient()).start()
  }

  def stop(): Unit = {
    logger.info("Stopping HTTP on port %s".format(this._port))

    this._serverActor.stop()
    this._clientActor.stop()
  }
}

object RESTActorService {
  final val Prefix = "rest"
}