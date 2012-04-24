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

package gr.grnet.aquarium.logic.accounting.dsl

import com.ckkloverdos.maybe.{NoVal, Failed, Just, Maybe}
import gr.grnet.aquarium.event.ResourceEvent
import gr.grnet.aquarium.AquariumException

/**
 * A cost policy indicates how charging for a resource will be done
 * wrt the various states a resource can be.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

abstract class DSLCostPolicy(val name: String, val vars: Set[DSLCostPolicyVar]) extends DSLItem {

  def varNames = vars.map(_.name)

  /**
   * Generate a map where the key is a [[gr.grnet.aquarium.logic.accounting.dsl.DSLCostPolicyVar]]
   * and the value the respective value. This map will be used to do the actual credit charge calculation
   * by the respective algorithm.
   *
   * Values are obtained from a corresponding context, which is provided by the parameters. We assume that this context
   * has been validated before the call to `makeValueMap` is made.
   *
   * @param totalCredits   the value for [[gr.grnet.aquarium.logic.accounting.dsl.DSLTotalCreditsVar]]
   * @param oldTotalAmount the value for [[gr.grnet.aquarium.logic.accounting.dsl.DSLOldTotalAmountVar]]
   * @param newTotalAmount the value for [[gr.grnet.aquarium.logic.accounting.dsl.DSLNewTotalAmountVar]]
   * @param timeDelta      the value for [[gr.grnet.aquarium.logic.accounting.dsl.DSLTimeDeltaVar]]
   * @param previousValue  the value for [[gr.grnet.aquarium.logic.accounting.dsl.DSLPreviousValueVar]]
   * @param currentValue   the value for [[gr.grnet.aquarium.logic.accounting.dsl.DSLCurrentValueVar]]
   * @param unitPrice      the value for [[gr.grnet.aquarium.logic.accounting.dsl.DSLUnitPriceVar]]
   *
   * @return a map from [[gr.grnet.aquarium.logic.accounting.dsl.DSLCostPolicyVar]]s to respective values.
   */
  def makeValueMap(totalCredits: Double,
                   oldTotalAmount: Double,
                   newTotalAmount: Double,
                   timeDelta: Double,
                   previousValue: Double,
                   currentValue: Double,
                   unitPrice: Double): Map[DSLCostPolicyVar, Any] = {

    DSLCostPolicy.makeValueMapFor(
      this,
      totalCredits,
      oldTotalAmount,
      newTotalAmount,
      timeDelta,
      previousValue,
      currentValue,
      unitPrice)
  }

  def isOnOff: Boolean = isNamed(DSLCostPolicyNames.onoff)

  def isContinuous: Boolean = isNamed(DSLCostPolicyNames.continuous)

  def isDiscrete: Boolean = isNamed(DSLCostPolicyNames.discrete)
  
  def isOnce: Boolean = isNamed(DSLCostPolicyNames.once)

  def isNamed(aName: String): Boolean = aName == name

  def needsPreviousEventForCreditAndAmountCalculation: Boolean = {
    // If we need any variable that is related to the previous event
    // then we do need a previous event
    vars.exists(_.isDirectlyRelatedToPreviousEvent)
  }

  /**
   * Given the old amount of a resource instance (see [[gr.grnet.aquarium.user.ResourceInstanceSnapshot]]) and the
   * value arriving in a new resource event, compute the new instance amount.
   *
   * Note that the `oldAmount` does not make sense for all types of [[gr.grnet.aquarium.logic.accounting.dsl.DSLCostPolicy]],
   * in which case it is ignored.
   *
   * @param oldAmount the old accumulating amount
   * @param newEventValue the value contained in a newly arrived [[gr.grnet.aquarium.event.ResourceEvent]]
   * @return
   */
  def computeNewAccumulatingAmount(oldAmount: Double, newEventValue: Double): Double

  def computeResourceInstanceAmountForNewBillingPeriod(oldAmount: Double): Double

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
   * Get the value that will be used in credit calculation in Accounting.chargeEvents
   */
  def getValueForCreditCalculation(oldAmountM: Maybe[Double], newEventValue: Double): Maybe[Double]

  /**
   * An event's value by itself should carry enough info to characterize it billable or not.
   *
   * Typically all events are billable by default and indeed this is the default implementation
   * provided here.
   *
   * The only exception to the rule is ON events for [[gr.grnet.aquarium.logic.accounting.dsl.OnOffCostPolicy]].
   */
  def isBillableEventBasedOnValue(eventValue: Double): Boolean = true

  /**
   * This is called when we have the very first event for a particular resource instance, and we want to know
   * if it is billable or not.
   *
   * @param eventValue
   * @return
   */
  def isBillableFirstEventBasedOnValue(eventValue: Double): Boolean
  
  /**
   * There are resources (cost policies) for which implicit events must be generated at the end of the billing period
   * and also at the beginning of the next one. For these cases, this method must return `true`.
   *
   * The motivating example comes from the [[gr.grnet.aquarium.logic.accounting.dsl.OnOffCostPolicy]] for which we
   * must implicitly assume `OFF` events at the end of the billing period and `ON` events at the beginning of the next
   * one.
   *
   */
  def supportsImplicitEvents: Boolean

  def mustConstructImplicitEndEventFor(resourceEvent: ResourceEvent): Boolean

  @throws(classOf[Exception])
  def constructImplicitEndEventFor(resourceEvent: ResourceEvent, newOccurredMillis: Long): ResourceEvent

  @throws(classOf[Exception])
  def constructImplicitStartEventFor(resourceEvent: ResourceEvent): ResourceEvent
}

