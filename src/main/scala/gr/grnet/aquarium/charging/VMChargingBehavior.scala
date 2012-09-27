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

import VMChargingBehavior.SelectorLabels.PowerStatus
import VMChargingBehavior.Selectors.Power
import gr.grnet.aquarium.charging.state.UserAgreementHistoryModel
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.event.DetailsModel
import gr.grnet.aquarium.message.MessageConstants
import gr.grnet.aquarium.message.avro.gen.{UserStateMsg, WalletEntryMsg, ResourceTypeMsg, ResourcesChargingStateMsg, ResourceInstanceChargingStateMsg, ResourceEventMsg}
import gr.grnet.aquarium.message.avro.{AvroHelpers, MessageHelpers, MessageFactory}
import gr.grnet.aquarium.util.LogHelpers._
import gr.grnet.aquarium.{Aquarium, AquariumInternalError}
import scala.collection.JavaConverters.asScalaBufferConverter
import gr.grnet.aquarium.Real
import gr.grnet.aquarium.HrsOfMillis

/**
 * The new [[gr.grnet.aquarium.charging.ChargingBehavior]] for VMs usage.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
final class VMChargingBehavior extends ChargingBehaviorSkeleton(List(PowerStatus)) {
  def computeCreditsToSubtract(
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg,
      oldCredits: Real,
      timeDeltaMillis: Long,
      unitPrice: Real
  ): (Real, String /* explanation */) = {

    val credits = HrsOfMillis(timeDeltaMillis) * unitPrice
    val explanation = "Hours(%s) * UnitPrice(%s)".format(HrsOfMillis(timeDeltaMillis), unitPrice)

    (credits, explanation)

  }

  def computeSelectorPath(
      chargingBehaviorDetails: DetailsModel.Type,
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg,
      currentResourceEvent: ResourceEventMsg,
      referenceFromMillis: Long,
      referenceToMillis: Long,
      totalCredits: Real
  ): List[String] = {
    val previousEvents = resourceInstanceChargingState.getPreviousEvents.asScala.toList
    (currentResourceEvent.getValue.toInt,previousEvents) match {
      case (1,Nil) => // create --> on
        //List(Power.create)
        Nil
      case (x,hd::_) =>  (x,hd.getValue.toInt) match {
        case (1,0) => // off ---> on
          List(Power.powerOff)
        case (0,1) => // on ---> off
          List(Power.powerOn)
        case (2,1) => // off --> destroy
          //List(Power.powerOff,Power.destroy)
          Nil
        case _ =>
          throw new AquariumInternalError("Invalid state") // FIXME better message
      }
      case _ =>
        throw new AquariumInternalError("Invalid state") // FIXME better message
    }
  }

  def initialChargingDetails = {
    DetailsModel.make
  }

  def computeNewAccumulatingAmount(
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg,
      eventDetails: DetailsModel.Type
  ) = {
    Real(resourceInstanceChargingState.getCurrentValue)
  }

  def constructImplicitEndEventFor(resourceEvent: ResourceEventMsg, newOccurredMillis: Long) = {
    assert(VMChargingBehaviorValues.isONValue(resourceEvent.getValue))

    val details = resourceEvent.getDetails
    val newDetails = DetailsModel.copyOf(details)
    MessageHelpers.setAquariumSyntheticAndImplicitEnd(newDetails)

    ResourceEventMsg.newBuilder(resourceEvent).
      setDetails(newDetails).
      setOccurredMillis(newOccurredMillis).
      setReceivedMillis(newOccurredMillis).
      setValue(VMChargingBehaviorValues.OFF).
      build()
  }

  def constructImplicitStartEventFor(resourceEvent: ResourceEventMsg) = {
    throw new AquariumInternalError("constructImplicitStartEventFor() Not compliant with %s", this)
  }

  /**
   *
   * @return The number of wallet entries recorded and the new total credits
   */
  override def processResourceEvent(
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
    ensureInitializedWorkingState(resourcesChargingState,resourceEvent)

    // 2. Fill in data from the new event
    val stateOfResourceInstance = resourcesChargingState.getStateOfResourceInstance
    val resourcesChargingStateDetails = resourcesChargingState.getDetails
    val instanceID = resourceEvent.getInstanceID
    val resourceInstanceChargingState = stateOfResourceInstance.get(instanceID)
    fillWorkingResourceInstanceChargingStateFromEvent(resourceInstanceChargingState, resourceEvent)

    val previousEvents = resourceInstanceChargingState.getPreviousEvents
    val retVal = previousEvents.size() match {
      case 0 ⇒
        (0, Real.Zero)

      case _ ⇒
        val previousEvent = previousEvents.get(0) // head is most recent
        Debug(logger, "I have previous event %s", AvroHelpers.jsonStringOfSpecificRecord(previousEvent))

        computeWalletEntriesForNewEvent(
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
    }

    // We need just one previous event, so we update it
    MessageHelpers.setOnePreviousEvent(resourceInstanceChargingState, resourceEvent)
    retVal
  }

  def createVirtualEventsForRealtimeComputation(
      userID: String,
      resourceTypeName: String,
      resourceInstanceID: String,
      eventOccurredMillis: Long,
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg
  ): List[ResourceEventMsg] = {
    val resourceInstanceID = resourceInstanceChargingState.getInstanceID

    def vmEvent(value: String) : List[ResourceEventMsg] = {
      val dm = DetailsModel.make
      DetailsModel.setBoolean(dm, MessageConstants.DetailsKeys.aquarium_is_synthetic)
      DetailsModel.setBoolean(dm, MessageConstants.DetailsKeys.aquarium_is_realtime_virtual)

      MessageFactory.newResourceEventMsg(
        MessageHelpers.VirtualEventsIDGen.nextUID(),
        eventOccurredMillis,
        eventOccurredMillis,
        userID,
        "aquarium",
        resourceTypeName,
        resourceInstanceID,
        value,
        MessageConstants.EventVersion_1_0,
        dm
      ) :: Nil
    }

    def mkON  = vmEvent(VMChargingBehaviorValues.ON)
    def mkOFF = vmEvent(VMChargingBehaviorValues.OFF)

    val previousEvents = resourceInstanceChargingState.getPreviousEvents
    previousEvents.size() match {
      case 0 ⇒  Nil
      case _ ⇒

        previousEvents.get(0).getValue.toInt match {
       case 0 ⇒  mkON //produce an on event
       case 1 ⇒  mkOFF //produce an off event
       case 2 ⇒  mkOFF //produce an off event
      }
    }
  }
}

object VMChargingBehavior {
  object SelectorLabels {
    final val PowerStatus = "Power Status (ON/OFF)"
  }

  object Selectors {
    object Power {
      // When the VM is created
      //final val create = "create"

      // When the VM is destroyed
      //final val destroy = "destroy"

      // When the VM is powered on
      final val powerOn = "powerOn"

      // When the VM is powered off
      final val powerOff = "powerOff"
    }
  }
}
