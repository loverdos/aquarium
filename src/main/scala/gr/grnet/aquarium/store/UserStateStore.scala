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

package gr.grnet.aquarium.store

import gr.grnet.aquarium.user.UserState
import com.ckkloverdos.maybe.Maybe

/**
 * A store for user state snapshots.
 *
 * This is used to hold snapshots of [[gr.grnet.aquarium.user.UserState]]
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

trait UserStateStore {

  /**
   * Store a user state
   */
  def storeUserState(userState: UserState): Maybe[RecordID]


  def storeUserState2(userState: UserState): Maybe[UserState] = {
    for {
      recordID <- storeUserState(userState)
    } yield {
      userState.copy(id = recordID.id)
    }
  }

  /**
   * Find a state by user ID
   */
  def findUserStateByUserId(userId: String): Maybe[UserState]

  /**
   * Find the most up-to-date user state for the particular billing period.
   */
  def findLatestUserStateForEndOfBillingMonth(userId: String, yearOfBillingMonth: Int, billingMonth: Int): Maybe[UserState]

  /**
   * Delete a state for a user
   */
  def deleteUserState(userId: String): Unit
}