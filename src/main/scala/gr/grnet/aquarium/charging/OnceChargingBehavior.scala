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
import gr.grnet.aquarium.{Aquarium, AquariumException}
import scala.collection.mutable
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.policy.{ResourceType, FullPriceTable}
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.charging.state.{WorkingResourceInstanceChargingState, AgreementHistoryModel, WorkingResourcesChargingState}
import gr.grnet.aquarium.charging.wallet.WalletEntry

/**
 * A charging behavior for which resource events just carry a credit amount that will be added to the total one.
 *
 * Examples are: a) Give a gift of X credits to the user, b) User bought a book, so charge for the book price.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
final class OnceChargingBehavior extends ChargingBehaviorSkeleton(Nil) {
  def computeCreditsToSubtract(
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState,
      oldCredits: Double,
      timeDeltaMillis: Long,
      unitPrice: Double
  ): (Double /* credits */, String /* explanation */) = {

    val currentValue = workingResourceInstanceChargingState.currentValue
    // Always remember to multiply with the `unitPrice`, since it scales the credits, depending on
    // the particular resource type tha applies.
    val credits = currentValue * unitPrice
    val explanation = "Value(%s) * UnitPrice(%s)".format(currentValue, unitPrice)

    (credits, explanation)
  }

  def computeSelectorPath(
      workingChargingBehaviorDetails: mutable.Map[String, Any],
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState,
      currentResourceEvent: ResourceEventModel,
      referenceTimeslot: Timeslot,
      totalCredits: Double
  ): List[String] = {
    List(FullPriceTable.DefaultSelectorKey)
  }

  override def processResourceEvent(
      aquarium: Aquarium,
      resourceEvent: ResourceEventModel,
      resourceType: ResourceType,
      billingMonthInfo: BillingMonthInfo,
      workingResourcesChargingState: WorkingResourcesChargingState,
      userAgreements: AgreementHistoryModel,
      totalCredits: Double,
      walletEntryRecorder: WalletEntry ⇒ Unit
  ): (Int, Double) = {
    // The credits are given in the value
    // But we cannot just apply them, since we also need to take into account the unit price.
    // Normally, the unit price is 1.0 but we have the flexibility to allow more stuff).

    // 1. Ensure proper initial state per resource and per instance
    ensureInitializedWorkingState(workingResourcesChargingState, resourceEvent)

    // 2. Fill in data from the new event
    val stateOfResourceInstance = workingResourcesChargingState.stateOfResourceInstance
    val workingResourcesChargingStateDetails = workingResourcesChargingState.details
    val instanceID = resourceEvent.instanceID
    val workingResourceInstanceChargingState = stateOfResourceInstance(instanceID)
    fillWorkingResourceInstanceChargingStateFromEvent(workingResourceInstanceChargingState, resourceEvent)

    computeWalletEntriesForNewEvent(
      resourceEvent,
      resourceType,
      billingMonthInfo,
      totalCredits,
      Timeslot(resourceEvent.occurredMillis, resourceEvent.occurredMillis + 1), // single point in time
      userAgreements.agreementByTimeslot,
      workingResourcesChargingStateDetails,
      workingResourceInstanceChargingState,
      aquarium.policyStore,
      walletEntryRecorder
    )
  }

  def initialChargingDetails: Map[String, Any] = Map()

  def computeNewAccumulatingAmount(
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState,
      eventDetails: Map[String, String]
  ): Double = {
    workingResourceInstanceChargingState.oldAccumulatingAmount
  }

  def createVirtualEventsForRealtimeComputation(
      userID: String,
      resourceTypeName: String,
      resourceInstanceID: String,
      eventOccurredMillis: Long,
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState
  ): List[ResourceEventModel] = {

    // We optimize and generate no virtual event
    Nil
  }
}

object OnceChargingBehavior {
  private[this] final val TheOne = new OnceChargingBehavior

  def apply(): OnceChargingBehavior = TheOne
}
