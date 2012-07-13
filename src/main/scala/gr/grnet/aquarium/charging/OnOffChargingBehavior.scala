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

/**
 * An onoff charging behavior expects a resource to be in one of the two allowed
 * states (`on` and `off`, respectively). It will charge for resource usage
 * within the timeframes specified by consecutive on and off resource events.
 * An onoff policy is the same as a continuous policy, except for
 * the timeframes within the resource is in the `off` state.
 *
 * Example resources that might be adept to onoff policies are VMs in a
 * cloud application and books in a book lending application.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
final class OnOffChargingBehavior
    extends ChargingBehavior(
      ChargingBehaviorAliases.onoff,
      Set(ChargingBehaviorNameInput, UnitPriceInput, TimeDeltaInput)) {

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

  private[this]
  def getValueForCreditCalculation(oldAmount: Double, newEventValue: Double): Double = {
    import OnOffChargingBehaviorValues.{ON, OFF}

    def exception(rs: OnOffPolicyResourceState) =
      new AquariumException("Resource state transition error (%s -> %s)".format(rs, rs))

    (oldAmount, newEventValue) match {
      case (ON, ON) ⇒
        throw exception(OnResourceState)
      case (ON, OFF) ⇒
        OFF
      case (OFF, ON) ⇒
        ON
      case (OFF, OFF) ⇒
        throw exception(OffResourceState)
    }
  }

  override def isBillableEvent(event: ResourceEventModel) = {
    // ON events do not contribute, only OFF ones.
    OnOffChargingBehaviorValues.isOFFValue(event.value)
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
    OnOffChargingBehaviorValues.isONValue(resourceEvent.value)
  }

  def constructImplicitEndEventFor(resourceEvent: ResourceEventModel, newOccurredMillis: Long) = {
    assert(supportsImplicitEvents && mustConstructImplicitEndEventFor(resourceEvent))
    assert(OnOffChargingBehaviorValues.isONValue(resourceEvent.value))

    val details = resourceEvent.details
    val newDetails = ResourceEventModel.setAquariumSyntheticAndImplicitEnd(details)
    val newValue   = OnOffChargingBehaviorValues.OFF

    resourceEvent.withDetailsAndValue(newDetails, newValue, newOccurredMillis)
  }

  def constructImplicitStartEventFor(resourceEvent: ResourceEventModel) = {
    throw new AquariumInternalError("constructImplicitStartEventFor() Not compliant with %s".format(this))
  }
}

object OnOffChargingBehavior {
  private[this] final val TheOne = new OnOffChargingBehavior

  def apply(): OnOffChargingBehavior = TheOne
}