object DSLCostPolicyNames {
  final val onoff      = "onoff"
  final val discrete   = "discrete"
  final val continuous = "continuous"
  final val once       = "once"
}

object DSLCostPolicy {
  def apply(name: String): DSLCostPolicy  = {
    name match {
      case null ⇒
        throw new DSLParseException("<null> cost policy")

      case name ⇒ name.toLowerCase match {
        case DSLCostPolicyNames.onoff      ⇒ OnOffCostPolicy
        case DSLCostPolicyNames.discrete   ⇒ DiscreteCostPolicy
        case DSLCostPolicyNames.continuous ⇒ ContinuousCostPolicy
        case DSLCostPolicyNames.once       ⇒ ContinuousCostPolicy

        case _ ⇒
          throw new DSLParseException("Invalid cost policy %s".format(name))
      }
    }
  }

  def makeValueMapFor(costPolicy: DSLCostPolicy,
                      totalCredits: Double,
                      oldTotalAmount: Double,
                      newTotalAmount: Double,
                      timeDelta: Double,
                      previousValue: Double,
                      currentValue: Double,
                      unitPrice: Double): Map[DSLCostPolicyVar, Any] = {
    val vars = costPolicy.vars
    var map = Map[DSLCostPolicyVar, Any]()

    if(vars contains DSLCostPolicyNameVar) map += DSLCostPolicyNameVar -> costPolicy.name
    if(vars contains DSLTotalCreditsVar  ) map += DSLTotalCreditsVar   -> totalCredits
    if(vars contains DSLOldTotalAmountVar) map += DSLOldTotalAmountVar -> oldTotalAmount
    if(vars contains DSLNewTotalAmountVar) map += DSLNewTotalAmountVar -> newTotalAmount
    if(vars contains DSLTimeDeltaVar     ) map += DSLTimeDeltaVar      -> timeDelta
    if(vars contains DSLPreviousValueVar ) map += DSLPreviousValueVar  -> previousValue
    if(vars contains DSLCurrentValueVar  ) map += DSLCurrentValueVar   -> currentValue
    if(vars contains DSLUnitPriceVar     ) map += DSLUnitPriceVar      -> unitPrice

    map
  }
}

/**
 * A cost policy for which resource events just carry a credit amount that will be added to the total one.
 *
 * Examples are: a) Give a gift of X credits to the user, b) User bought a book, so charge for the book price.
 *
 */
case object OnceCostPolicy
  extends DSLCostPolicy(DSLCostPolicyNames.once, Set(DSLCostPolicyNameVar, DSLCurrentValueVar)) {

  def isBillableFirstEventBasedOnValue(eventValue: Double) = true

  def computeNewAccumulatingAmount(oldAmount: Double, newEventValue: Double) = oldAmount

  def computeResourceInstanceAmountForNewBillingPeriod(oldAmount: Double) = getResourceInstanceInitialAmount

  def getResourceInstanceInitialAmount = 0.0

  def getValueForCreditCalculation(oldAmountM: Maybe[Double], newEventValue: Double) = Just(newEventValue)

  def supportsImplicitEvents = false

  def mustConstructImplicitEndEventFor(resourceEvent: ResourceEvent) = false

  def constructImplicitEndEventFor(resourceEvent: ResourceEvent, occurredMillis: Long) = {
    throw new AquariumException("constructImplicitEndEventFor() Not compliant with %s".format(this))
  }

  def constructImplicitStartEventFor(resourceEvent: ResourceEvent) = {
    throw new AquariumException("constructImplicitStartEventFor() Not compliant with %s".format(this))
  }
}

