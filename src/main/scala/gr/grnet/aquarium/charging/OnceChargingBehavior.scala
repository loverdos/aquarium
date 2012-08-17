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
import gr.grnet.aquarium.AquariumException
import scala.collection.mutable
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.policy.FullPriceTable

/**
 * A charging behavior for which resource events just carry a credit amount that will be added to the total one.
 *
 * Examples are: a) Give a gift of X credits to the user, b) User bought a book, so charge for the book price.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
final class OnceChargingBehavior
    extends ChargingBehaviorSkeleton(
      ChargingBehaviorAliases.once,
      Set(ChargingBehaviorNameInput, CurrentValueInput)) {

  protected def computeSelectorPath(
      chargingData: mutable.Map[String, Any],
      currentResourceEvent: ResourceEventModel,
      referenceTimeslot: Timeslot,
      previousValue: Double,
      totalCredits: Double,
      oldAccumulatingAmount: Double,
      newAccumulatingAmount: Double
  ): List[String] = {
    List(FullPriceTable.DefaultSelectorKey)
  }
  /**
   * This is called when we have the very first event for a particular resource instance, and we want to know
   * if it is billable or not.
   */
  def isBillableFirstEvent(event: ResourceEventModel) = {
    true
  }

  def mustGenerateDummyFirstEvent = false // no need to

  def computeNewAccumulatingAmount(oldAmount: Double, newEventValue: Double, details: Map[String, String]) = {
    oldAmount
  }

  def getResourceInstanceInitialAmount = 0.0

  def supportsImplicitEvents = false

  def mustConstructImplicitEndEventFor(resourceEvent: ResourceEventModel) = false

  def constructImplicitEndEventFor(resourceEvent: ResourceEventModel, occurredMillis: Long) = {
    throw new AquariumException("constructImplicitEndEventFor() Not compliant with %s".format(this))
  }
}

object OnceChargingBehavior {
  private[this] final val TheOne = new OnceChargingBehavior

  def apply(): OnceChargingBehavior = TheOne
}
