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
package provider

import com.ckkloverdos.props.Props
import akka.actor.ActorRef
import gr.grnet.aquarium.Configurable
import java.util.concurrent.ConcurrentHashMap
import gr.grnet.aquarium.util.Loggable
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.actor.ActorRole
import gr.grnet.aquarium.actor.message.config.{AquariumPropertiesLoaded, ActorProviderConfigured}


/**
 * All actors are provided locally.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class SimpleLocalActorProvider extends ActorProvider with Configurable with Loggable {
  private[this] val actorCache = new ConcurrentHashMap[ActorRole, ActorRef]
  private[this] var _props: Props = _

  def configure(props: Props): Unit = {
    this._props = props
    logger.info("Configured with props: %s".format(props))
  }

  def start(): Unit = {
    // Start all actors that need to [be started]
    val RolesToBeStarted = SimpleLocalActorProvider.RolesToBeStarted
    val Size = RolesToBeStarted.size
    logger.info("About to start actors for %s roles: %s".format(Size, RolesToBeStarted))
    for((role, index) <- RolesToBeStarted.zipWithIndex) {
      logger.debug("%s/%s (-). Starting actor for role %s".format(index + 1, Size, role))
      val ms0 = TimeHelpers.nowMillis
      actorForRole(role)
      val ms1 = TimeHelpers.nowMillis
      logger.info("%s/%s (+). Started actor for role %s in %.3f sec".format(index + 1, Size, role, (ms1 - ms0).toDouble / 1000.0))
    }

    // Now that all actors have been started, send them some initialization code
    val message = ActorProviderConfigured(this)
    for(role <- RolesToBeStarted) {
      if(role.canHandleConfigurationMessage(classOf[ActorProviderConfigured])) {
        logger.debug("Configuring %s with %s".format(role, message))
        actorForRole(role) ! message
      }
    }

    logger.info("Started")
  }

  def stop(): Unit = {
    logger.info("Stopped")
  }

  private[this] def _newActor(role: ActorRole): ActorRef = {
    val actorRef = akka.actor.Actor.actorOf(role.actorType).start()

    if(role.canHandleConfigurationMessage(classOf[AquariumPropertiesLoaded])) {
      actorRef ! AquariumPropertiesLoaded(this._props)
    }
    if(role.canHandleConfigurationMessage(classOf[ActorProviderConfigured])) {
      actorRef ! ActorProviderConfigured(this)
    }

    actorRef
  }

  private[this] def _fromCacheOrNew(role: ActorRole): ActorRef = synchronized {
    actorCache.get(role) match {
      case null ⇒
        val actorRef = _newActor(role)
        actorCache.put(role, actorRef)
        actorRef
      case actorRef ⇒
        actorRef
    }
  }

  @throws(classOf[Exception])
  def actorForRole(role: ActorRole, hints: Props = Props.empty) = synchronized {
    if(role.isCacheable) {
      logger.debug("%s is cacheable".format(role.role))
      _fromCacheOrNew(role)
    } else {
      logger.debug("%s is not cacheable".format(role.role))
      _newActor(role)
    }
  }

  override def toString = gr.grnet.aquarium.util.shortClassNameOf(this)
}

object SimpleLocalActorProvider {
  // Always set Dispatcher at the end.
  // We could definitely use some automatic dependency sorting here (topological sorting anyone?)
  final val RolesToBeStarted = List(
    //    ResourceProcessorRole,
    RESTRole,
    UserActorManagerRole,
    PingerRole,
    DispatcherRole)

  lazy val ActorClassByRole: Map[ActorRole, Class[_ <: AquariumActor]] =
    RolesToBeStarted map {
      role ⇒
        (role, role.actorType)
    } toMap

  lazy val ActorRefByRole: Map[ActorRole, ActorRef] =
    ActorClassByRole map {
      case (role, clazz) ⇒
        (role, akka.actor.Actor.actorOf(clazz).start())
    }
}