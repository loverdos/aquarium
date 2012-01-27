/*
 * Copyright 2011 GRNET S.A. All rights reserved.
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

import com.ckkloverdos.maybe.{Failed, Just, Maybe}


/**
 * A cost policy indicates how charging for a resource will be done
 * wrt the various states a resource can be.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
abstract class DSLCostPolicy(val name: String) {
  def isOnOff: Boolean = isNamed(DSLCostPolicyNames.onoff)

  def isContinuous: Boolean = isNamed(DSLCostPolicyNames.continuous)

  def isDiscrete: Boolean = isNamed(DSLCostPolicyNames.discrete)
  
  def isNamed(aName: String): Boolean = aName == name
  
  def needsPreviousEventForCreditCalculation: Boolean

  /**
   * True if the resource event has an absolute value.
   */
  def resourceEventValueIsAbs: Boolean = !resourceEventValueIsDiff
  def resourceEventValueIsDiff: Boolean = !resourceEventValueIsAbs

  /**
   * True if an absolute value is needed for the credit calculation (for example: diskspace).
   * An absolute value is obtained using the beginning of the billing period as a reference point
   * and is modified every time a new resource event arrives.
   *
   * At the beginning of the billing period, a resource's absolute value can either:
   * a) Set to zero
   * b) Set to a non-zero, constant value
   * c) Set to a varying value, based on some function
   * d) Not change at all. For this case, we need some initial value to start with.
   */
  def needsAbsValueForCreditCalculation: Boolean = !needsDiffValueForCreditCalculation
  def needsDiffValueForCreditCalculation: Boolean = !needsAbsValueForCreditCalculation

  /**
   * Given the old value and a value from a resource event, compute the new one.
   */
  def computeNewResourceInstanceValue(oldValue: Double, newEventValue: Double): Double

  /**
   * Get the value that will be used in credit calculation in Accounting.chargeEvents
   */
  def getCreditCalculationValue(oldValue: Double, newEventValue: Double): Maybe[Double]

  /**
   * An event's value by itself should carry enough info to characterize it billable or not.
   *
   * Typically all events are billable by default and indeed this is the default implementation
   * provided here.
   */
  def isBillableEventBasedOnValue(newEventValue: Double): Boolean = true
}

object DSLCostPolicyNames {
  final val onoff      = "onoff"
  final val discrete   = "discrete"
  final val continuous = "continuous"
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

        case _ ⇒
          throw new DSLParseException("Invalid cost policy %s".format(name))
      }
    }
  }
}

/**
 * In practice a resource usage will be charged for the total amount of usage
 * between resource usage changes.
 *
 * Example resource that might be adept to a continuous policy
 * is diskspace.
 */
case object ContinuousCostPolicy extends DSLCostPolicy(DSLCostPolicyNames.continuous) {
  def needsPreviousEventForCreditCalculation: Boolean = true

  override def needsAbsValueForCreditCalculation = true

  override def resourceEventValueIsDiff = true

  def computeNewResourceInstanceValue(oldValue: Double, newEventValue: Double) = {
    oldValue + newEventValue
  }

  def getCreditCalculationValue(oldValue: Double, newEventValue: Double): Maybe[Double] = {
    Just(oldValue)
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
case object OnOffCostPolicy extends DSLCostPolicy(DSLCostPolicyNames.onoff) {
  def needsPreviousEventForCreditCalculation: Boolean = true

  override def needsAbsValueForCreditCalculation = true

  override def resourceEventValueIsAbs = true

  def computeNewResourceInstanceValue(oldValue: Double, newEventValue: Double) = {
    newEventValue
  }
  
  def getCreditCalculationValue(oldValue: Double, newEventValue: Double): Maybe[Double] = {
    import OnOffCostPolicyValues.{ON, OFF}

    def exception(rs: OnOffPolicyResourceState) =
      new Exception("Resource state transition error (%s -> %s)".format(rs, rs))
    def failed(rs: OnOffPolicyResourceState) =
      Failed(exception(rs))

    (oldValue, newEventValue) match {
      case (ON, ON) ⇒
        failed(OnResourceState)
      case (ON, OFF) ⇒
        Just(OFF)
      case (OFF, ON) ⇒
        Just(ON)
      case (OFF, OFF) ⇒
        failed(OffResourceState)
    }
  }

  override def isBillableEventBasedOnValue(newEventValue: Double) = {
    OnOffCostPolicyValues.isOFF(newEventValue)
  }
}

object OnOffCostPolicyValues {
  final val ON : Double = 1.0
  final val OFF: Double = 0.0

  def isON (value: Double) = value == ON
  def isOFF(value: Double) = value == OFF
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
case object DiscreteCostPolicy extends DSLCostPolicy(DSLCostPolicyNames.discrete) {
  def needsPreviousEventForCreditCalculation: Boolean = false

  override def needsDiffValueForCreditCalculation = true

  override def resourceEventValueIsDiff = true

  def computeNewResourceInstanceValue(oldValue: Double, newEventValue: Double) = {
    newEventValue
  }
  
  def getCreditCalculationValue(oldValue: Double, newEventValue: Double): Maybe[Double] = {
    Just(newEventValue)
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
