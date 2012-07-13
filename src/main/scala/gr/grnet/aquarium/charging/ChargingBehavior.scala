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
import gr.grnet.aquarium.{AquariumInternalError, AquariumException}

/**
 * A charging behavior indicates how charging for a resource will be done
 * wrt the various states a resource can be.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

abstract class ChargingBehavior(val alias: String, val inputs: Set[ChargingInput]) {

  final lazy val inputNames = inputs.map(_.name)

  /**
   * Generate a map where the key is a [[gr.grnet.aquarium.charging.ChargingInput]]
   * and the value the respective value. This map will be used to do the actual credit charge calculation
   * by the respective algorithm.
   *
   * Values are obtained from a corresponding context, which is provided by the parameters. We assume that this context
   * has been validated before the call to `makeValueMap` is made.
   *
   * @param totalCredits   the value for [[gr.grnet.aquarium.charging.TotalCreditsInput.]]
   * @param oldTotalAmount the value for [[gr.grnet.aquarium.charging.OldTotalAmountInput]]
   * @param newTotalAmount the value for [[gr.grnet.aquarium.charging.NewTotalAmountInput]]
   * @param timeDelta      the value for [[gr.grnet.aquarium.charging.TimeDeltaInput]]
   * @param previousValue  the value for [[gr.grnet.aquarium.charging.PreviousValueInput]]
   * @param currentValue   the value for [[gr.grnet.aquarium.charging.CurrentValueInput]]
   * @param unitPrice      the value for [[gr.grnet.aquarium.charging.UnitPriceInput]]
   *
   * @return a map from [[gr.grnet.aquarium.charging.ChargingInput]]s to respective values.
   */
  def makeValueMap(
      totalCredits: Double,
      oldTotalAmount: Double,
      newTotalAmount: Double,
      timeDelta: Double,
      previousValue: Double,
      currentValue: Double,
      unitPrice: Double
  ): Map[ChargingInput, Any] = {

    ChargingBehavior.makeValueMapFor(
      this,
      totalCredits,
      oldTotalAmount,
      newTotalAmount,
      timeDelta,
      previousValue,
      currentValue,
      unitPrice)
  }

  def isNamed(aName: String): Boolean = aName == alias

  def needsPreviousEventForCreditAndAmountCalculation: Boolean = {
    // If we need any variable that is related to the previous event
    // then we do need a previous event
    inputs.exists(_.isDirectlyRelatedToPreviousEvent)
  }

  /**
   * Given the old amount of a resource instance
   * (see [[gr.grnet.aquarium.computation.state.parts.ResourceInstanceSnapshot]]), the
   * value arriving in a new resource event and the new details, compute the new instance amount.
   *
   * Note that the `oldAmount` does not make sense for all types of [[gr.grnet.aquarium.charging.ChargingBehavior]],
   * in which case it is ignored.
   *
   * @param oldAmount     the old accumulating amount
   * @param newEventValue the value contained in a newly arrived
   *                      [[gr.grnet.aquarium.event.model.resource.ResourceEventModel]]
   * @param details       the `details` of the newly arrived
   *                      [[gr.grnet.aquarium.event.model.resource.ResourceEventModel]]
   * @return
   */
  def computeNewAccumulatingAmount(oldAmount: Double, newEventValue: Double, details: Map[String, String]): Double

  /**
   * The initial amount.
   */
  def getResourceInstanceInitialAmount: Double

  /**
   * The amount used when no amount is meant to be relevant.
   *
   * For example, when there is no need for a previous event but an API requires the amount of the previous event.
   *
   * Normally, this value will never be used by client code (= charge computation code).
   */
  def getResourceInstanceUndefinedAmount: Double = -1.0

  /**
   * An event carries enough info to characterize it as billable or not.
   *
   * Typically all events are billable by default and indeed this is the default implementation
   * provided here.
   *
   * The only exception to the rule is ON events for [[gr.grnet.aquarium.charging.OnOffChargingBehavior]].
   */
  def isBillableEvent(event: ResourceEventModel): Boolean = false

  /**
   * This is called when we have the very first event for a particular resource instance, and we want to know
   * if it is billable or not.
   */
  def isBillableFirstEvent(event: ResourceEventModel): Boolean

  def mustGenerateDummyFirstEvent: Boolean

  def dummyFirstEventValue: Double = 0.0

  def constructDummyFirstEventFor(actualFirst: ResourceEventModel, newOccurredMillis: Long): ResourceEventModel = {
    if(!mustGenerateDummyFirstEvent) {
      throw new AquariumException("constructDummyFirstEventFor() Not compliant with %s".format(this))
    }

    val newDetails = Map(
      ResourceEventModel.Names.details_aquarium_is_synthetic   -> "true",
      ResourceEventModel.Names.details_aquarium_is_dummy_first -> "true",
      ResourceEventModel.Names.details_aquarium_reference_event_id -> actualFirst.id,
      ResourceEventModel.Names.details_aquarium_reference_event_id_in_store -> actualFirst.stringIDInStoreOrEmpty
    )

    actualFirst.withDetailsAndValue(newDetails, dummyFirstEventValue, newOccurredMillis)
  }

  /**
   * There are resources (cost policies) for which implicit events must be generated at the end of the billing period
   * and also at the beginning of the next one. For these cases, this method must return `true`.
   *
   * The motivating example comes from the [[gr.grnet.aquarium.charging.OnOffChargingBehavior]] for which we
   * must implicitly assume `OFF` events at the end of the billing period and `ON` events at the beginning of the next
   * one.
   *
   */
  def supportsImplicitEvents: Boolean

  def mustConstructImplicitEndEventFor(resourceEvent: ResourceEventModel): Boolean

  @throws(classOf[Exception])
  def constructImplicitEndEventFor(resourceEvent: ResourceEventModel, newOccurredMillis: Long): ResourceEventModel
}

object ChargingBehavior {
  def makeValueMapFor(
      chargingBehavior: ChargingBehavior,
      totalCredits: Double,
      oldTotalAmount: Double,
      newTotalAmount: Double,
      timeDelta: Double,
      previousValue: Double,
      currentValue: Double,
      unitPrice: Double
  ): Map[ChargingInput, Any] = {
    
    val inputs = chargingBehavior.inputs
    var map = Map[ChargingInput, Any]()

    if(inputs contains ChargingBehaviorNameInput) map += ChargingBehaviorNameInput -> chargingBehavior.alias
    if(inputs contains TotalCreditsInput  ) map += TotalCreditsInput   -> totalCredits
    if(inputs contains OldTotalAmountInput) map += OldTotalAmountInput -> oldTotalAmount
    if(inputs contains NewTotalAmountInput) map += NewTotalAmountInput -> newTotalAmount
    if(inputs contains TimeDeltaInput     ) map += TimeDeltaInput      -> timeDelta
    if(inputs contains PreviousValueInput ) map += PreviousValueInput  -> previousValue
    if(inputs contains CurrentValueInput  ) map += CurrentValueInput   -> currentValue
    if(inputs contains UnitPriceInput     ) map += UnitPriceInput      -> unitPrice

    map
  }
}