/**
 * In practice a resource usage will be charged for the total amount of usage
 * between resource usage changes.
 *
 * Example resource that might be adept to a continuous policy
 * is diskspace.
 */
case object ContinuousCostPolicy
  extends DSLCostPolicy(DSLCostPolicyNames.continuous,
                        Set(DSLCostPolicyNameVar, DSLUnitPriceVar, DSLOldTotalAmountVar, DSLTimeDeltaVar)) {

  def computeNewAccumulatingAmount(oldAmount: Double, newEventValue: Double): Double = {
    oldAmount + newEventValue
  }

  def computeResourceInstanceAmountForNewBillingPeriod(oldAmount: Double): Double = {
    oldAmount
  }

  def getResourceInstanceInitialAmount: Double = {
    0.0
  }

  def getValueForCreditCalculation(oldAmountM: Maybe[Double], newEventValue: Double): Maybe[Double] = {
    oldAmountM
  }

  def isBillableFirstEventBasedOnValue(eventValue: Double) = {
    false
  }

  def supportsImplicitEvents = {
    true
  }

  def mustConstructImplicitEndEventFor(resourceEvent: ResourceEvent) = {
    true
  }

  def constructImplicitEndEventFor(resourceEvent: ResourceEvent, newOccurredMillis: Long) = {
    assert(supportsImplicitEvents && mustConstructImplicitEndEventFor(resourceEvent))

    val details = resourceEvent.details
    val newDetails = ResourceEvent.setAquariumSyntheticAndImplicitEnd(details)
    val newValue   = resourceEvent.value

    resourceEvent.copy(
      occurredMillis = newOccurredMillis,
      details = newDetails,
      value = newValue
    )
  }

  def constructImplicitStartEventFor(resourceEvent: ResourceEvent) = {
    throw new AquariumException("constructImplicitStartEventFor() Not compliant with %s".format(this))
  }
}

/**
 * An onoff cost policy expects a resource to be in one of the two allowed
 * states (`on` and `off`, respectively). It will charge for resource usage
 * within the timeframes specified by consecutive on and off resource events.
 * An onoff policy is the same as a continuous policy, except for
 * the timeframes within the resource is in the `off` state.
 *
 * Example resources that might be adept to onoff policies are VMs in a
 * cloud application and books in a book lending application.
 */
