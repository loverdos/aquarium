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
 * The type of variable needed in order to compute costs, that is credit charges.
 *
 * A [[gr.grnet.aquarium.logic.accounting.dsl.DSLCostPolicy]] declares which variables
 * it needs in order to compute credit charges. These and only these variables are
 * passed to the repsective [[gr.grnet.aquarium.logic.accounting.dsl.DSLAlgorithm]] (and
 * are also used to type-check the algorithm).
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
sealed abstract class DSLCostPolicyVar(val name: String) {
  def isDirectlyRelatedToPreviousEvent: Boolean = false
  def isDirectlyRelatedToCurrentEvent: Boolean = false
}

/**
 * The type of [[gr.grnet.aquarium.logic.accounting.dsl.DSLCostPolicyVar]] that holds the name of the cost
 * policy for which a cost computation applies.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case object DSLCostPolicyNameVar extends DSLCostPolicyVar("costPolicyName")

/**
 * The type of [[gr.grnet.aquarium.logic.accounting.dsl.DSLCostPolicyVar]] that holds the total credits.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case object DSLTotalCreditsVar extends DSLCostPolicyVar("totalCredits")

/**
 * The type of [[gr.grnet.aquarium.logic.accounting.dsl.DSLCostPolicyVar]] that holds the old total (accumulating)
 * amount, that is the resource amount before taking into account a new resource event.
 * For example, in the case of `diskspace`, this is the total diskspace used by a user.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case object DSLOldTotalAmountVar extends DSLCostPolicyVar("oldTotalAmount")

/**
 * The type of [[gr.grnet.aquarium.logic.accounting.dsl.DSLCostPolicyVar]] that holds the new total (accumulating)
 * amount, that is the resource amount after taking into account a new resource event.
 * For example, in the case of `diskspace`, this is the total diskspace used by a user.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case object DSLNewTotalAmountVar extends DSLCostPolicyVar("newTotalAmount")

/**
 * The type of [[gr.grnet.aquarium.logic.accounting.dsl.DSLCostPolicyVar]] that holds the time delta between two
 * consecutive resource events of the same type (same `resource` and `instanceId`). Time is measured in milliseconds.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case object DSLTimeDeltaVar extends DSLCostPolicyVar("timeDelta") {
  override def isDirectlyRelatedToPreviousEvent = true
  override def isDirectlyRelatedToCurrentEvent = true
}

/**
 * The type of [[gr.grnet.aquarium.logic.accounting.dsl.DSLCostPolicyVar]] that holds the `value` of the previous
 * [[gr.grnet.aquarium.logic.events.ResourceEvent]].
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case object DSLPreviousValueVar extends DSLCostPolicyVar("previousValue") {
  override def isDirectlyRelatedToPreviousEvent = true
}

/**
 * The type of [[gr.grnet.aquarium.logic.accounting.dsl.DSLCostPolicyVar]] that holds the `value` of the current
 * [[gr.grnet.aquarium.logic.events.ResourceEvent]].
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case object DSLCurrentValueVar extends DSLCostPolicyVar("currentValue") {
  override def isDirectlyRelatedToCurrentEvent = true
}

/**
 * The type of [[gr.grnet.aquarium.logic.accounting.dsl.DSLCostPolicyVar]] that holds the unit price as
 * given in a [[gr.grnet.aquarium.logic.accounting.dsl.DSLPriceList]].
 *
 */
case object DSLUnitPriceVar extends DSLCostPolicyVar("unitPrice")