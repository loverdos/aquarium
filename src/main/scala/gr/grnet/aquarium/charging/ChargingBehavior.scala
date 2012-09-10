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

package gr.grnet.aquarium.charging

import gr.grnet.aquarium.Aquarium
import gr.grnet.aquarium.charging.state.UserStateModel
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.event.{CreditsModel, DetailsModel}
import gr.grnet.aquarium.message.avro.gen.{WalletEntryMsg, EffectivePriceTableMsg, FullPriceTableMsg, ResourcesChargingStateMsg, ResourceTypeMsg, ResourceInstanceChargingStateMsg, ResourceEventMsg}
import gr.grnet.aquarium.uid.{PrefixedUIDGenerator, ConcurrentVMLocalUIDGenerator}
import gr.grnet.aquarium.policy.{EffectivePriceTableModel, FullPriceTableModel}

/**
 * A charging behavior indicates how charging for a resource will be done
 * wrt the various states a resource instance can be in.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

trait ChargingBehavior {
  def selectorLabelsHierarchy: List[String]

  /**
   * Provides some initial charging details that will be part of the mutable charging state
   * ([[gr.grnet.aquarium.message.avro.gen.ResourcesChargingStateMsg]]).
   */
  def initialChargingDetails: DetailsModel.Type

  def computeSelectorPath(
      ChargingBehaviorDetails: DetailsModel.Type,
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg,
      currentResourceEvent: ResourceEventMsg,
      referenceStartMillis: Long,
      referenceStopMillis: Long,
      totalCredits: CreditsModel.Type
  ): List[String]

  def selectEffectivePriceTableModel(
      fullPriceTable: FullPriceTableModel,
      chargingBehaviorDetails: DetailsModel.Type,
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg,
      currentResourceEvent: ResourceEventMsg,
      referenceStartMillis: Long,
      referenceStopMillis: Long,
      totalCredits: Double
  ): EffectivePriceTableModel

  /**
   *
   * @return The number of wallet entries recorded and the credit difference generated during processing (these are
   *         the credits to subtract from the total credits).
   */
  def processResourceEvent(
      aquarium: Aquarium,
      resourceEvent: ResourceEventMsg,
      resourceType: ResourceTypeMsg,
      billingMonthInfo: BillingMonthInfo,
      resourcesChargingState: ResourcesChargingStateMsg,
      userStateModel: UserStateModel,
      walletEntryRecorder: WalletEntryMsg ⇒ Unit
  ): (Int, CreditsModel.Type)

  def computeCreditsToSubtract(
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg,
      oldCredits: CreditsModel.Type,
      timeDeltaMillis: Long,
      unitPrice: Double
  ): (CreditsModel.Type, String /* explanation */)

  /**
   * Given the charging state of a resource instance and the details of the incoming message, compute the new
   * accumulating amount.
   */
  def computeNewAccumulatingAmount(
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg,
      eventDetails: DetailsModel.Type
  ): CreditsModel.Type

  def createVirtualEventsForRealtimeComputation(
      userID: String,
      resourceTypeName: String,
      resourceInstanceID: String,
      eventOccurredMillis: Long,
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg
  ): List[ResourceEventMsg]
}
