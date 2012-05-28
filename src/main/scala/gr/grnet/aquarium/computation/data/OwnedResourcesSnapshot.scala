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
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class OwnedResourcesSnapshot(resourceInstanceSnapshots: List[ResourceInstanceSnapshot]) {

  def toResourcesMap: OwnedResourcesMap = {
    val tuples = for(rc <- resourceInstanceSnapshots) yield ((rc.resource, rc.instanceId), (rc.instanceAmount))

    new OwnedResourcesMap(Map(tuples.toSeq: _*))
  }

  def resourceInstanceSnapshotsExcept(resource: String, instanceId: String) = {
    // Unfortunately, we have to use a List for data, since JSON serialization is not as flexible
    // (at least out of the box). Thus, the update is O(L), where L is the length of the data List.
    resourceInstanceSnapshots.filterNot(_.isSameResourceInstance(resource, instanceId))
  }

  def findResourceInstanceSnapshot(resource: String, instanceId: String): Option[ResourceInstanceSnapshot] = {
    resourceInstanceSnapshots.find(x => resource == x.resource && instanceId == x.instanceId)
  }

  def getResourceInstanceAmount(resource: String, instanceId: String, defaultValue: Double): Double = {
    findResourceInstanceSnapshot(resource, instanceId).map(_.instanceAmount).getOrElse(defaultValue)
  }

  def computeResourcesSnapshotUpdate(resource: String,   // resource name
                                     instanceId: String, // resource instance id
                                     newAmount: Double,
                                     snapshotTime: Long): (OwnedResourcesSnapshot,
                                                          Option[ResourceInstanceSnapshot],
    ResourceInstanceSnapshot) = {

    val newResourceInstance = ResourceInstanceSnapshot(resource, instanceId, newAmount)
    val oldResourceInstanceOpt = this.findResourceInstanceSnapshot(resource, instanceId)

    val newResourceInstances = oldResourceInstanceOpt match {
      case Some(oldResourceInstance) ⇒
        // Resource instance found, so delete the old one and add the new one
        newResourceInstance :: resourceInstanceSnapshotsExcept(resource, instanceId)

      case None ⇒
        // Resource not found, so this is the first time and we just add the new snapshot
        newResourceInstance :: resourceInstanceSnapshots
    }

    val newOwnedResources = OwnedResourcesSnapshot(newResourceInstances)

    (newOwnedResources, oldResourceInstanceOpt, newResourceInstance)
 }
}

object OwnedResourcesSnapshot {
  final val Empty = OwnedResourcesSnapshot(Nil)
}