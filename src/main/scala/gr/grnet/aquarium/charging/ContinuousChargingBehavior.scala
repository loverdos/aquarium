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

import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.policy.ResourceType
import gr.grnet.aquarium.charging.state.{AgreementHistoryModel, WorkingResourcesChargingState, WorkingResourceInstanceChargingState}
import scala.collection.mutable
import gr.grnet.aquarium.Aquarium
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.charging.wallet.WalletEntry

/**
 * In practice a resource usage will be charged for the total amount of usage
 * between resource usage changes.
 *
 * Example resource that might be adept to a continuous policy is diskspace, as in Pithos+ service.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
final class ContinuousChargingBehavior extends ChargingBehaviorSkeleton(Nil) {

  def computeCreditsToSubtract(
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState,
      oldCredits: Double,
      timeDeltaMillis: Long,
      unitPrice: Double
  ): (Double /* credits */, String /* explanation */) = {

    val oldAccumulatingAmount = workingResourceInstanceChargingState.oldAccumulatingAmount
    val credits = hrs(timeDeltaMillis) * oldAccumulatingAmount * unitPrice
    val explanation = "Time(%s) * OldTotal(%s) * Unit(%s)".format(
      hrs(timeDeltaMillis),
      oldAccumulatingAmount,
      unitPrice
    )

    (credits, explanation)

  }

  def computeSelectorPath(
      workingChargingBehaviorDetails: mutable.Map[String, Any],
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState,
      currentResourceEvent: ResourceEventModel,
      referenceTimeslot: Timeslot,
      totalCredits: Double
  ): List[String] = {
    Nil
  }

  def initialChargingDetails: Map[String, Any] = Map()

  def computeNewAccumulatingAmount(
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState,
      eventDetails: Map[String, String]
  ): Double = {
    workingResourceInstanceChargingState.oldAccumulatingAmount +
    workingResourceInstanceChargingState.currentValue
  }

  def constructImplicitEndEventFor(resourceEvent: ResourceEventModel, newOccurredMillis: Long) = {
    val details = resourceEvent.details
    val newDetails = ResourceEventModel.setAquariumSyntheticAndImplicitEnd(details)

    resourceEvent.withDetails(newDetails, newOccurredMillis)
  }

  override def processResourceEvent(
       aquarium: Aquarium,
       currentResourceEvent: ResourceEventModel,
       resourceType: ResourceType,
       billingMonthInfo: BillingMonthInfo,
       workingResourcesChargingState: WorkingResourcesChargingState,
       userAgreements: AgreementHistoryModel,
       totalCredits: Double,
       walletEntryRecorder: WalletEntry ⇒ Unit
   ): (Int, Double) = {
    (0,0)
  }
}

object ContinuousChargingBehavior {
  private[this] final val TheOne = new ContinuousChargingBehavior

  def apply(): ContinuousChargingBehavior = TheOne
}
