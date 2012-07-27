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

import gr.grnet.aquarium.{AquariumInternalError, AquariumException}
import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import scala.collection.mutable
import VMChargingBehavior.Selectors.Power

/**
 * The new [[gr.grnet.aquarium.charging.ChargingBehavior]] for VMs usage.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
final class VMChargingBehavior
    extends ChargingBehavior(
      ChargingBehaviorAliases.vmtime,
      Set(ChargingBehaviorNameInput, UnitPriceInput, TimeDeltaInput),
      List(List(Power.powerOn, Power.powerOff))) {

  protected def computeSelectorPath(
     chargingData: mutable.Map[String, Any],
     currentResourceEvent: ResourceEventModel,
     referenceTimeslot: Timeslot,
     previousValue: Double,
     totalCredits: Double,
     oldAccumulatingAmount: Double,
     newAccumulatingAmount: Double
 ): List[String] = {
    // FIXME
    List(Power.powerOn) // compute prices for power-on state
  }
  /**
   *
   * @param oldAmount is ignored
   * @param newEventValue
   * @return
   */
  def computeNewAccumulatingAmount(oldAmount: Double, newEventValue: Double, details: Map[String, String]): Double = {
    newEventValue
  }

  def getResourceInstanceInitialAmount: Double = {
    0.0
  }

  override def isBillableEvent(event: ResourceEventModel) = {
    // ON events do not contribute, only OFF ones.
    VMChargingBehaviorValues.isOFFValue(event.value)
  }

  /**
   * This is called when we have the very first event for a particular resource instance, and we want to know
   * if it is billable or not.
   */
  def isBillableFirstEvent(event: ResourceEventModel) = {
    false
  }

  def mustGenerateDummyFirstEvent = false // should be handled by the implicit OFFs

  def supportsImplicitEvents = {
    true
  }

  def mustConstructImplicitEndEventFor(resourceEvent: ResourceEventModel) = {
    // If we have ON events with no OFF companions at the end of the billing period,
    // then we must generate implicit OFF events.
    VMChargingBehaviorValues.isONValue(resourceEvent.value)
  }

  def constructImplicitEndEventFor(resourceEvent: ResourceEventModel, newOccurredMillis: Long) = {
    assert(supportsImplicitEvents && mustConstructImplicitEndEventFor(resourceEvent))
    assert(VMChargingBehaviorValues.isONValue(resourceEvent.value))

    val details = resourceEvent.details
    val newDetails = ResourceEventModel.setAquariumSyntheticAndImplicitEnd(details)
    val newValue   = VMChargingBehaviorValues.OFF

    resourceEvent.withDetailsAndValue(newDetails, newValue, newOccurredMillis)
  }

  def constructImplicitStartEventFor(resourceEvent: ResourceEventModel) = {
    throw new AquariumInternalError("constructImplicitStartEventFor() Not compliant with %s".format(this))
  }
}

object VMChargingBehavior {
  object Selectors {
    object Power {
      // When the VM is powered on
      final val powerOn = "powerOn"
      // When the VM is powered off
      final val powerOff = "powerOff"
    }
  }
}
