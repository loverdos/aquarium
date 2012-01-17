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

package gr.grnet.aquarium.user

import gr.grnet.aquarium.store.ResourceEventStore

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object UserStateComputations {
  def createBlankState(userId: String, agreementName: String = "default") = {
    val now = 0L
    UserState(
      userId,
      now,
      ActiveSuspendedSnapshot(false, now),
      CreditSnapshot(0, now),
      AgreementSnapshot(agreementName, now),
      RolesSnapshot(List(), now),
      PaymentOrdersSnapshot(Nil, now),
      OwnedGroupsSnapshot(Nil, now),
      GroupMembershipsSnapshot(Nil, now),
      OwnedResourcesSnapshot(List(), now)
    )
  }

  def runBilling(initialUserState: UserState,
                 startMillis: Long,
                 stopMillis: Long,
                 rcEventStore: ResourceEventStore): UserState = {

    val userId = initialUserState.userId
    val rcEvents = rcEventStore.findResourceEventsForReceivedPeriod(userId, startMillis, stopMillis)

    rcEvents.foldLeft[UserState](initialUserState) {
      case (userState, rcEvent) ⇒
        userState
    }
  }
}