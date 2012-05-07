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

package gr.grnet.aquarium.computation.data

/**
 * Maintains the current state of a resource instance owned by the user.
 * The encoding is as follows:
 *
 * name: DSLResource.name
 * instanceId: instance-id (in the resource's descriminatorField)
 * data: current-resource-value
 * snapshotTime: last-update-timestamp
 *
 * In order to have a uniform representation of the resource state for all
 * resource types (complex or simple) the following convention applies:
 *
 *  - If the resource is complex, the (name, instanceId) is (DSLResource.name, instance-id)
 *  - If the resource is simple,  the (name, instanceId) is (DSLResource.name, "1")
 *
 * @param resource        Same as `resource` of [[gr.grnet.aquarium.event.resource.ResourceEventModel]]
 * @param instanceId      Same as `instanceId` of [[gr.grnet.aquarium.event.resource.ResourceEventModel]]
 * @param instanceAmount  This is the amount kept for the resource instance.
*                         The general rule is that an amount saved in a [[gr.grnet.aquarium.computation.data. ResourceInstanceSnapshot]]
 *                        represents a total value, while a value appearing in a [[gr.grnet.aquarium.event.resource.ResourceEventModel]]
 *                        represents a difference. How these two values are combined to form the new amount is dictated
 *                        by the underlying [[gr.grnet.aquarium.logic.accounting.dsl.DSLCostPolicy]]
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class ResourceInstanceSnapshot(resource: String,
                                    instanceId: String,
                                    instanceAmount: Double) {

  def isSameResourceInstance(resource: String, instanceId: String) = {
    this.resource == resource &&
    this.instanceId == instanceId
  }
}
