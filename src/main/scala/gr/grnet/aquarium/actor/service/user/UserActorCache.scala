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
package user

import akka.actor.ActorRef
import gr.grnet.aquarium.util.{Loggable, Lifecycle}
import com.google.common.cache._

/**
 * An actor cache implementation using Guava.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
object UserActorCache extends Lifecycle with Loggable {

  private lazy val cache: Cache[String, ActorRef] =
    CacheBuilder.newBuilder()
      .maximumSize(1000)
      .initialCapacity(100)
      .concurrencyLevel(20)
      .removalListener(EvictionListener)
      .build()

  private[this] object EvictionListener extends RemovalListener[String, ActorRef] with Loggable {

    def onRemoval(rn: RemovalNotification[String, ActorRef]) {
      val userID = rn.getKey
      val userActor = rn.getValue

      logger.debug("Parking UserActor for userID = %s".format(userID))
      UserActorSupervisor.supervisor.unlink(userActor)
      // Check this is received after any currently servicing business logic message.
      userActor.stop()
    }
  }

  def start() = {
    logStartingF(""){}{}
  }

  def stop() = {
    logStoppingF("") {
      cache.invalidateAll
      cache.cleanUp
    } {}
  }

  def put(userID: String, userActor: ActorRef): Unit = {
    cache.put(userID, userActor)
  }

  def get(userID: String): Option[ActorRef] =
    cache.getIfPresent(userID) match {
      case null ⇒ None
      case actorRef ⇒ Some(actorRef)
    }

  def invalidate(userID: String): Unit = {
    cache.invalidate(userID)
  }
}
