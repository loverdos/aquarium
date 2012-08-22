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

import scala.collection.immutable
import scala.collection.mutable

import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import gr.grnet.aquarium.{Aquarium, AquariumInternalError}
import gr.grnet.aquarium.policy.{FullPriceTable, EffectivePriceTable, UserAgreementModel, ResourceType}
import gr.grnet.aquarium.util._
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.charging.wallet.WalletEntry
import gr.grnet.aquarium.computation.{TimeslotComputations, BillingMonthInfo}
import gr.grnet.aquarium.charging.state.{WorkingResourceInstanceChargingState, WorkingResourcesChargingState, AgreementHistoryModel}
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.store.PolicyStore

/**
 * A charging behavior indicates how charging for a resource will be done
 * wrt the various states a resource can be.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

abstract class ChargingBehaviorSkeleton(
    final val selectorLabelsHierarchy: List[String]
) extends ChargingBehavior with Loggable {

  protected def hrs(millis: Double) = {
    val hours = millis / 1000 / 60 / 60
    val roundedHours = hours
    roundedHours
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

  final protected def ensureWorkingState(
      workingResourcesChargingState: WorkingResourcesChargingState,
      resourceEvent: ResourceEventModel
  ) {
    ensureResourcesChargingStateDetails(workingResourcesChargingState.details)
    ensureResourceInstanceChargingState(workingResourcesChargingState, resourceEvent)
  }

  protected def ensureResourcesChargingStateDetails(
    details: mutable.Map[String, Any]
  ) {}

  protected def ensureResourceInstanceChargingState(
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

    val policyByTimeslot = policyStore.loadAndSortPoliciesWithin(
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

    (1, newTotalCredits)
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

    fullPriceTable.effectivePriceTableOfSelectorForResource(selectorPath, currentResourceEvent.safeResource)
  }

  /**
   * A generic implementation for charging a resource event.
   * TODO: Ditch this in favor of completely ahdoc behaviors.
   *
   * @return The number of wallet entries recorded and the new total credits
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
  ): (Int, Double) = {
    (0,0)

    /*val currentResourceEventDebugInfo = rcDebugInfo(currentResourceEvent)

    val isBillable = this.isBillableEvent(currentResourceEvent)
    val retval = if(!isBillable) {
      // The resource event is not billable.
      Debug(logger, "Ignoring not billable %s", currentResourceEventDebugInfo)
      (0, totalCredits)
    } else {
      // The resource event is billable.
      // Find the previous event if needed.
      // This is (potentially) needed to calculate new credit amount and new resource instance amount
      if(this.needsPreviousEventForCreditAndAmountCalculation) {
        if(previousResourceEventOpt.isDefined) {
          val previousResourceEvent = previousResourceEventOpt.get
          val previousValue = previousResourceEvent.value

          Debug(logger, "I have previous event %s", previousResourceEvent.toDebugString)

          computeChargeslots(
            chargingData,
            previousResourceEventOpt,
            currentResourceEvent,
            billingMonthInfo,
            Timeslot(previousResourceEvent.occurredMillis, currentResourceEvent.occurredMillis),
            resourceType,
            userAgreements.agreementByTimeslot,
            previousValue,
            totalCredits,
            aquarium.policyStore,
            walletEntryRecorder
          )
        } else {
          // We do not have the needed previous event, so this must be the first resource event of its kind, ever.
          // Let's see if we can create a dummy previous event.
          val actualFirstEvent = currentResourceEvent

          // FIXME: Why && ?
          if(this.isBillableFirstEvent(actualFirstEvent) && this.mustGenerateDummyFirstEvent) {
            Debug(logger, "First event of its kind %s", currentResourceEventDebugInfo)

            val dummyFirst = this.constructDummyFirstEventFor(currentResourceEvent, billingMonthInfo.monthStartMillis)
            Debug(logger, "Dummy first event %s", dummyFirst.toDebugString)

            val previousResourceEvent = dummyFirst
            val previousValue = previousResourceEvent.value

            computeChargeslots(
              chargingData,
              Some(previousResourceEvent),
              currentResourceEvent,
              billingMonthInfo,
              Timeslot(previousResourceEvent.occurredMillis, currentResourceEvent.occurredMillis),
              resourceType,
              userAgreements.agreementByTimeslot,
              previousValue,
              totalCredits,
              aquarium.policyStore,
              walletEntryRecorder
            )
          } else {
            Debug(logger, "Ignoring first event of its kind %s", currentResourceEventDebugInfo)
            // userStateWorker.updateIgnored(currentResourceEvent)
            (0, totalCredits)
          }
        }
      } else {
        // No need for previous event. One event does it all.
        computeChargeslots(
          chargingData,
          None,
          currentResourceEvent,
          billingMonthInfo,
          Timeslot(currentResourceEvent.occurredMillis, currentResourceEvent.occurredMillis + 1),
          resourceType,
          userAgreements.agreementByTimeslot,
          0.0,
          totalCredits,
          aquarium.policyStore,
          walletEntryRecorder
        )
      }
    }

    retval*/
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
