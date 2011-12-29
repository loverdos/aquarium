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

package gr.grnet.aquarium.store.memory

import gr.grnet.aquarium.user.UserState
import gr.grnet.aquarium.Configurable
import com.ckkloverdos.props.Props
import gr.grnet.aquarium.store.{RecordID, UserStateStore}
import com.ckkloverdos.maybe.{NoVal, Just, Maybe}

/**
 * A user store backed by main memory.
 *
 * The IDs returned are the original user IDs.
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class MemUserStateStore extends UserStateStore with Configurable {
  private[this] val userStateByUserId = new java.util.concurrent.ConcurrentHashMap[String, Just[UserState]]()
  
  def configure(props: Props) = {
  }

  def storeUserState(userState: UserState): Maybe[RecordID] = {
    val userId = userState.userId
    val userStateJ = Just(userState)
    userStateByUserId.put(userId, userStateJ)
    Just(RecordID(userId))
  }

  def findUserStateByUserId(userId: String) = {
    userStateByUserId.get(userId) match {
      case null       ⇒ NoVal
      case userStateJ ⇒ userStateJ
    }
  }
}