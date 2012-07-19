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

package gr.grnet.aquarium.store.mongodb

import gr.grnet.aquarium.charging.state.{WorkingUserState, UserStateModel}
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.charging.reason.ChargingReason
import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import gr.grnet.aquarium.computation.state.parts.AgreementHistory
import gr.grnet.aquarium.charging.wallet.WalletEntry
import gr.grnet.aquarium.converter.{JsonTextFormat, StdConverters}
import gr.grnet.aquarium.policy.ResourceType

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class MongoDBUserState(
    _id: String,
    parentIDInStore: Option[String],
    userID: String,
    occurredMillis: Long,
    totalCredits: Double,
    theFullBillingMonth: Option[BillingMonthInfo],
    chargingReason: ChargingReason,
    previousResourceEvents: List[ResourceEventModel],
    implicitlyIssuedStartEvents: List[ResourceEventModel],
    accumulatingAmountOfResourceInstance: Map[String, Double],
    chargingDataOfResourceInstance: Map[String, Map[String, Any]],
    billingPeriodOutOfSyncResourceEventsCounter: Long,
    agreementHistory: AgreementHistory,
    walletEntries: List[WalletEntry]
) extends UserStateModel {

  def id = _id

  def toWorkingUserState(resourceTypesMap: Map[String, ResourceType]): WorkingUserState = null

  def newWithChargingReason(changeReason: ChargingReason): UserStateModel = null
}

object MongoDBUserState {
  def fromJSONString(json: String): MongoDBUserState = {
    StdConverters.AllConverters.convertEx[MongoDBUserState](JsonTextFormat(json))
  }

  def fromOther(model: UserStateModel, _id: String): MongoDBUserState = {
    MongoDBUserState(
      _id,
      model.parentIDInStore,
      model.userID,
      model.occurredMillis,
      model.totalCredits,
      model.theFullBillingMonth,
      model.chargingReason,
      model.previousResourceEvents,
      model.implicitlyIssuedStartEvents,
      model.accumulatingAmountOfResourceInstance,
      model.chargingDataOfResourceInstance,
      model.billingPeriodOutOfSyncResourceEventsCounter,
      model.agreementHistory,
      model.walletEntries
    )
  }
}