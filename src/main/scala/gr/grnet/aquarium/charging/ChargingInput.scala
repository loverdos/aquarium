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

/**
 * An input that is used in a charging function.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

sealed abstract class ChargingInput(
    val name: String,
    val isDirectlyRelatedToPreviousEvent: Boolean = false,
    val isDirectlyRelatedToCurrentEvent: Boolean = false
)

/**
 * The type of [[gr.grnet.aquarium.charging.ChargingInput]] that holds the name of the cost
 * policy for which a cost computation applies.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case object ChargingBehaviorNameInput extends ChargingInput("chargingBehaviorName")

/**
 * The type of [[gr.grnet.aquarium.charging.ChargingInput]] that holds the total credits.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case object TotalCreditsInput extends ChargingInput("totalCredits")

/**
 * The type of [[gr.grnet.aquarium.charging.ChargingInput]] that holds the old total (accumulating)
 * amount, that is the resource amount before taking into account a new resource event.
 * For example, in the case of `diskspace`, this is the total diskspace used by a user.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case object OldTotalAmountInput extends ChargingInput("oldTotalAmount")

/**
 * The type of [[gr.grnet.aquarium.charging.ChargingInput]] that holds the new total (accumulating)
 * amount, that is the resource amount after taking into account a new resource event.
 * For example, in the case of `diskspace`, this is the total diskspace used by a user.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case object NewTotalAmountInput extends ChargingInput("newTotalAmount")

/**
 * The type of [[gr.grnet.aquarium.charging.ChargingInput]] that holds the time delta between two
 * consecutive resource events of the same type (same `resource` and `instanceId`). Time is measured in milliseconds.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case object TimeDeltaInput extends ChargingInput("timeDelta", true, true)

/**
 * The type of [[gr.grnet.aquarium.charging.ChargingInput]] that holds the `value` of the previous
 * [[gr.grnet.aquarium.event.model.resource.ResourceEventModel]].
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case object PreviousValueInput extends ChargingInput("previousValue", true, false)

/**
 * The type of [[gr.grnet.aquarium.charging.ChargingInput]] that holds the `value` of the current
 * [[gr.grnet.aquarium.event.model.resource.ResourceEventModel]].
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case object CurrentValueInput extends ChargingInput("currentValue", false, true)

/**
 * The type of [[gr.grnet.aquarium.charging.ChargingInput]] that holds the unit price.
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case object UnitPriceInput extends ChargingInput("unitPrice")

case object DetailsInput extends ChargingInput("details")




