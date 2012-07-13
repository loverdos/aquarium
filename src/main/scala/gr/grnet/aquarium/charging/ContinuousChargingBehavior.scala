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

/**
 * In practice a resource usage will be charged for the total amount of usage
 * between resource usage changes.
 *
 * Example resource that might be adept to a continuous policy
 * is diskspace.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
final class ContinuousChargingBehavior
    extends ChargingBehavior(
      ChargingBehaviorAliases.continuous,
      Set(ChargingBehaviorNameInput, UnitPriceInput, OldTotalAmountInput, TimeDeltaInput)) {

  def computeNewAccumulatingAmount(oldAmount: Double, newEventValue: Double, details: Map[String, String]): Double = {
    // If the total is in the details, get it, or else compute it
    details.get("total") match {
      case Some(total) ⇒
        total.toDouble

      case _ ⇒
        oldAmount + newEventValue
    }
  }

  def getResourceInstanceInitialAmount: Double = {
    0.0
  }

  /**
   * This is called when we have the very first event for a particular resource instance, and we want to know
   * if it is billable or not.
   */
  def isBillableFirstEvent(event: ResourceEventModel) = {
    true
  }

  def mustGenerateDummyFirstEvent = true

  def supportsImplicitEvents = {
    true
  }

  def mustConstructImplicitEndEventFor(resourceEvent: ResourceEventModel) = {
    true
  }

  def constructImplicitEndEventFor(resourceEvent: ResourceEventModel, newOccurredMillis: Long) = {
    assert(supportsImplicitEvents && mustConstructImplicitEndEventFor(resourceEvent))

    val details = resourceEvent.details
    val newDetails = ResourceEventModel.setAquariumSyntheticAndImplicitEnd(details)

    resourceEvent.withDetails(newDetails, newOccurredMillis)
  }
}

object ContinuousChargingBehavior {
  private[this] final val TheOne = new ContinuousChargingBehavior

  def apply(): ContinuousChargingBehavior = TheOne
}
