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

import gr.grnet.aquarium.util.json.JsonSupport
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import gr.grnet.aquarium.charging.wallet.WalletEntry
import gr.grnet.aquarium.charging.reason.ChargingReason
import gr.grnet.aquarium.policy.ResourceType

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

  def totalCredits: Double

  def theFullBillingMonth: Option[BillingMonthInfo]

  def chargingReason: ChargingReason

  def previousResourceEvents: List[ResourceEventModel]

  def implicitlyIssuedStartEvents: List[ResourceEventModel]

  def accumulatingAmountOfResourceInstance: Map[String, Double]

  def chargingDataOfResourceInstance: Map[String, Map[String, Any]]

  def billingPeriodOutOfSyncResourceEventsCounter: Long

  def agreementHistory: AgreementHistory

  def walletEntries: List[WalletEntry]

  def toWorkingUserState(resourceTypesMap: Map[String, ResourceType]): WorkingUserState

  def newWithChargingReason(changeReason: ChargingReason): UserStateModel
}

object UserStateModel {
  trait NamesT {
    final val userID = "userID"
    final val occurredMillis = "occurredMillis"
    final val theFullBillingMonth_year = "theFullBillingMonth.year"
    final val theFullBillingMonth_month = "theFullBillingMonth.month"
  }

  object Names extends NamesT
}