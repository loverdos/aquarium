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

/**
 * A cost policy indicates how charging for a resource will be done
 * wrt the various states a resource can be.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
abstract class DSLCostPolicy(name: String)

object DSLCostPolicy {
  def apply(name: String): DSLCostPolicy  = {
    name match {
      case x if(x.equalsIgnoreCase("onoff"))  => OnOffCostPolicy
      case y if(y.equalsIgnoreCase("continuous"))   => ContinuousCostPolicy
      case z if(z.equalsIgnoreCase("discrete")) => DiscreteCostPolicy
      case _ => throw new DSLParseException("Invalid cost policy %s".format(name))
    }
  }
}

/**
 * A continuous cost policy expects a resource's usage to be
 * a continuous function `f(t)`, where `t` is the time since the
 * resource was first used.
 * For continuous resources, the charging algorithm calculates costs
 * as the integral of function `f(t)`. In practice, this means that
 * a resource usage will be charged for the total amount of usage
 * between resource usage changes.
 *
 * Example resources that might be adept to a continuous policy
 * are bandwidth and diskspace.
 */
case object ContinuousCostPolicy extends DSLCostPolicy("continuous")

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
case object OnOffCostPolicy extends DSLCostPolicy("onoff")

/**
 * An discrete cost policy indicates that a resource should be charged directly
 * at each resource state change, i.e. the charging is not dependent on
 * the time the resource.
 *
 * Example oneoff resources might be individual charges applied to various
 * actions (e.g. the fact that a user has created an account) or resources
 * that should be charged per volume once (e.g. the allocation of a volume)
 */
case object DiscreteCostPolicy extends DSLCostPolicy("discrete")

/**
 * Encapsulates the possible states that a resource with an
 * [[gr.grnet.aquarium.logic.accounting.dsl.OnOffCostPolicy]]
 * can be.
 */
abstract class OnOffPolicyResourceState(state: String) {
  def isOn: Boolean = !isOff
  def isOff: Boolean = !isOn
}

object OnOffPolicyResourceState {
  def apply(name: Any): OnOffPolicyResourceState = {
    name match {
      case x: String if (x.equalsIgnoreCase("on"))  => OnResourceState
      case y: String if (y.equalsIgnoreCase("off")) => OffResourceState
      case a: Double if (a == 0) => OffResourceState
      case b: Double if (b == 1) => OnResourceState
      case i: Int if (i == 0) => OffResourceState
      case j: Int if (j == 1) => OnResourceState
      case _ => throw new DSLParseException("Invalid OnOffPolicyResourceState %s".format(name))
    }
  }
}

object OnResourceState extends OnOffPolicyResourceState("on") {
  override def isOn = true
}
object OffResourceState extends OnOffPolicyResourceState("off") {
  override def isOff = true
}