case object OnOffCostPolicy
  extends DSLCostPolicy(DSLCostPolicyNames.onoff,
                        Set(DSLCostPolicyNameVar, DSLUnitPriceVar, DSLTimeDeltaVar)) {

  /**
   *
   * @param oldAmount is ignored
   * @param newEventValue
   * @return
   */
  def computeNewAccumulatingAmount(oldAmount: Double, newEventValue: Double): Double = {
    newEventValue
  }
  
  def computeResourceInstanceAmountForNewBillingPeriod(oldAmount: Double): Double = {
    import OnOffCostPolicyValues.{ON, OFF}
    oldAmount match {
      case ON  ⇒ /* implicit off at the end of the billing period */ OFF
      case OFF ⇒ OFF
    }
  }

  def getResourceInstanceInitialAmount: Double = {
    0.0
  }
  
  def getValueForCreditCalculation(oldAmountM: Maybe[Double], newEventValue: Double): Maybe[Double] = {
    oldAmountM match {
      case Just(oldAmount) ⇒
        Maybe(getValueForCreditCalculation(oldAmount, newEventValue))
      case NoVal ⇒
        Failed(new AquariumException("NoVal for oldValue instead of Just"))
      case Failed(e) ⇒
        Failed(new AquariumException("Failed for oldValue instead of Just", e))
    }
  }

  private[this]
  def getValueForCreditCalculation(oldAmount: Double, newEventValue: Double): Double = {
    import OnOffCostPolicyValues.{ON, OFF}

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

  override def isBillableEventBasedOnValue(eventValue: Double) = {
    // ON events do not contribute, only OFF ones.
    OnOffCostPolicyValues.isOFFValue(eventValue)
  }

  def isBillableFirstEventBasedOnValue(eventValue: Double) = {
    false
  }

  def supportsImplicitEvents = {
    true
  }


  def mustConstructImplicitEndEventFor(resourceEvent: ResourceEvent) = {
    // If we have ON events with no OFF companions at the end of the billing period,
    // then we must generate implicit OFF events.
    OnOffCostPolicyValues.isONValue(resourceEvent.value)
  }

  def constructImplicitEndEventFor(resourceEvent: ResourceEvent, newOccurredMillis: Long) = {
    assert(supportsImplicitEvents && mustConstructImplicitEndEventFor(resourceEvent))
    assert(OnOffCostPolicyValues.isONValue(resourceEvent.value))

    val details = resourceEvent.details
    val newDetails = ResourceEvent.setAquariumSyntheticAndImplicitEnd(details)
    val newValue   = OnOffCostPolicyValues.OFF

    resourceEvent.copy(
      occurredMillis = newOccurredMillis,
      details = newDetails,
      value = newValue
    )
  }

  def constructImplicitStartEventFor(resourceEvent: ResourceEvent) = {
    throw new AquariumException("constructImplicitStartEventFor() Not compliant with %s".format(this))
  }
}

object OnOffCostPolicyValues {
  final val ON  = 1.0
  final val OFF = 0.0

  def isONValue (value: Double) = value == ON
  def isOFFValue(value: Double) = value == OFF
}

/**
 * An discrete cost policy indicates that a resource should be charged directly
 * at each resource state change, i.e. the charging is not dependent on
 * the time the resource.
 *
 * Example oneoff resources might be individual charges applied to various
 * actions (e.g. the fact that a user has created an account) or resources
 * that should be charged per volume once (e.g. the allocation of a volume)
 */
case object DiscreteCostPolicy extends DSLCostPolicy(DSLCostPolicyNames.discrete,
                                                     Set(DSLCostPolicyNameVar, DSLUnitPriceVar, DSLCurrentValueVar)) {

  def computeNewAccumulatingAmount(oldAmount: Double, newEventValue: Double): Double = {
    oldAmount + newEventValue
  }

  def computeResourceInstanceAmountForNewBillingPeriod(oldAmount: Double): Double  = {
    0.0 // ?? def getResourceInstanceInitialAmount
  }

  def getResourceInstanceInitialAmount: Double = {
    0.0
  }
  
  def getValueForCreditCalculation(oldAmountM: Maybe[Double], newEventValue: Double): Maybe[Double] = {
    Just(newEventValue)
  }

  def isBillableFirstEventBasedOnValue(eventValue: Double) = {
    false // nope, we definitely need a  previous one.
  }

  def supportsImplicitEvents = {
    false
  }

  def mustConstructImplicitEndEventFor(resourceEvent: ResourceEvent) = {
    false
  }

  def constructImplicitEndEventFor(resourceEvent: ResourceEvent, occurredMillis: Long) = {
    throw new AquariumException("constructImplicitEndEventFor() Not compliant with %s".format(this))
  }

  def constructImplicitStartEventFor(resourceEvent: ResourceEvent) = {
    throw new AquariumException("constructImplicitStartEventFor() Not compliant with %s".format(this))
  }
}

/**
 * Encapsulates the possible states that a resource with an
 * [[gr.grnet.aquarium.logic.accounting.dsl.OnOffCostPolicy]]
 * can be.
 */
abstract class OnOffPolicyResourceState(val state: String) {
  def isOn: Boolean = !isOff
  def isOff: Boolean = !isOn
}

object OnOffPolicyResourceState {
  def apply(name: Any): OnOffPolicyResourceState = {
    name match {
      case x: String if (x.equalsIgnoreCase(OnOffPolicyResourceStateNames.on))  => OnResourceState
      case y: String if (y.equalsIgnoreCase(OnOffPolicyResourceStateNames.off)) => OffResourceState
      case a: Double if (a == 0) => OffResourceState
      case b: Double if (b == 1) => OnResourceState
      case i: Int if (i == 0) => OffResourceState
      case j: Int if (j == 1) => OnResourceState
      case _ => throw new DSLParseException("Invalid OnOffPolicyResourceState %s".format(name))
    }
  }
}

object OnOffPolicyResourceStateNames {
  final val on  = "on"
  final val off = "off"
}

object OnResourceState extends OnOffPolicyResourceState(OnOffPolicyResourceStateNames.on) {
  override def isOn = true
}
object OffResourceState extends OnOffPolicyResourceState(OnOffPolicyResourceStateNames.off) {
  override def isOff = true
}
