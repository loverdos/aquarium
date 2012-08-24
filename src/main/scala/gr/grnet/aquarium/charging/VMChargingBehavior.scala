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

import gr.grnet.aquarium.{Aquarium, AquariumInternalError}
import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import VMChargingBehavior.Selectors.Power
import VMChargingBehavior.SelectorLabels.PowerStatus
import gr.grnet.aquarium.policy.ResourceType
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.charging.state.{WorkingResourceInstanceChargingState, WorkingResourcesChargingState, AgreementHistoryModel}
import gr.grnet.aquarium.charging.wallet.WalletEntry
import scala.collection.mutable

/**
 * The new [[gr.grnet.aquarium.charging.ChargingBehavior]] for VMs usage.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
final class VMChargingBehavior extends ChargingBehaviorSkeleton(List(PowerStatus)) {
  def computeCreditsToSubtract(
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState,
      oldCredits: Double,
      timeDeltaMillis: Long,
      unitPrice: Double
  ): (Double /* credits */, String /* explanation */) = {

    val credits = HrsOfMillis(timeDeltaMillis) * unitPrice
    val explanation = "Hours(%s) * UnitPrice(%s)".format(HrsOfMillis(timeDeltaMillis), unitPrice)

    (credits, explanation)

  }

  def computeSelectorPath(
      workingChargingBehaviorDetails: mutable.Map[String, Any],
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState,
      currentResourceEvent: ResourceEventModel,
      referenceTimeslot: Timeslot,
      totalCredits: Double
  ): List[String] = {
    // FIXME
    List(Power.powerOn) // compute prices for power-on state
  }

  def initialChargingDetails: Map[String, Any] = Map()

  def computeNewAccumulatingAmount(
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState,
      eventDetails: Map[String, String]
  ): Double = {
    workingResourceInstanceChargingState.currentValue
  }

  def constructImplicitEndEventFor(resourceEvent: ResourceEventModel, newOccurredMillis: Long) = {
    assert(VMChargingBehaviorValues.isONValue(resourceEvent.value))

    val details = resourceEvent.details
    val newDetails = ResourceEventModel.setAquariumSyntheticAndImplicitEnd(details)
    val newValue   = VMChargingBehaviorValues.OFF

    resourceEvent.withDetailsAndValue(newDetails, newValue, newOccurredMillis)
  }

  def constructImplicitStartEventFor(resourceEvent: ResourceEventModel) = {
    throw new AquariumInternalError("constructImplicitStartEventFor() Not compliant with %s".format(this))
  }

  /**
   *
   * @return The number of wallet entries recorded and the new total credits
   */
  override def processResourceEvent(
      aquarium: Aquarium,
      currentResourceEvent: ResourceEventModel,
      resourceType: ResourceType,
      billingMonthInfo: BillingMonthInfo,
      workingState: WorkingResourcesChargingState,
      userAgreements: AgreementHistoryModel,
      totalCredits: Double,
      walletEntryRecorder: WalletEntry ⇒ Unit
  ): (Int, Double) = {

    // FIXME Implement
    (0,0)
  }

  def createVirtualEventsForRealtimeComputation(
      userID: String,
      resourceTypeName: String,
      resourceInstanceID: String,
      eventOccurredMillis: Long,
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState
  ): List[ResourceEventModel] = {
    // FIXME Implement
    Nil
  }
}

object VMChargingBehavior {
  object SelectorLabels {
    final val PowerStatus = "Power Status (ON/OFF)"
  }

  object Selectors {
    object Power {
      // When the VM is powered on
      final val powerOn = "powerOn"
      // When the VM is powered off
      final val powerOff = "powerOff"
    }
  }
}
