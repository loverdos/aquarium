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
import gr.grnet.aquarium.message.avro.gen.{WalletEntryMsg, ResourcesChargingStateMsg, ResourceTypeMsg, ResourceInstanceChargingStateMsg, ResourceEventMsg}
import gr.grnet.aquarium.policy.FullPriceTableModel
import gr.grnet.aquarium.message.MessageConstants

/**
 * A charging behavior for which resource events just carry a credit amount that will be added to the total one.
 *
 * Examples are: a) Give a gift of X credits to the user, b) User bought a book, so charge for the book price.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
final class OnceChargingBehavior extends ChargingBehaviorSkeleton(Nil) {
  def computeCreditsToSubtract(
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg,
      oldCredits: CreditsModel.Type,
      timeDeltaMillis: Long,
      unitPrice: CreditsModel.Type
  ): (CreditsModel.Type, String /* explanation */) = {

    val currentValue = CreditsModel.from(resourceInstanceChargingState.getCurrentValue)
    // Always remember to multiply with the `unitPrice`, since it scales the credits, depending on
    // the particular resource type tha applies.
    val credits = CreditsModel.mul(currentValue, unitPrice)
    val explanation = "Value(%s) * UnitPrice(%s)".format(currentValue, unitPrice)

    (credits, explanation)
  }

  def computeSelectorPath(
      chargingBehaviorDetails: DetailsModel.Type,
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg,
      currentResourceEvent: ResourceEventMsg,
      referenceFromMillis: Long,
      referenceToMillis: Long,
      totalCredits: CreditsModel.Type
  ): List[String] = {
    List(MessageConstants.DefaultSelectorKey)
  }

  override def processResourceEvent(
      aquarium: Aquarium,
      resourceEvent: ResourceEventMsg,
      resourceType: ResourceTypeMsg,
      billingMonthInfo: BillingMonthInfo,
      resourcesChargingState: ResourcesChargingStateMsg,
      userStateModel: UserStateModel,
      walletEntryRecorder: WalletEntryMsg ⇒ Unit
  ): (Int, Double) = {
    // The credits are given in the value
    // But we cannot just apply them, since we also need to take into account the unit price.
    // Normally, the unit price is 1.0 but we have the flexibility to allow more stuff).

    // 1. Ensure proper initial state per resource and per instance
    ensureInitializedWorkingState(resourcesChargingState,resourceEvent)

    // 2. Fill in data from the new event
    val stateOfResourceInstance = resourcesChargingState.getStateOfResourceInstance
    val resourcesChargingStateDetails = resourcesChargingState.getDetails
    val instanceID = resourceEvent.getInstanceID
    val resourceInstanceChargingState = stateOfResourceInstance.get(instanceID)
    fillWorkingResourceInstanceChargingStateFromEvent(resourceInstanceChargingState, resourceEvent)

    val userAgreementHistoryModel = userStateModel.userAgreementHistoryModel

    computeWalletEntriesForNewEvent(
      resourceEvent,
      resourceType,
      billingMonthInfo,
      userStateModel.totalCredits,
      resourceEvent.getOccurredMillis,
      resourceEvent.getOccurredMillis + 1, // single point in time
      userAgreementHistoryModel.agreementByTimeslot,
      resourcesChargingStateDetails,
      resourceInstanceChargingState,
      aquarium,
      walletEntryRecorder
    )
  }

  def initialChargingDetails = {
    DetailsModel.make
  }

  def computeNewAccumulatingAmount(
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg,
      eventDetails: DetailsModel.Type
  ): CreditsModel.Type = {
    CreditsModel.from(resourceInstanceChargingState.getOldAccumulatingAmount)
  }

  def createVirtualEventsForRealtimeComputation(
      userID: String,
      resourceTypeName: String,
      resourceInstanceID: String,
      eventOccurredMillis: Long,
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg
  ): List[ResourceEventMsg] = {

    // We optimize and generate no virtual event
    Nil
  }
}
