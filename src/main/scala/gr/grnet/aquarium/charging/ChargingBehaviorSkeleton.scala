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

import gr.grnet.aquarium.{Real, Aquarium, AquariumInternalError}
import gr.grnet.aquarium.computation.{TimeslotComputations, BillingMonthInfo}
import gr.grnet.aquarium.event.DetailsModel
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.message.avro.gen.{EffectivePriceTableMsg, FullPriceTableMsg, ResourceTypeMsg, WalletEntryMsg, ResourceInstanceChargingStateMsg, ResourcesChargingStateMsg, ResourceEventMsg}
import gr.grnet.aquarium.message.avro.{MessageHelpers, AvroHelpers, MessageFactory}
import gr.grnet.aquarium.policy.{PolicyModel, EffectivePriceTableModel, FullPriceTableModel, UserAgreementModel}
import gr.grnet.aquarium.store.PolicyStore
import gr.grnet.aquarium.util._
import gr.grnet.aquarium.util.date.TimeHelpers
import java.{util ⇒ ju}
import java.util.{List ⇒ JList, ArrayList ⇒ JArrayList}
import scala.collection.immutable
import scala.collection.mutable
import gr.grnet.aquarium.message.MessageConstants


/**
 * A charging behavior indicates how charging for a resource will be done
 * wrt the various states a resource can be.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

abstract class ChargingBehaviorSkeleton(
    final val selectorLabelsHierarchy: List[String]
) extends ChargingBehavior with Loggable {

  @inline final protected def rcDebugInfo(rcEvent: ResourceEventMsg) = {
    AvroHelpers.jsonStringOfSpecificRecord(rcEvent)
  }

  protected def newResourceInstanceChargingStateMsg(
      clientID: String,
      resource: String,
      instanceID: String
  ) = {

    MessageFactory.newResourceInstanceChargingStateMsg(

      DetailsModel.make,
      new JArrayList[ResourceEventMsg](),
      new JArrayList[ResourceEventMsg](),
      Real.toMsgField(0.0),
      Real.toMsgField(0.0),
      Real.toMsgField(0.0),
      Real.toMsgField(0.0),
      clientID,
      resource,
      instanceID
    )
  }

  final protected def ensureInitializedWorkingState(
      resourcesChargingState: ResourcesChargingStateMsg,
      resourceEvent: ResourceEventMsg
  ) {
    ensureInitializedResourcesChargingStateDetails(resourcesChargingState.getDetails)
    ensureInitializedResourceInstanceChargingState(resourcesChargingState, resourceEvent)
  }

  protected def ensureInitializedResourcesChargingStateDetails(details: DetailsModel.Type) {}

  protected def ensureInitializedResourceInstanceChargingState(
      resourcesChargingState: ResourcesChargingStateMsg,
      resourceEvent: ResourceEventMsg
  ) {

    val instanceID = resourceEvent.getInstanceID
    val clientID = resourceEvent.getClientID
    val resource = resourceEvent.getResource
    val stateOfResourceInstance = resourcesChargingState.getStateOfResourceInstance

    stateOfResourceInstance.get(instanceID) match {
      case null ⇒
        stateOfResourceInstance.put(
          instanceID,
          newResourceInstanceChargingStateMsg(clientID, resource, instanceID)
        )

      case _ ⇒
    }
  }

  protected def fillWorkingResourceInstanceChargingStateFromEvent(
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg,
      resourceEvent: ResourceEventMsg
  ) {

    resourceInstanceChargingState.setCurrentValue(resourceEvent.getValue)
  }

  protected def computeWalletEntriesForNewEvent(
      resourceEvent: ResourceEventMsg,
      resourceType: ResourceTypeMsg,
      billingMonthInfo: BillingMonthInfo,
      totalCredits: Real,
      referenceStartMillis: Long,
      referenceStopMillis: Long,
      agreementByTimeslot: immutable.SortedMap[Timeslot, UserAgreementModel],
      workingResourcesChargingStateDetails: DetailsModel.Type,
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg,
      aquarium: Aquarium,
      walletEntryRecorder: WalletEntryMsg ⇒ Unit
  ): (Int, Real) = {

    val userID = resourceEvent.getUserID
    val resourceEventDetails = resourceEvent.getDetails

    var _oldTotalReal = totalCredits

    var _newAccumulatingAmount = computeNewAccumulatingAmount(resourceInstanceChargingState, resourceEventDetails)
    // It will also update the old one inside the data structure.
    resourceInstanceChargingState.setOldAccumulatingAmount(resourceInstanceChargingState.getAccumulatingAmount)
    resourceInstanceChargingState.setAccumulatingAmount(Real.toMsgField(_newAccumulatingAmount))

    val policyByTimeslot = aquarium.policyStore.loadSortedPolicyModelsWithin(
      referenceStartMillis,
      referenceStopMillis
    )

    val effectivePriceTableModelSelector: FullPriceTableModel ⇒ EffectivePriceTableModel = fullPriceTable ⇒ {
      this.selectEffectivePriceTableModel(
        fullPriceTable,
        workingResourcesChargingStateDetails,
        resourceInstanceChargingState,
        resourceEvent,
        referenceStartMillis,
        referenceStopMillis,
        totalCredits
      )
    }

    val fullPriceTableModelGetter = aquarium.unsafeFullPriceTableModelForAgreement(_,_)

    val initialChargeslots = TimeslotComputations.computeInitialChargeslots(
      Timeslot(referenceStartMillis, referenceStopMillis),
      policyByTimeslot,
      agreementByTimeslot,
      fullPriceTableModelGetter,
      effectivePriceTableModelSelector
    )

    val fullChargeslots = initialChargeslots.map { cs ⇒
      val timeDeltaMillis = cs.getStopMillis - cs.getStartMillis

      val (creditsToSubtract, explanation) = this.computeCreditsToSubtract(
        resourceInstanceChargingState,
        _oldTotalReal, // FIXME ??? Should recalculate ???
        timeDeltaMillis,
        Real(cs.getUnitPrice)
      )

      cs.setCreditsToSubtract(creditsToSubtract.toString())
      cs.setExplanation(explanation)

      cs
    }

    if(fullChargeslots.length == 0) {
      throw new AquariumInternalError("No chargeslots computed for resource event %s".format(resourceEvent.getOriginalID))
    }

    val sumOfRealToSubtract = fullChargeslots.map(x ⇒ Real(x.getCreditsToSubtract)).sum
    val newTotalReal = _oldTotalReal - sumOfRealToSubtract

    val eventsForWallet = new ju.ArrayList[ResourceEventMsg](resourceInstanceChargingState.getPreviousEvents)
    eventsForWallet.add(0, resourceEvent)
    import scala.collection.JavaConverters.seqAsJavaListConverter
    val newWalletEntry = MessageFactory.newWalletEntryMsg(
      userID,
      Real.toMsgField(sumOfRealToSubtract),
      Real.toMsgField(_oldTotalReal),
      Real.toMsgField(newTotalReal),
      TimeHelpers.nowMillis(),
      referenceStartMillis,
      referenceStopMillis,
      billingMonthInfo.year,
      billingMonthInfo.month,
      billingMonthInfo.day,
      fullChargeslots.asJava,
      eventsForWallet,
      resourceType,
      resourceEvent.getIsSynthetic
    )

    logger.debug("newWalletEntry = {}", AvroHelpers.jsonStringOfSpecificRecord(newWalletEntry))

    walletEntryRecorder.apply(newWalletEntry)

    (1, sumOfRealToSubtract)
  }


  def selectEffectivePriceTableModel(
      fullPriceTable: FullPriceTableModel,
      chargingBehaviorDetails: DetailsModel.Type,
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg,
      currentResourceEvent: ResourceEventMsg,
      referenceStartMillis: Long,
      referenceStopMillis: Long,
      totalCredits: Real
  ): EffectivePriceTableModel = {

    val selectorPath = computeSelectorPath(
      chargingBehaviorDetails,
      resourceInstanceChargingState,
      currentResourceEvent,
      referenceStartMillis,
      referenceStopMillis,
      totalCredits
    )

    fullPriceTable.effectivePriceTableOfSelectorForResource(
      selectorPath,
      currentResourceEvent.getResource,
      logger
    )
  }

  final protected def constructDummyFirstEventFor(
      actualFirst: ResourceEventMsg,
      newOccurredMillis: Long,
      value: String
  ): ResourceEventMsg = {

    val dm = DetailsModel.make
    DetailsModel.setBoolean(dm, MessageConstants.DetailsKeys.aquarium_is_synthetic)
    DetailsModel.setBoolean(dm, MessageConstants.DetailsKeys.aquarium_is_dummy_first)
    DetailsModel.setString(dm, MessageConstants.DetailsKeys.aquarium_reference_event_id, actualFirst.getOriginalID)
    DetailsModel.setString(dm, MessageConstants.DetailsKeys.aquarium_reference_event_id_in_store, actualFirst.getInStoreID)

    ResourceEventMsg.newBuilder(actualFirst).
      setDetails(dm).
      setValue(value).
      setOccurredMillis(newOccurredMillis).
      setReceivedMillis(newOccurredMillis).
    build
  }
}
