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

package gr.grnet.aquarium
package user

import util.json.JsonSupport

/**
 * Snapshot of data that are user-related.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

sealed trait UserDataSnapshot[T] extends DataSnapshot[T]

case class CreditSnapshot(data: Double, snapshotTime: Long) extends UserDataSnapshot[Double]

case class AgreementSnapshot(data: String, snapshotTime: Long) extends UserDataSnapshot[String]

case class RolesSnapshot(data: List[String], snapshotTime: Long) extends UserDataSnapshot[List[String]]

// TODO: Check if needed
case class PaymentOrdersSnapshot(data: List[AnyRef], snapshotTime: Long) extends UserDataSnapshot[List[AnyRef]]

// TODO: Check if needed
case class OwnedGroupsSnapshot(data: List[String], snapshotTime: Long) extends UserDataSnapshot[List[String]]

// TODO: Check if needed
case class GroupMembershipsSnapshot(data: List[String], snapshotTime: Long) extends UserDataSnapshot[List[String]]

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
 */
case class ResourceInstanceSnapshot(
    name: String,
    instanceId: String,
    data: Float,
    snapshotTime: Long)
  extends UserDataSnapshot[Float] {

  def value = data
  
  def isResource(name: String, instanceId: String) =
    this.name == name &&
    this.instanceId == instanceId
}
case class OwnedResourcesSnapshot(data: List[ResourceInstanceSnapshot], snapshotTime: Long)
  extends UserDataSnapshot[List[ResourceInstanceSnapshot]] with JsonSupport {

  def findResourceSnapshot(name: String, instanceId: String): Option[ResourceInstanceSnapshot] =
    data.find { x => name.equals(x.name) && instanceId.equals(x.instanceId) }

  
  def addOrUpdateResourceSnapshot(name: String,
                                  instanceId: String,
                                  value: Float,
                                  snapshotTime: Long): (OwnedResourcesSnapshot, Option[ResourceInstanceSnapshot], ResourceInstanceSnapshot) = {
    val oldRCInstanceOpt = this.findResourceSnapshot(name, instanceId)
    val newRCInstance = ResourceInstanceSnapshot(name, instanceId, value, snapshotTime)
    val newData = oldRCInstanceOpt match {
      case Some(currentRCInstance) ⇒
        // Need to delete the old one and add the new one
        val newValue = newRCInstance.data + currentRCInstance.data
        newRCInstance.copy(data = newValue) :: (data.filterNot(_.isResource(name, instanceId)))
      case None ⇒
        // Resource not found, so this is the first time and we just add the new snapshot
        newRCInstance :: data
    }

    val newOwnedResources = this.copy(data = newData, snapshotTime = snapshotTime)

    (newOwnedResources, oldRCInstanceOpt, newRCInstance)
  }
}


/**
 * A generic exception thrown when errors occur in dealing with user data snapshots
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class UserDataSnapshotException(msg: String) extends Exception(msg)

/**
 * Holds the user active/suspended status.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class ActiveSuspendedSnapshot(data: Boolean, snapshotTime: Long) extends UserDataSnapshot[Boolean]