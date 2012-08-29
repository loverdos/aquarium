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
import gr.grnet.aquarium.event.model.resource.{StdResourceEvent, ResourceEventModel}
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import VMChargingBehavior.Selectors.Power
import VMChargingBehavior.SelectorLabels.PowerStatus
import gr.grnet.aquarium.policy.ResourceType
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.charging.state.{WorkingResourceInstanceChargingState, WorkingResourcesChargingState, AgreementHistoryModel}
import gr.grnet.aquarium.charging.wallet.WalletEntry
import scala.collection.mutable
import gr.grnet.aquarium.event.model.EventModel
import gr.grnet.aquarium.util.LogHelpers._
import scala.Some
import gr.grnet.aquarium.policy.ResourceType

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
    (currentResourceEvent.value.toInt,workingResourceInstanceChargingState.previousEvents) match {
      case (1,Nil) => // create --> on
        //List(Power.create)
        Nil
      case (x,hd::_) =>  (x,hd.value.toInt) match {
        case (1,0) => // off ---> on
          List(Power.powerOff)
        case (0,1) => // on ---> off
          List(Power.powerOn)
        case (2,1) => // off --> destroy
          //List(Power.powerOff,Power.destroy)
          Nil
        case _ =>
          throw new Exception("Invalid state")
      }
      case _ =>
        throw new Exception("Invalid state")
    }
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
      resourceEvent: ResourceEventModel,
      resourceType: ResourceType,
      billingMonthInfo: BillingMonthInfo,
      workingResourcesChargingState: WorkingResourcesChargingState,
      userAgreements: AgreementHistoryModel,
      totalCredits: Double,
      walletEntryRecorder: WalletEntry ⇒ Unit
  ): (Int, Double) = {

    // 1. Ensure proper initial state per resource and per instance
    ensureInitializedWorkingState(workingResourcesChargingState,resourceEvent)

    // 2. Fill in data from the new event
    val stateOfResourceInstance = workingResourcesChargingState.stateOfResourceInstance
    val workingResourcesChargingStateDetails = workingResourcesChargingState.details
    val instanceID = resourceEvent.instanceID
    val workingResourceInstanceChargingState = stateOfResourceInstance(instanceID)
    fillWorkingResourceInstanceChargingStateFromEvent(workingResourceInstanceChargingState, resourceEvent)

    val retVal = workingResourceInstanceChargingState.previousEvents.headOption match {
      case Some(previousEvent) ⇒
        Debug(logger, "I have previous event %s", previousEvent.toDebugString)
        computeWalletEntriesForNewEvent(
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
      case None ⇒
        (0,0.0D)
    }
    // We need just one previous event, so we update it
    workingResourceInstanceChargingState.setOnePreviousEvent(resourceEvent)
    retVal
  }

  def createVirtualEventsForRealtimeComputation(
      userID: String,
      resourceTypeName: String,
      resourceInstanceID: String,
      eventOccurredMillis: Long,
      workingResourceInstanceChargingState: WorkingResourceInstanceChargingState
  ): List[ResourceEventModel] = {
    //
    def vmEvent(value:Long) : List[ResourceEventModel] =
      StdResourceEvent(
        ChargingBehavior.VirtualEventsIDGen.nextUID(),
        eventOccurredMillis,
        eventOccurredMillis,
        userID,
        "aquarium",
        resourceTypeName,
        resourceInstanceID,
        value.toDouble,
        EventModel.EventVersion_1_0,
        Map(
          ResourceEventModel.Names.details_aquarium_is_synthetic   -> "true",
          ResourceEventModel.Names.details_aquarium_is_realtime_virtual -> "true"
        )
      ) :: Nil
    //
    workingResourceInstanceChargingState.previousEvents.headOption match {
      case None =>  Nil
      case Some(hd) => hd.value.toInt match {
       case 0 =>  vmEvent(1) //produce an on event
       case 1 =>  vmEvent(0) //produce an off event
       case 2 =>  vmEvent(0) //produce an off event
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
