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

import gr.grnet.aquarium.util.json.JsonSupport
import gr.grnet.aquarium.logic.accounting.Policy
import java.util.Date
import logic.accounting.dsl.DSLAgreement
import com.ckkloverdos.maybe.{Failed, NoVal, Maybe, Just}

/**
 * Snapshot of data that are user-related.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

sealed trait UserDataSnapshot[T] extends DataSnapshot[T]

case class CreditSnapshot(data: Double, snapshotTime: Long) extends UserDataSnapshot[Double]

case class RolesSnapshot(data: List[String], snapshotTime: Long) extends UserDataSnapshot[List[String]]

// TODO: Check if needed
case class PaymentOrdersSnapshot(data: List[AnyRef], snapshotTime: Long) extends UserDataSnapshot[List[AnyRef]]

// TODO: Check if needed
case class OwnedGroupsSnapshot(data: List[String], snapshotTime: Long) extends UserDataSnapshot[List[String]]

// TODO: Check if needed
case class GroupMembershipsSnapshot(data: List[String], snapshotTime: Long) extends UserDataSnapshot[List[String]]

/**
 * Represents an agreement valid for a specific amount of time. By convention,
 * if an agreement is currently valid, then the validTo field is equal to -1.
 */
case class Agreement(agreement: String, validFrom: Long, validTo: Long) {
  if (validTo != -1) assert(validTo > validFrom)
  assert(!agreement.isEmpty)

  Policy.policy(new Date(validFrom)) match {
    case Just(x) => x.findAgreement(agreement) match {
      case None => assert(false)
      case _ =>
    }
    case _ => assert(false)
  }
}

/**
 * All user agreements. The provided list of agreements cannot have time gaps. This
 * is checked at object creation type.
 */
case class AgreementSnapshot(data: List[Agreement], snapshotTime: Long)
  extends UserDataSnapshot[List[Agreement]] {

  ensureNoGaps(data.sortWith((a,b) => if (b.validFrom > a.validFrom) true else false))

  def ensureNoGaps(agreements: List[Agreement]): Unit = agreements match {
    case h :: t => assert(h.validTo - t.head.validFrom == 1); ensureNoGaps(t)
    case h :: Nil => assert(h.validTo == -1)
  }

  /**
   * Get the user agreement at the specified timestamp
   */
  def getAgreement(at: Long): Maybe[DSLAgreement] =
    data.find{ x => x.validFrom < at && x.validTo > at} match {
      case Some(x) => Policy.policy(new Date(at)) match {
        case Just(y) =>  y.findAgreement(x.agreement) match {
          case Some(z) => Just(z)
          case None => NoVal
        }
        case NoVal => NoVal
        case failed @ Failed(x, y) => failed
      }
      case None => NoVal
    }
}

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
    data: Double,
    snapshotTime: Long)
  extends UserDataSnapshot[Double] {

  def value = data
  
  def isSameResource(name: String, instanceId: String) = {
    this.name == name &&
    this.instanceId == instanceId
  }
}

/**
 * A map from (resourceName, resourceInstanceId) to (value, snapshotTime).
 * This representation is convenient for computations and updating, while the
 * [[gr.grnet.aquarium.user.OwnedResourcesSnapshot]] representation is convenient for JSON serialization.
 */
class OwnedResourcesMap(map: Map[(String, String), (Double, Long)]) {
  def toResourcesSnapshot(snapshotTime: Long): OwnedResourcesSnapshot =
    OwnedResourcesSnapshot(
      map map {
        case ((name, instanceId), (value, snapshotTime)) ⇒
          ResourceInstanceSnapshot(name, instanceId, value, snapshotTime
      )} toList,
      snapshotTime
    )
}

case class OwnedResourcesSnapshot(data: List[ResourceInstanceSnapshot], snapshotTime: Long)
  extends UserDataSnapshot[List[ResourceInstanceSnapshot]] with JsonSupport {

  def toResourcesMap: OwnedResourcesMap = {
    val tuples = for(rc <- data) yield ((rc.name, rc.instanceId), (rc.value, rc.snapshotTime))

    new OwnedResourcesMap(Map(tuples.toSeq: _*))
  }

  def findResourceSnapshot(name: String, instanceId: String): Option[ResourceInstanceSnapshot] =
    data.find { x => name == x.name && instanceId == x.instanceId }

  
  def addOrUpdateResourceSnapshot(name: String,
                                  instanceId: String,
                                  newEventValue: Double,
                                  snapshotTime: Long): (OwnedResourcesSnapshot, Option[ResourceInstanceSnapshot], ResourceInstanceSnapshot) = {

    val newRCInstance = ResourceInstanceSnapshot(name, instanceId, newEventValue, snapshotTime)
    val oldRCInstanceOpt = this.findResourceSnapshot(name, instanceId)
    val newData = oldRCInstanceOpt match {
      case Some(oldRCInstance) ⇒
        // Need to delete the old one and add the new one
        // FIXME: Get rid of this Policy.policy
        val costPolicy = Policy.policy.findResource(name).get.costPolicy
        val newValue = costPolicy.computeNewResourceInstanceValue(oldRCInstance.value, newRCInstance.value/* =newEventValue */)

        newRCInstance.copy(data = newValue) :: (data.filterNot(_.isSameResource(name, instanceId)))
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
