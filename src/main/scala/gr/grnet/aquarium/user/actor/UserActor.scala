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

import gr.grnet.aquarium.user.UserState
import gr.grnet.aquarium.util.Loggable
import gr.grnet.aquarium.processor.actor.{UserResponseGetBalance, UserRequestGetBalance}
import scala.PartialFunction
import gr.grnet.aquarium.actor._
import com.ckkloverdos.maybe.Maybe


/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class UserActor extends AquariumActor with Loggable {
  @volatile
  private[this] var _userId: String = _
  @volatile
  private[this] var _isInitialized: Boolean = false
  @volatile
  private[this] var _userState: UserState = _
  @volatile
  private[this] var _actorProvider: ActorProvider = _

  def role = UserActorRole

  protected def receive: Receive = {
    case UserActorStop ⇒
      self.stop()

    case m @ UserActorInitWithUserId(userId) ⇒
      this._userId = userId
      this._isInitialized = true
      // TODO: query DB etc to get internal state
      logger.info("Setup my userId = %s".format(userId))

    case m @ ActorProviderConfigured(actorProvider) ⇒
      this._actorProvider = actorProvider
      logger.info("Configured %s with %s".format(this, m))

    case m @ UserRequestGetBalance(userId, timestamp) ⇒
      if(this._userId != userId) {
        logger.error("Received %s but my userId = %s".format(m, this._userId))
        // TODO: throw an exception here
      } else {
        // This is the big party.
        // Get the user state, if it exists and make sure it is not stale.

        // Do we have a user state?
        if(_userState ne null) {
          // Yep, we do. See what there is inside it.
          val credits = _userState.credits
          val creditsTimestamp = credits.snapshotTime

          // Check if data is stale
          if(creditsTimestamp + 10000 > timestamp) {
            // No, it's OK
            self reply UserResponseGetBalance(userId, credits.data)
          } else {
            // Yep, data is stale and must recompute balance
            // FIXME: implement
            logger.error("FIXME: Should have computed a new value for %s".format(credits))
            self reply UserResponseGetBalance(userId, credits.data)
          }
        } else {
          // Nope. No user state exists. Must reproduce one
          // FIXME: implement
          logger.error("FIXME: Should have computed the user state for userId = %s".format(userId))
          self reply UserResponseGetBalance(userId, Maybe(userId.toDouble).getOr(10.5))
        }
      }
  }
}