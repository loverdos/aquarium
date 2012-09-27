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

import gr.grnet.aquarium.charging.state.UserAgreementHistoryModel
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.event.DetailsModel
import gr.grnet.aquarium.message.MessageConstants
import gr.grnet.aquarium.message.avro.gen.{UserStateMsg, WalletEntryMsg, ResourcesChargingStateMsg, ResourceTypeMsg, ResourceInstanceChargingStateMsg, ResourceEventMsg}
import gr.grnet.aquarium.message.avro.{MessageHelpers, AvroHelpers, MessageFactory}
import gr.grnet.aquarium.util.LogHelpers.Debug
import gr.grnet.aquarium.{AquariumInternalError, Aquarium}
import gr.grnet.aquarium.Real
import gr.grnet.aquarium.HrsOfMillis
import gr.grnet.aquarium.MBsOfBytes

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
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg,
      oldCredits: Real,
      timeDeltaMillis: Long,
      unitPrice: Real
  ): (Real /* credits */, String /* explanation */) = {

    val bytes = Real(resourceInstanceChargingState.getOldAccumulatingAmount)
    val MBs = MBsOfBytes(bytes)
    val Hrs = HrsOfMillis(timeDeltaMillis)

    val credits = Hrs * MBs * unitPrice
    val explanation = "Hours(%s) * MBs(%s) * UnitPrice(%s)".format(
      Hrs,
      MBs,
      unitPrice
    )

    (credits, explanation)
  }

  def computeSelectorPath(
      chargingBehaviorDetails: DetailsModel.Type,
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg,
      currentResourceEvent: ResourceEventMsg,
      referenceStartMillis: Long,
      referenceStopMillis: Long,
      totalCredits: Real
  ): List[String] = {
    List(MessageConstants.DefaultSelectorKey)
  }

  def initialChargingDetails = {
    DetailsModel.make
  }

  def computeNewAccumulatingAmount(
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg,
      eventDetails: DetailsModel.Type
  ) = {

    val oldAccumulatingAmount = Real(resourceInstanceChargingState.getOldAccumulatingAmount)
    val currentValue = Real(resourceInstanceChargingState.getCurrentValue)

    oldAccumulatingAmount + currentValue
  }

  def constructImplicitEndEventFor(resourceEvent: ResourceEventMsg, newOccurredMillis: Long) = {
    val details = resourceEvent.getDetails
    val newDetails = DetailsModel.copyOf(details)
    MessageHelpers.setAquariumSyntheticAndImplicitEnd(newDetails)

    // FIXME: What value ?
    ResourceEventMsg.newBuilder(resourceEvent).
      setDetails(newDetails).
      setOccurredMillis(newOccurredMillis).
      setReceivedMillis(newOccurredMillis).
      build()
  }

  def processResourceEvent(
       aquarium: Aquarium,
       resourceEvent: ResourceEventMsg,
       resourceType: ResourceTypeMsg,
       billingMonthInfo: BillingMonthInfo,
       resourcesChargingState: ResourcesChargingStateMsg,
       userAgreementHistoryModel: UserAgreementHistoryModel,
       userStateMsg: UserStateMsg,
       walletEntryRecorder: WalletEntryMsg ⇒ Unit
   ): (Int, Real) = {

    // 1. Ensure proper initial state per resource and per instance
    ensureInitializedWorkingState(resourcesChargingState, resourceEvent)

    // 2. Fill in data from the new event
    val stateOfResourceInstance = resourcesChargingState.getStateOfResourceInstance
    val resourcesChargingStateDetails = resourcesChargingState.getDetails
    val instanceID = resourceEvent.getInstanceID
    val resourceInstanceChargingState = stateOfResourceInstance.get(instanceID)

    fillWorkingResourceInstanceChargingStateFromEvent(resourceInstanceChargingState, resourceEvent)

    val previousEvents = resourceInstanceChargingState.getPreviousEvents
    val previousEvent = previousEvents.size() match {
      case 0 ⇒
        // We do not have the needed previous event, so this must be the first resource event of its kind, ever.
        // Let's see if we can create a dummy previous event.
        Debug(logger, "First event of its kind %s", AvroHelpers.jsonStringOfSpecificRecord(resourceEvent))

        val dummyFirstEventValue = "0.0" // TODO ? From configuration

        val millis = userAgreementHistoryModel.agreementByTimeslot.headOption match {
          case None =>
            throw new AquariumInternalError("No agreement!!!") // FIXME Better explanation
          case Some((_,aggr)) =>
            val millisAgg = aggr.timeslot.from.getTime
            val millisMon = billingMonthInfo.monthStartMillis
            if(millisAgg>millisMon) millisAgg else millisMon
        }

        val dummyFirstEvent = constructDummyFirstEventFor(resourceEvent, millis, dummyFirstEventValue)

        Debug(logger, "Dummy first event %s", AvroHelpers.jsonStringOfSpecificRecord(dummyFirstEvent))
        dummyFirstEvent


      case _ ⇒
        val previousEvent = previousEvents.get(0) // head is most recent
        Debug(logger, "I have previous event %s", AvroHelpers.jsonStringOfSpecificRecord(previousEvent))
        previousEvent

    }

    val retval = computeWalletEntriesForNewEvent(
      resourceEvent,
      resourceType,
      billingMonthInfo,
      Real(userStateMsg.getTotalCredits),
      previousEvent.getOccurredMillis,
      resourceEvent.getOccurredMillis,
      userAgreementHistoryModel.agreementByTimeslot,
      resourcesChargingStateDetails,
      resourceInstanceChargingState,
      aquarium,
      walletEntryRecorder
    )

    // We need just one previous event, so we update it
    MessageHelpers.setOnePreviousEvent(resourceInstanceChargingState, resourceEvent)
    assert(resourceInstanceChargingState.getPreviousEvents.size() == 1)

    retval
  }

  def createVirtualEventsForRealtimeComputation(
      userID: String,
      resourceTypeName: String,
      resourceInstanceID: String,
      eventOccurredMillis: Long,
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg
  ): List[ResourceEventMsg] = {
    // FIXME This is too adhoc...
    val path = resourceInstanceChargingState.getPreviousEvents.size() match {
      case 0 ⇒
        "unknown" // FIXME This should not happen. Throw?

      case _ ⇒
        val previousEvent = resourceInstanceChargingState.getPreviousEvents.get(0)
        previousEvent.getDetails.get(MessageConstants.DetailsKeys.path) match {
          case null ⇒
            "unknown" // FIXME This should not happen. Throw?

          case path ⇒
            MessageHelpers.stringOfAnyValueMsg(path)
        }
    }

    // FIXME This is too adhoc...
    val action = resourceInstanceChargingState.getPreviousEvents.size() match {
      case 0 ⇒
        "unknown" // FIXME This should not happen. Throw?

      case _ ⇒
        val previousEvent = resourceInstanceChargingState.getPreviousEvents.get(0)
        previousEvent.getDetails.get(MessageConstants.DetailsKeys.action) match {
          case null ⇒
            "update"

          case action ⇒
            MessageHelpers.stringOfAnyValueMsg(action)
        }
    }

    MessageFactory.newResourceEventMsg(
      MessageHelpers.VirtualEventsIDGen.nextUID(),
      eventOccurredMillis,
      eventOccurredMillis,
      userID,
      "aquarium",
      resourceTypeName,
      resourceInstanceID,
      "0.0",
      MessageConstants.EventVersion_1_0,
      MessageFactory.newDetails(
        MessageFactory.newBooleanDetail(MessageConstants.DetailsKeys.aquarium_is_synthetic, true),
        MessageFactory.newBooleanDetail(MessageConstants.DetailsKeys.aquarium_is_realtime_virtual, true),
        MessageFactory.newStringDetail(MessageConstants.DetailsKeys.path, path),
        MessageFactory.newStringDetail(MessageConstants.DetailsKeys.action, action)
      )
    ) :: Nil
  }
}

object ContinuousChargingBehavior {
  private[this] final val TheOne = new ContinuousChargingBehavior

  def apply(): ContinuousChargingBehavior = TheOne
}
