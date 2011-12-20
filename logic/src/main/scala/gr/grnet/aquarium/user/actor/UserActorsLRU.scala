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

package gr.grnet.aquarium.user.actor

import org.apache.solr.util.ConcurrentLRUCache
import akka.actor.ActorRef
import gr.grnet.aquarium.util.{Loggable, Lifecycle}

/**
 * This class holds an LRU cache for the user actors.
 *
 * The underlying implementation is borrowed from the Apache lucene+solr project(s).
 *
 * The provided collections-like API is neither Java- nor Scala-oriented.
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class UserActorsLRU(val upperWaterMark: Int, val lowerWatermark: Int) extends Lifecycle {
  private[this] val _cache = new ConcurrentLRUCache[String, ActorRef](
    upperWaterMark,
    lowerWatermark,
    ((upperWaterMark + lowerWatermark).toLong / 2).toInt,
    (3L * upperWaterMark / 4).toInt,
    true,
    false,
    EvictionListener)

  def put(userId: String, userActor: ActorRef): Unit = {
    _cache.put(userId, userActor)
  }

  def get(userId: String): Option[ActorRef] = {
    _cache.get(userId) match {
      case null     ⇒ None
      case actorRef ⇒ Some(actorRef)
    }
  }

  def size: Int   = _cache.size()
  def clear: Unit = _cache.clear()

  def start() = {}

  def stop() = {
    _cache.destroy()
  }
  
  private[this] object EvictionListener extends ConcurrentLRUCache.EvictionListener[String, ActorRef] with Loggable {
    def evictedEntry(userId: String, userActor: ActorRef): Unit = {
      logger.debug("Parking UserActor for userId = %s".format(userId))
      userActor ! UserActorPark
      // hopefully no need to further track these actors as they now enter a state machine which ultimately leads
      // to their shutting down
    }
  }
}

