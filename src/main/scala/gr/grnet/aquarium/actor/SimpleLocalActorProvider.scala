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

package gr.grnet.aquarium.actor

import com.ckkloverdos.props.Props
import akka.actor.ActorRef
import gr.grnet.aquarium.Configurable
import java.util.concurrent.ConcurrentHashMap
import gr.grnet.aquarium.util.Loggable


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
    logger.info("About to start actors for %s roles: %s".format(RolesToBeStarted.size, RolesToBeStarted))
    for((role, index) <- RolesToBeStarted.zipWithIndex) {
      actorForRole(role)
      logger.info("%s. Started actor for role %s".format(index + 1, role))
    }

    // Now that all actors have been started, send them some initialization code
    val message = ActorProviderConfigured(this)
    for(role <- RolesToBeStarted) {
      role match {
        case DispatcherRole ⇒
          logger.info("Configuring %s with %s".format(DispatcherRole, message))
          actorForRole(DispatcherRole) ! message
        case anyOtherRole ⇒
          logger.info("Configuring %s with %s".format(anyOtherRole, message))
          actorForRole(anyOtherRole) ! message
      }
    }

    logger.info("Started")
  }

  def stop(): Unit = {
    logger.info("Stopped")
  }

  private[this] def _newActor(role: ActorRole): ActorRef = {
    akka.actor.Actor.actorOf(role.actorType).start()
  }

  private[this] def _fromCacheOrNew(role: ActorRole): ActorRef = {
    actorCache.get(role) match {
      case null ⇒
        val actorRef = _newActor(role)
        actorCache.put(role, actorRef)

        if(role.canHandleConfigurationMessage(classOf[AquariumPropertiesLoaded])) {
          actorRef ! AquariumPropertiesLoaded(this._props)
        }

        actorRef
      case actorRef ⇒
        actorRef
    }
  }

  @throws(classOf[Exception])
  def actorForRole(role: ActorRole, hints: Props = Props.empty) = {
    // Currently, all actors are initialized to one instance
    // and user actor are treated specially
    role match {
      case RESTRole ⇒
        _fromCacheOrNew(RESTRole)
      case DispatcherRole ⇒
        _fromCacheOrNew(DispatcherRole)
      case UserActorManagerRole ⇒
        _fromCacheOrNew(UserActorManagerRole)
      case UserActorRole ⇒
        // NOTE: This always creates a new actor and is intended to be called only
        // from internal API that can manage the created actors.
        // E.g. UserActorProvider knows how to manage multiple user actors and properly initialize them.
        //
        // Note that the returned actor is not initialized!
        val actorRef = _newActor(UserActorRole)

        if(role.canHandleConfigurationMessage(classOf[AquariumPropertiesLoaded])) {
          actorRef ! AquariumPropertiesLoaded(this._props)
        }

        if(role.canHandleConfigurationMessage(classOf[ActorProviderConfigured])) {
          actorRef ! ActorProviderConfigured(this)
        }

        actorRef
      case _ ⇒
        throw new Exception("Cannot create actor for role %s".format(role))
    }
  }

  override def toString = gr.grnet.aquarium.util.shortClassNameOf(this)
}

object SimpleLocalActorProvider {
  // Always set Dispatcher at the end.
  final val RolesToBeStarted = List(
//    ResourceProcessorRole,
    RESTRole,
    UserActorManagerRole,
    DispatcherRole)

  lazy val ActorClassByRole: Map[ActorRole, Class[_ <: AquariumActor]] =
    RolesToBeStarted map { role ⇒
      (role, role.actorType)
    } toMap
  
  lazy val ActorRefByRole: Map[ActorRole, ActorRef] =
    ActorClassByRole map { case (role, clazz) ⇒
    (role, akka.actor.Actor.actorOf(clazz).start())
  }
}