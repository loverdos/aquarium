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

abstract class ChargingBehavior(val name: String, val inputs: Set[ChargingInput]) {

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

  def isNamed(aName: String): Boolean = aName == name

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
  def apply(name: String): ChargingBehavior  = {
    name match {
      case null ⇒
        throw new AquariumException("<null> charging behavior")

      case name ⇒ name.toLowerCase match {
        case ChargingBehaviorNames.onoff      ⇒ OnOffChargingBehavior
        case ChargingBehaviorNames.discrete   ⇒ DiscreteChargingBehavior
        case ChargingBehaviorNames.continuous ⇒ ContinuousChargingBehavior
        case ChargingBehaviorNames.once       ⇒ ContinuousChargingBehavior

        case _ ⇒
          throw new AquariumException("Invalid charging behavior %s".format(name))
      }
    }
  }

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

    if(inputs contains ChargingBehaviorNameInput) map += ChargingBehaviorNameInput -> chargingBehavior.name
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

/**
 * A charging behavior for which resource events just carry a credit amount that will be added to the total one.
 *
 * Examples are: a) Give a gift of X credits to the user, b) User bought a book, so charge for the book price.
 *
 */
case object OnceChargingBehavior
extends ChargingBehavior(
    ChargingBehaviorNames.once,
    Set(ChargingBehaviorNameInput, CurrentValueInput)
) {

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

/**
 * In practice a resource usage will be charged for the total amount of usage
 * between resource usage changes.
 *
 * Example resource that might be adept to a continuous policy
 * is diskspace.
 */
case object ContinuousChargingBehavior
extends ChargingBehavior(
    ChargingBehaviorNames.continuous,
    Set(ChargingBehaviorNameInput, UnitPriceInput, OldTotalAmountInput, TimeDeltaInput)
) {

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

/**
 * An onoff charging behavior expects a resource to be in one of the two allowed
 * states (`on` and `off`, respectively). It will charge for resource usage
 * within the timeframes specified by consecutive on and off resource events.
 * An onoff policy is the same as a continuous policy, except for
 * the timeframes within the resource is in the `off` state.
 *
 * Example resources that might be adept to onoff policies are VMs in a
 * cloud application and books in a book lending application.
 */
case object OnOffChargingBehavior
extends ChargingBehavior(
    ChargingBehaviorNames.onoff,
    Set(ChargingBehaviorNameInput, UnitPriceInput, TimeDeltaInput)
) {

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

object OnOffChargingBehaviorValues {
  final val ON  = 1.0
  final val OFF = 0.0

  def isONValue (value: Double) = value == ON
  def isOFFValue(value: Double) = value == OFF
}

/**
 * An discrete charging behavior indicates that a resource should be charged directly
 * at each resource state change, i.e. the charging is not dependent on
 * the time the resource.
 *
 * Example oneoff resources might be individual charges applied to various
 * actions (e.g. the fact that a user has created an account) or resources
 * that should be charged per volume once (e.g. the allocation of a volume)
 */
case object DiscreteChargingBehavior
extends ChargingBehavior(
    ChargingBehaviorNames.discrete,
    Set(ChargingBehaviorNameInput, UnitPriceInput, CurrentValueInput)
) {

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
