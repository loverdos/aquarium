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

import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import gr.grnet.aquarium.AquariumInternalError

/**
 * An discrete charging behavior indicates that a resource should be charged directly
 * at each resource state change, i.e. the charging is not dependent on
 * the time the resource.
 *
 * Example oneoff resources might be individual charges applied to various
 * actions (e.g. the fact that a user has created an account) or resources
 * that should be charged per volume once (e.g. the allocation of a volume)
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
final class DiscreteChargingBehavior
    extends ChargingBehavior(
      ChargingBehaviorAliases.discrete,
      Set(ChargingBehaviorNameInput, UnitPriceInput, CurrentValueInput)) {

  def computeNewAccumulatingAmount(oldAmount: Double, newEventValue: Double, details: Map[String, String]): Double = {
    oldAmount + newEventValue
  }

  def getResourceInstanceInitialAmount: Double = {
    0.0
  }

  /**
   * This is called when we have the very first event for a particular resource instance, and we want to know
   * if it is billable or not.
   */
  def isBillableFirstEvent(event: ResourceEventModel) = {
    false // nope, we definitely need a  previous one.
  }

  // FIXME: Check semantics of this. I just put false until thorough study
  def mustGenerateDummyFirstEvent = false

  def supportsImplicitEvents = {
    false
  }

  def mustConstructImplicitEndEventFor(resourceEvent: ResourceEventModel) = {
    false
  }

  def constructImplicitEndEventFor(resourceEvent: ResourceEventModel, occurredMillis: Long) = {
    throw new AquariumInternalError("constructImplicitEndEventFor() Not compliant with %s".format(this))
  }
}

object DiscreteChargingBehavior {
  private[this] final val TheOne = new DiscreteChargingBehavior

  def apply(): DiscreteChargingBehavior = TheOne
}
