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

import com.ckkloverdos.props.Props
import akka.actor.ActorRef
import gr.grnet.aquarium.{AquariumAwareSkeleton, Configurable}
import java.util.concurrent.ConcurrentHashMap
import gr.grnet.aquarium.util.Loggable
import gr.grnet.aquarium.actor.message.config.{AquariumPropertiesLoaded, ActorProviderConfigured}
import gr.grnet.aquarium.actor._

/**
 * All actors are provided locally.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class SimpleLocalRoleableActorProviderService
  extends RoleableActorProviderService
     with AquariumAwareSkeleton
     with Configurable
     with Loggable {

  private[this] val actorCache = new ConcurrentHashMap[ActorRole, ActorRef]
  @volatile private[this] var _props: Props = _

  def propertyPrefix = None

  def configure(props: Props): Unit = {
    this._props = props
  }

  def start(): Unit = {
    // Start and configure actors
    import SimpleLocalRoleableActorProviderService.RolesToBeStarted

    for(role <- RolesToBeStarted) {
      actorForRole(role)
    }
  }

  def stop(): Unit = {
  }

  private[this] def _newActor(role: ActorRole): ActorRef = {
    val actorFactory = (_class: Class[_ <: RoleableActor]) ⇒ {
      aquarium.newInstance(_class, _class.getName)
    }

    val actorRef = akka.actor.Actor.actorOf(actorFactory(role.actorType)).start()

    val propsMsg = AquariumPropertiesLoaded(this._props)
    if(role.canHandleConfigurationMessage(propsMsg)) {
      actorRef ! propsMsg
    }

    val providerMsg = ActorProviderConfigured(this)
    if(role.canHandleConfigurationMessage(providerMsg)) {
      actorRef ! providerMsg
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
      _fromCacheOrNew(role)
    } else {
      _newActor(role)
    }
  }

  override def toString = gr.grnet.aquarium.util.shortClassNameOf(this)
}

object SimpleLocalRoleableActorProviderService {
  // Always set Router at the end.
  // We could definitely use some automatic dependency sorting here (topological sorting anyone?)
  final val RolesToBeStarted = List(
    //    ResourceProcessorRole,
    RESTRole,
    PingerRole,
    RouterRole)

  lazy val ActorClassByRole: Map[ActorRole, Class[_ <: RoleableActor]] =
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