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

import akka.actor.{Actor, Props, ActorRef, ActorSystem}
import gr.grnet.aquarium.util.{Loggable, Lifecycle, shortClassNameOf}
import gr.grnet.aquarium.ResourceLocator.SysEnvs
import gr.grnet.aquarium.{AquariumAwareSkeleton, Configurable, AquariumException, AquariumInternalError}
import com.typesafe.config.ConfigFactory
import java.util.concurrent.atomic.AtomicBoolean
import com.google.common.cache.{CacheStats, RemovalNotification, RemovalListener, CacheBuilder, Cache}
import com.ckkloverdos.props.{Props ⇒ KKProps}
import gr.grnet.aquarium.actor.service.user.UserActor
import gr.grnet.aquarium.service.event.AquariumCreatedEvent
import gr.grnet.aquarium.actor.message.config.InitializeUserActorState
import gr.grnet.aquarium.util.date.TimeHelpers
import java.util.concurrent.{TimeUnit, ConcurrentHashMap, Callable}
import akka.dispatch.{Await, Future}
import akka.util.Duration

/**
 * A wrapper around Akka, so that it is uniformly treated as an Aquarium service.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

final class AkkaService extends AquariumAwareSkeleton with Configurable with Lifecycle with Loggable {
  @volatile private[this] var _actorSystem: ActorSystem = _
  @volatile private[this] var _userActorCache: Cache[String, ActorRef] = _
  @volatile private[this] var _cacheEvictionListener: RemovalListener[String, ActorRef] = _
  @volatile private[this] var _cacheMaximumSize: Int = _
  @volatile private[this] var _cacheInitialCapacity: Int = _
  @volatile private[this] var _cacheConcurrencyLevel: Int = _

  private[this] val stoppingUserActors = new ConcurrentHashMap[String, Future[Boolean]]

  private[this] val isShuttingDown = new AtomicBoolean(false)

  def propertyPrefix: Option[String] = Some("actors")

  /**
   * Configure this instance with the provided properties.
   *
   * If `propertyPrefix` is defined, then `props` contains only keys that start with the given prefix.
   */
  def configure(props: KKProps): Unit = {
    this._cacheMaximumSize = 1000
    this._cacheInitialCapacity = 2 * this._cacheMaximumSize / 3
    this._cacheConcurrencyLevel = Runtime.getRuntime.availableProcessors() * 2
  }

  def actorSystem = {
    if(this._actorSystem eq null) {
      throw new AquariumInternalError("Akka actorSystem is null")
    }

    if(this.isShuttingDown.get()) {
      throw new AquariumException("%s is shutting down".format(shortClassNameOf(this)))
    }

    this._actorSystem
  }

  def foreachCachedUserID[A](f: String ⇒ A): Unit = {
    val keys = this._userActorCache.asMap().keySet().iterator()
    while(keys.hasNext) { f(keys.next()) }
  }

  def cacheStats: CacheStats = {
    this._userActorCache.stats()
  }

  def cacheSize: Long = {
    this._userActorCache.size()
  }

  def start() = {
    // We have AKKA builtin, so no need to mess with pre-existing installation.
    if(SysEnvs.AKKA_HOME.value.isJust) {
      val error = new AquariumInternalError("%s is set. Please unset and restart Aquarium".format(SysEnvs.Names.AKKA_HOME))
      logger.error("%s is set".format(SysEnvs.Names.AKKA_HOME), error)
      throw error
    }

    this._cacheEvictionListener = new RemovalListener[String, ActorRef] {
      def onRemoval(rn: RemovalNotification[String, ActorRef]): Unit = {
        if(isShuttingDown.get()) {
          return
        }

        val userID   = rn.getKey
        val actorRef = rn.getValue
        val cause    = rn.getCause

        if(rn.wasEvicted()) {
          logger.debug("Evicted UserActor %s due to %s".format(userID, cause))
          gracefullyStopUserActor(userID, actorRef)
        } else {
          logger.debug("UserActor %s cache notification for %s".format(userID, cause))
        }
      }
    }

    this._userActorCache = CacheBuilder.
      newBuilder().
        recordStats().
        maximumSize(this._cacheMaximumSize).
        initialCapacity(this._cacheInitialCapacity).
        concurrencyLevel(this._cacheConcurrencyLevel).
        removalListener(this._cacheEvictionListener).
      build()

    this._actorSystem = ActorSystem("aquarium-akka", ConfigFactory.load("akka.conf"))
  }

  def stop() = {
    this.isShuttingDown.set(true)

    this.stoppingUserActors.clear()

    logger.info("UserActor cache stats: {}", this._userActorCache.stats())
    this._userActorCache.invalidateAll
    this._userActorCache.cleanUp()

    this._actorSystem.shutdown()
  }

  def notifyUserActorPostStop(userActor: UserActor): Unit = {
    logger.debug("Removing UserActor %s from stopping set (after postStop())".format(userActor.unsafeUserID))
    this.stoppingUserActors.remove(userActor.unsafeUserID)
  }

  private[this] def gracefullyStopUserActor(userID: String, actorRef: ActorRef): Unit = {
    logger.debug("Gracefully stopping UserActor %s (and inserting into stopping set)".format(userID))
    this.stoppingUserActors.put(
      userID,
      akka.pattern.gracefulStop(actorRef, Duration(1000, TimeUnit.MILLISECONDS))(this._actorSystem)
    )
  }

  def invalidateUserActor(userActor: UserActor): Unit = {
    if(this.isShuttingDown.get()) {
      return
    }

    val userID = userActor.unsafeUserID
    val actorRef = userActor.self

    this._userActorCache.invalidate(userID)
    gracefullyStopUserActor(userID, actorRef)
  }

  def createNamedActor[T <: Actor:ClassManifest](name:String) : ActorRef=
     if(this._actorSystem==null) null
     else this.actorSystem.actorOf(Props[T],name)

  def getOrCreateUserActor(userID: String): ActorRef = {
    if(this.isShuttingDown.get()) {
      throw new AquariumException(
        "%s is shutting down. Cannot provide user actor %s".format(
          shortClassNameOf(this),
          userID))
    }

    // If stopping, wait to stop or ignore
    this.stoppingUserActors.get(userID) match {
      case null ⇒
//        logger.debug("UserActor %s was not in stopping set (but I don't know if it exists yet)".format(userID))

      case future ⇒
        try {
          logger.debug("Await.result(): Waiting while UserActor %s is stopping".format(userID))
          val stopped = Await.result(future, Duration(1000, TimeUnit.MILLISECONDS))
          if(!stopped) {
            // TODO: Add metric
            logger.warn("Await.result(): UserActor %s id not stop. Will remove from stopping set anyway".format(userID))
          }
        }
        catch {
          case e: java.util.concurrent.TimeoutException ⇒
            // TODO: Add metric
            logger.error("Timed-out while waiting for UserActor %s to stop. Will remove from stopping set anyway".format(userID), e)

          case e: Throwable ⇒
            logger.error("While Await(ing) UserActor %s to stop. Will remove from stopping set anyway".format(userID), e)
        }
        finally {
          this.stoppingUserActors.remove(userID)
        }
    }

    this._userActorCache.get(userID, new Callable[ActorRef] {
      def call(): ActorRef = {
        // Create new User Actor instance
        logger.debug("Creating new UserActor instance for %s".format(userID))
        val actorRef = _actorSystem.actorOf(Props.apply({
          aquarium.newInstance(classOf[UserActor])
        }), "userActor::%s".format(userID))

        // Cache it for subsequent calls
        _userActorCache.put(userID, actorRef)

        // Send the initialization message
        actorRef ! InitializeUserActorState(userID, TimeHelpers.nowMillis())

        actorRef
      }
    })
  }
}
