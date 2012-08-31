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

import gr.grnet.aquarium.charging.state.{WorkingResourceInstanceChargingState, WorkingResourcesChargingState, AgreementHistoryModel}
import gr.grnet.aquarium.charging.wallet.WalletEntry
import gr.grnet.aquarium.computation.{TimeslotComputations, BillingMonthInfo}
import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.policy.{FullPriceTable, EffectivePriceTable, UserAgreementModel, ResourceType}
import gr.grnet.aquarium.store.PolicyStore
import gr.grnet.aquarium.util._
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.{Aquarium, AquariumInternalError}
import scala.collection.immutable
import scala.collection.mutable


/**
 * A charging behavior indicates how charging for a resource will be done
 * wrt the various states a resource can be.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

abstract class ChargingBehaviorSkeleton(
    final val selectorLabelsHierarchy: List[String]
) extends ChargingBehavior with Loggable {

  protected def HrsOfMillis(millis: Double) = {
    val hours = millis / (1000 * 60 * 60).toDouble
    val roundedHours = hours
    roundedHours
  }

  protected def MBsOfBytes(bytes: Double) = {
    bytes / (1024 * 1024).toDouble
  }

  protected def rcDebugInfo(rcEvent: ResourceEventModel) = {
    rcEvent.toDebugString
  }

  protected def newWorkingResourceInstanceChargingState() = {
    new WorkingResourceInstanceChargingState(
      mutable.Map(),
      Nil,
      Nil,
      0.0,
      0.0,
      0.0,
      0.0
    )
  }

  final protected def ensureInitializedWorkingState(
      workingResourcesChargingState: WorkingResourcesChargingState,
      resourceEvent: ResourceEventModel
  ) {
    ensureInitializedResourcesChargingStateDetails(workingResourcesChargingState.details)
    ensureInitializedResourceInstanceChargingState(workingResourcesChargingState, resourceEvent)
  }

  protected def ensureInitializedResourcesChargingStateDetails(details: mutable.Map[String, Any]) {}

  protected def ensureInitializedResourceInstanceChargingState(
      workingResourcesChargingState: WorkingResourcesChargingState,
      resourceEvent: ResourceEventModel
  ) {

    val instanceID = resourceEvent.instanceID
    val stateOfResourceInstance = workingResourcesChargingState.stateOfResourceInstance

    stateOfResourceInstance.get(instanceID) match {
      case None ⇒
        stateOfResourceInstance(instanceID) = newWorkingResourceInstanceChargingState()

      case _ ⇒
    }
  }

  protected def fillWorkingResourceInstanceChargingStateFromEvent(
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState,
      resourceEvent: ResourceEventModel
  ) {

    workingResourceInstanceChargingState.currentValue = resourceEvent.value
  }

  protected def computeWalletEntriesForNewEvent(
      resourceEvent: ResourceEventModel,
      resourceType: ResourceType,
      billingMonthInfo: BillingMonthInfo,
      totalCredits: Double,
      referenceTimeslot: Timeslot,
      agreementByTimeslot: immutable.SortedMap[Timeslot, UserAgreementModel],
      workingResourcesChargingStateDetails: mutable.Map[String, Any],
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState,
      policyStore: PolicyStore,
      walletEntryRecorder: WalletEntry ⇒ Unit
  ): (Int, Double) = {

    val userID = resourceEvent.userID
    val resourceEventDetails = resourceEvent.details

    var _oldTotalCredits = totalCredits

    var _newAccumulatingAmount = computeNewAccumulatingAmount(workingResourceInstanceChargingState, resourceEventDetails)
    // It will also update the old one inside the data structure.
    workingResourceInstanceChargingState.setNewAccumulatingAmount(_newAccumulatingAmount)

    val policyByTimeslot = policyStore.loadSortedPolicyModelsWithin(
      referenceTimeslot.from.getTime,
      referenceTimeslot.to.getTime
    )

    val effectivePriceTableSelector: FullPriceTable ⇒ EffectivePriceTable = fullPriceTable ⇒ {
      this.selectEffectivePriceTable(
        fullPriceTable,
        workingResourcesChargingStateDetails,
        workingResourceInstanceChargingState,
        resourceEvent,
        referenceTimeslot,
        totalCredits
      )
    }

    val initialChargeslots = TimeslotComputations.computeInitialChargeslots(
      referenceTimeslot,
      policyByTimeslot,
      agreementByTimeslot,
      effectivePriceTableSelector
    )

    val fullChargeslots = initialChargeslots.map {
      case chargeslot@Chargeslot(startMillis, stopMillis, unitPrice, _, _) ⇒
        val timeDeltaMillis = stopMillis - startMillis

        val (creditsToSubtract, explanation) = this.computeCreditsToSubtract(
          workingResourceInstanceChargingState,
          _oldTotalCredits, // FIXME ??? Should recalculate ???
          timeDeltaMillis,
          unitPrice
        )

        val newChargeslot = chargeslot.copyWithCreditsToSubtract(creditsToSubtract, explanation)
        newChargeslot
    }

    if(fullChargeslots.length == 0) {
      throw new AquariumInternalError("No chargeslots computed for resource event %s".format(resourceEvent.id))
    }

    val sumOfCreditsToSubtract = fullChargeslots.map(_.creditsToSubtract).sum
    val newTotalCredits = _oldTotalCredits - sumOfCreditsToSubtract

    val newWalletEntry = WalletEntry(
      userID,
      sumOfCreditsToSubtract,
      _oldTotalCredits,
      newTotalCredits,
      TimeHelpers.nowMillis(),
      referenceTimeslot,
      billingMonthInfo.year,
      billingMonthInfo.month,
      fullChargeslots,
      resourceEvent :: workingResourceInstanceChargingState.previousEvents,
      resourceType,
      resourceEvent.isSynthetic
    )

    logger.debug("newWalletEntry = {}", newWalletEntry.toJsonString)

    walletEntryRecorder.apply(newWalletEntry)

    (1, sumOfCreditsToSubtract)
  }


  def selectEffectivePriceTable(
      fullPriceTable: FullPriceTable,
      workingChargingBehaviorDetails: mutable.Map[String, Any],
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState,
      currentResourceEvent: ResourceEventModel,
      referenceTimeslot: Timeslot,
      totalCredits: Double
  ): EffectivePriceTable = {

    val selectorPath = computeSelectorPath(
      workingChargingBehaviorDetails,
      workingResourceInstanceChargingState,
      currentResourceEvent,
      referenceTimeslot,
      totalCredits
    )

    fullPriceTable.effectivePriceTableOfSelectorForResource(selectorPath, currentResourceEvent.safeResource, logger)
  }

  /**
   * Given the charging state of a resource instance and the details of the incoming message, compute the new
   * accumulating amount.
   */
  def computeNewAccumulatingAmount(
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState,
      eventDetails: Map[String, String]
  ): Double


  def constructDummyFirstEventFor(actualFirst: ResourceEventModel, newOccurredMillis: Long): ResourceEventModel = {

    val newDetails = Map(
      ResourceEventModel.Names.details_aquarium_is_synthetic   -> "true",
      ResourceEventModel.Names.details_aquarium_is_dummy_first -> "true",
      ResourceEventModel.Names.details_aquarium_reference_event_id -> actualFirst.id,
      ResourceEventModel.Names.details_aquarium_reference_event_id_in_store -> actualFirst.stringIDInStoreOrEmpty
    )

    actualFirst.withDetailsAndValue(newDetails, 0.0, newOccurredMillis)
  }
}
