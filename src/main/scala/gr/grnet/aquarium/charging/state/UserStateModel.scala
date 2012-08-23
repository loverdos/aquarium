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

package gr.grnet.aquarium.charging.state

import gr.grnet.aquarium.charging.wallet.WalletEntry
import gr.grnet.aquarium.policy.ResourceType
import gr.grnet.aquarium.util.json.JsonSupport

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

trait UserStateModel extends JsonSupport {
  def id: String

  def idInStore: String = id

  def parentIDInStore: Option[String]

  def userID: String

  def occurredMillis: Long // When this user state was computed

  def latestResourceEventOccurredMillis: Long

  def totalCredits: Double

  /**
   * True iff this user state represents a full billing month.
   */
  def isFullBillingMonth: Boolean

  def billingYear: Int

  def billingMonth: Int

  def stateOfResources: Map[String, ResourcesChargingState]

  def billingPeriodOutOfSyncResourceEventsCounter: Long

  def agreementHistory: AgreementHistory

  def walletEntries: List[WalletEntry]

  def toWorkingUserState(resourceTypesMap: Map[String, ResourceType]): WorkingUserState
}

object UserStateModel {
  trait NamesT {
    final val userID = "userID"
    final val occurredMillis = "occurredMillis"
    final val isFullBillingMonth = "isFullBillingMonth"
    final val billingYear = "billingYear"
    final val billingMonth = "billingMonth"
  }

  object Names extends NamesT
}