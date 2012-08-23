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
import gr.grnet.aquarium.charging.state.{AgreementHistoryModel, WorkingResourcesChargingState, WorkingResourceInstanceChargingState}
import gr.grnet.aquarium.charging.wallet.WalletEntry
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.policy.{FullPriceTable, ResourceType}
import gr.grnet.aquarium.util.LogHelpers.Debug
import scala.collection.mutable

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
    val explanation = "Time(%s) * OldTotal(%s) * UnitPrice(%s)".format(
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
    List(FullPriceTable.DefaultSelectorKey)
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
       resourceEvent: ResourceEventModel,
       resourceType: ResourceType,
       billingMonthInfo: BillingMonthInfo,
       workingResourcesChargingState: WorkingResourcesChargingState,
       userAgreements: AgreementHistoryModel,
       totalCredits: Double,
       walletEntryRecorder: WalletEntry ⇒ Unit
   ): (Int, Double) = {

    // 1. Ensure proper initial state per resource and per instance
    ensureInitializedWorkingState(workingResourcesChargingState, resourceEvent)

    // 2. Fill in data from the new event
    val stateOfResourceInstance = workingResourcesChargingState.stateOfResourceInstance
    val workingResourcesChargingStateDetails = workingResourcesChargingState.details
    val instanceID = resourceEvent.instanceID
    val workingResourceInstanceChargingState = stateOfResourceInstance(instanceID)
    fillWorkingResourceInstanceChargingStateFromEvent(workingResourceInstanceChargingState, resourceEvent)

    val previousEvent = workingResourceInstanceChargingState.previousEvents.headOption match {
      case Some(previousEvent) ⇒
        Debug(logger, "I have previous event %s", previousEvent.toDebugString)
        previousEvent


      case None ⇒
        // We do not have the needed previous event, so this must be the first resource event of its kind, ever.
        // Let's see if we can create a dummy previous event.
        Debug(logger, "First event of its kind %s", resourceEvent.toDebugString)

        val dummyFirstEventDetails = Map(
            ResourceEventModel.Names.details_aquarium_is_synthetic   -> "true",
            ResourceEventModel.Names.details_aquarium_is_dummy_first -> "true",
            ResourceEventModel.Names.details_aquarium_reference_event_id -> resourceEvent.id,
            ResourceEventModel.Names.details_aquarium_reference_event_id_in_store -> resourceEvent.stringIDInStoreOrEmpty
        )

        val dummyFirstEventValue = 0.0 // TODO From configuration
        val dummyFirstEvent = resourceEvent.withDetailsAndValue(
            dummyFirstEventDetails,
            dummyFirstEventValue,
            billingMonthInfo.monthStartMillis // TODO max(billingMonthInfo.monthStartMillis, userAgreementModel.validFromMillis)
        )

        Debug(logger, "Dummy first event %s", dummyFirstEvent.toDebugString)

        dummyFirstEvent
    }

    val retval = computeWalletEntriesForNewEvent(
      resourceEvent,
      resourceType,
      billingMonthInfo,
      totalCredits,
      Timeslot(previousEvent.occurredMillis, resourceEvent.occurredMillis),
      userAgreements.agreementByTimeslot,
      workingResourcesChargingStateDetails,
      workingResourceInstanceChargingState,
      aquarium.policyStore,
      walletEntryRecorder
    )

    // We need just one previous event, so we update it
    workingResourceInstanceChargingState.setOnePreviousEvent(resourceEvent)

    retval
  }
}

object ContinuousChargingBehavior {
  private[this] final val TheOne = new ContinuousChargingBehavior

  def apply(): ContinuousChargingBehavior = TheOne
}
