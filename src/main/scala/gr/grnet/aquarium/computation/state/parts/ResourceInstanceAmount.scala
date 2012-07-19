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

package gr.grnet.aquarium.computation
package state
package parts

/**
 * Maintains the current state of a resource instance owned by the user.
 *
 * In order to have a uniform representation of the resource state for all
 * resource types (complex or simple) the following convention applies:
 *
 *  - If the resource is complex, the (name, instanceID) is (DSLResource.name, instance-id)
 *  - If the resource is simple,  the (name, instanceID) is (DSLResource.name, "1")
 *
 * @param resource        Same as `resource` of [[gr.grnet.aquarium.event.model.resource.ResourceEventModel]]
 * @param instanceID      Same as `instanceID` of [[gr.grnet.aquarium.event.model.resource.ResourceEventModel]]
 * @param instanceAmount  This is the amount kept for the resource instance.
*                         The general rule is that an amount saved in a
 *                        [[gr.grnet.aquarium.computation.parts. ResourceInstanceSnapshot]]
 *                        represents a total value, while a value appearing in a
 *                        [[gr.grnet.aquarium.event .model.resource.ResourceEventModel]]
 *                        represents a difference. How these two values are combined to form the new amount is dictated
 *                        by the underlying [[gr.grnet.aquarium.charging.ChargingBehavior]]
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class ResourceInstanceAmount(
    resource: String,
    instanceID: String,
    instanceAmount: Double
) {

  def isSameResourceInstance(resource: String, instanceId: String) = {
    this.resource == resource &&
    this.instanceID == instanceId
  }

  def toResourceInstanceAmountMapElement = (resource, instanceID) -> this
}
