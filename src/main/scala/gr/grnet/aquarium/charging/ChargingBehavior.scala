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
import gr.grnet.aquarium.charging.state.{WorkingResourceInstanceChargingState, AgreementHistoryModel, WorkingResourcesChargingState}
import gr.grnet.aquarium.charging.wallet.WalletEntry
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.policy.{ResourceType, EffectivePriceTable, FullPriceTable}
import gr.grnet.aquarium.uid.{PrefixedUIDGenerator, ConcurrentVMLocalUIDGenerator, UIDGenerator}
import scala.collection.mutable

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
   * ([[gr.grnet.aquarium.charging.state.WorkingResourcesChargingState]]).
   */
  def initialChargingDetails: Map[String, Any]

  def computeSelectorPath(
      workingChargingBehaviorDetails: mutable.Map[String, Any],
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState,
      currentResourceEvent: ResourceEventModel,
      referenceTimeslot: Timeslot,
      totalCredits: Double
  ): List[String]

  def selectEffectivePriceTable(
      fullPriceTable: FullPriceTable,
      workingChargingBehaviorDetails: mutable.Map[String, Any],
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState,
      currentResourceEvent: ResourceEventModel,
      referenceTimeslot: Timeslot,
      totalCredits: Double
  ): EffectivePriceTable

  /**
   *
   * @return The number of wallet entries recorded and the credit difference generated during processing (these are
   *         the credits to subtract from the total credits).
   */
  def processResourceEvent(
      aquarium: Aquarium,
      currentResourceEvent: ResourceEventModel,
      resourceType: ResourceType,
      billingMonthInfo: BillingMonthInfo,
      workingResourcesChargingState: WorkingResourcesChargingState,
      userAgreements: AgreementHistoryModel,
      totalCredits: Double,
      walletEntryRecorder: WalletEntry ⇒ Unit
  ): (Int, Double)

  def computeCreditsToSubtract(
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState,
      oldCredits: Double,
      timeDeltaMillis: Long,
      unitPrice: Double
  ): (Double /* credits */, String /* explanation */)

  /**
   * Given the charging state of a resource instance and the details of the incoming message, compute the new
   * accumulating amount.
   */
  def computeNewAccumulatingAmount(
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState,
      eventDetails: Map[String, String]
  ): Double

  def createVirtualEventsForRealtimeComputation(
      userID: String,
      resourceTypeName: String,
      resourceInstanceID: String,
      eventOccurredMillis: Long,
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState
  ): List[ResourceEventModel]
}

object ChargingBehavior {
  final val VirtualEventsIDGen = new PrefixedUIDGenerator("virt-", new ConcurrentVMLocalUIDGenerator(0L))
}