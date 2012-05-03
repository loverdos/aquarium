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

package gr.grnet.aquarium
package user

import gr.grnet.aquarium.util.{findFromMapAsMaybe, findAndRemoveFromMap, shortClassNameOf}
import gr.grnet.aquarium.logic.accounting.Policy
import java.util.Date
import com.ckkloverdos.maybe.{NoVal, Maybe, Just}
import gr.grnet.aquarium.event.resource.ResourceEventModel.FullMutableResourceTypeMap
import logic.accounting.dsl.{Timeslot, DSLAgreement}
import collection.immutable.{TreeMap, SortedMap}
import util.date.MutableDateCalc
import event.resource.ResourceEventModel
import gr.grnet.aquarium.event.im.IMEventModel

/**
 * Snapshot of data that are user-related.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class CreditSnapshot(creditAmount: Double, snapshotTime: Long) extends DataSnapshot

case class IMStateSnapshot(imEvent: IMEventModel, snapshotTime: Long) extends DataSnapshot

//case class RolesSnapshot(roles: List[String], snapshotTime: Long) extends DataSnapshot

/**
 * Represents an agreement valid for a specific amount of time. By convention,
 * if an agreement is currently valid, then the validTo field is equal to `Long.MaxValue`.
 */
case class Agreement(name: String, validFrom: Long, validTo: Long = Long.MaxValue) {
  assert(validTo > validFrom)
  assert(!name.isEmpty)

//  Policy.policy(new Date(validFrom)) match {
//    case Just(x) => x.findAgreement(agreement) match {
//      case None => assert(false)
//      case _ =>
//    }
//    case _ => assert(false)
//  }
  
  def timeslot = Timeslot(new Date(validFrom), new Date(validTo))

  override def toString =
    "Agreement(%s, %s, %s)".
      format(name, new MutableDateCalc(validFrom), new MutableDateCalc(validTo))
}

/**
 * All user agreements. The provided list of agreements cannot have time gaps. This
 * is checked at object creation type.
 */
case class AgreementSnapshot(agreements: List[Agreement], snapshotTime: Long) extends DataSnapshot {

  ensureNoGaps(agreements.sortWith((a,b) => if (b.validFrom > a.validFrom) true else false))

  def agreementsByTimeslot: SortedMap[Timeslot, String] = {
    TreeMap(agreements.map(ag => (ag.timeslot, ag.name)): _*)
  }

  def ensureNoGaps(agreements: List[Agreement]): Unit = agreements match {
    case ha :: (t @ (hb :: tail)) =>
      assert(ha.validTo - hb.validFrom == 1);
      ensureNoGaps(t)
    case h :: Nil =>
      assert(h.validTo == Long.MaxValue)
    case Nil => ()
  }

  /**
   * Get the user agreement at the specified timestamp
   */
  def getAgreement(at: Long): Maybe[DSLAgreement] =
    agreements.find{ x => x.validFrom < at && x.validTo > at} match {
      case Some(x) => Policy.policy(new Date(at)).findAgreement(x.name) match {
          case Some(z) => Just(z)
          case None => NoVal
        }
      case None => NoVal
    }

  override def toString = {
    "%s(%s, %s)".format(shortClassNameOf(this), agreements, new MutableDateCalc(snapshotTime).toString)
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
 * @param resource        Same as `resource` of [[gr.grnet.aquarium.event.resource.ResourceEventModel]]
 * @param instanceId      Same as `instanceId` of [[gr.grnet.aquarium.event.resource.ResourceEventModel]]
 * @param instanceAmount  This is the amount kept for the resource instance.
*                         The general rule is that an amount saved in a [[gr.grnet.aquarium.user.ResourceInstanceSnapshot]]
 *                        represents a total value, while a value appearing in a [[gr.grnet.aquarium.event.resource.ResourceEventModel]]
 *                        represents a difference. How these two values are combined to form the new amount is dictated
 *                        by the underlying [[gr.grnet.aquarium.logic.accounting.dsl.DSLCostPolicy]]
 * @param snapshotTime
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class ResourceInstanceSnapshot(resource: String,
                                    instanceId: String,
                                    instanceAmount: Double,
                                    snapshotTime: Long) extends DataSnapshot {

  def isSameResourceInstance(resource: String, instanceId: String) = {
    this.resource == resource &&
    this.instanceId == instanceId
  }
}

/**
 * A map from (resourceName, resourceInstanceId) to (value, snapshotTime).
 * This representation is convenient for computations and updating, while the
 * [[gr.grnet.aquarium.user.OwnedResourcesSnapshot]] representation is convenient for JSON serialization.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class OwnedResourcesMap(resourcesMap: Map[(String, String), (Double, Long)]) {
  def toResourcesSnapshot(snapshotTime: Long): OwnedResourcesSnapshot =
    OwnedResourcesSnapshot(
      resourcesMap map {
        case ((name, instanceId), (value, snapshotTime)) ⇒
          ResourceInstanceSnapshot(name, instanceId, value, snapshotTime
      )} toList,
      snapshotTime
    )
}

/**
 *
 * @param resourceInstanceSnapshots
 * @param snapshotTime
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class OwnedResourcesSnapshot(resourceInstanceSnapshots: List[ResourceInstanceSnapshot], snapshotTime: Long)
  extends DataSnapshot {

  def toResourcesMap: OwnedResourcesMap = {
    val tuples = for(rc <- resourceInstanceSnapshots) yield ((rc.resource, rc.instanceId), (rc.instanceAmount, rc.snapshotTime))

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

    val newResourceInstance = ResourceInstanceSnapshot(resource, instanceId, newAmount, snapshotTime)
    val oldResourceInstanceOpt = this.findResourceInstanceSnapshot(resource, instanceId)

    val newResourceInstances = oldResourceInstanceOpt match {
      case Some(oldResourceInstance) ⇒
        // Resource instance found, so delete the old one and add the new one
        newResourceInstance :: resourceInstanceSnapshotsExcept(resource, instanceId)
      case None ⇒
        // Resource not found, so this is the first time and we just add the new snapshot
        newResourceInstance :: resourceInstanceSnapshots
    }

    val newOwnedResources = OwnedResourcesSnapshot(newResourceInstances, snapshotTime)

    (newOwnedResources, oldResourceInstanceOpt, newResourceInstance)
 }
}


/**
 * A generic exception thrown when errors occur in dealing with user data snapshots
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class DataSnapshotException(msg: String) extends Exception(msg)

/**
 * Holds the user active/suspended status.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
//case class ActiveStateSnapshot(isActive: Boolean, snapshotTime: Long) extends DataSnapshot

/**
 * Keeps the latest resource event per resource instance.
 *
 * @param resourceEvents
 * @param snapshotTime
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class LatestResourceEventsSnapshot(resourceEvents: List[ResourceEventModel],
                                        snapshotTime: Long) extends DataSnapshot {

  /**
   * The gateway to playing with mutable state.
   *
   * @return A fresh instance of [[gr.grnet.aquarium.user.LatestResourceEventsWorker]].
   */
  def toMutableWorker = {
    val map = scala.collection.mutable.Map[ResourceEventModel.FullResourceType, ResourceEventModel]()
    for(latestEvent <- resourceEvents) {
      map(latestEvent.fullResourceInfo) = latestEvent
    }
    LatestResourceEventsWorker(map)
  }
}

/**
 * This is the mutable cousin of [[gr.grnet.aquarium.user.LatestResourceEventsSnapshot]].
 *
 * @param latestEventsMap
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class LatestResourceEventsWorker(latestEventsMap: FullMutableResourceTypeMap) {

  /**
   * The gateway to immutable state.
   *
   * @param snapshotTime The relevant snapshot time.
   * @return A fresh instance of [[gr.grnet.aquarium.user.LatestResourceEventsSnapshot]].
   */
  def toImmutableSnapshot(snapshotTime: Long) =
    LatestResourceEventsSnapshot(latestEventsMap.valuesIterator.toList, snapshotTime)

  def updateResourceEvent(resourceEvent: ResourceEventModel): Unit = {
    latestEventsMap((resourceEvent.resource, resourceEvent.instanceID)) = resourceEvent
  }
  
  def findResourceEvent(resource: String, instanceId: String): Maybe[ResourceEventModel] = {
    findFromMapAsMaybe(latestEventsMap, (resource, instanceId))
  }

  def findAndRemoveResourceEvent(resource: String, instanceId: String): Maybe[ResourceEventModel] = {
    findAndRemoveFromMap(latestEventsMap, (resource, instanceId))
  }

  def size = latestEventsMap.size

  def foreach[U](f: ResourceEventModel => U): Unit = {
    latestEventsMap.valuesIterator.foreach(f)
  }
}

object LatestResourceEventsWorker {
  final val Empty = LatestResourceEventsWorker(scala.collection.mutable.Map())

  /**
   * Helper factory to construct a worker from a list of events.
   */
  def fromList(latestEventsList: List[ResourceEventModel]): LatestResourceEventsWorker = {
    LatestResourceEventsSnapshot(latestEventsList, 0L).toMutableWorker
  }
}

/**
 * Keeps the implicit OFF events when a billing period ends.
 * This is normally recorded in the [[gr.grnet.aquarium.user.UserState]].
 *
 * @param implicitlyIssuedEvents
 * @param snapshotTime
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class ImplicitlyIssuedResourceEventsSnapshot(implicitlyIssuedEvents: List[ResourceEventModel],
                                                  snapshotTime: Long) extends DataSnapshot {
  /**
   * The gateway to playing with mutable state.
   *
   * @return A fresh instance of [[gr.grnet.aquarium.user.ImplicitlyIssuedResourceEventsWorker]].
   */
  def toMutableWorker = {
    val map = scala.collection.mutable.Map[ResourceEventModel.FullResourceType, ResourceEventModel]()
    for(implicitEvent <- implicitlyIssuedEvents) {
      map(implicitEvent.fullResourceInfo) = implicitEvent
    }

    ImplicitlyIssuedResourceEventsWorker(map)
  }
}

/**
 * This is the mutable cousin of [[gr.grnet.aquarium.user.ImplicitlyIssuedResourceEventsSnapshot]].
 *
 * @param implicitlyIssuedEventsMap
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class ImplicitlyIssuedResourceEventsWorker(implicitlyIssuedEventsMap: FullMutableResourceTypeMap) {

  def toList: scala.List[ResourceEventModel] = {
    implicitlyIssuedEventsMap.valuesIterator.toList
  }

  def toImmutableSnapshot(snapshotTime: Long) =
    ImplicitlyIssuedResourceEventsSnapshot(toList, snapshotTime)

  def findAndRemoveResourceEvent(resource: String, instanceId: String): Maybe[ResourceEventModel] = {
    findAndRemoveFromMap(implicitlyIssuedEventsMap, (resource, instanceId))
  }

  def size = implicitlyIssuedEventsMap.size

  def foreach[U](f: ResourceEventModel => U): Unit = {
    implicitlyIssuedEventsMap.valuesIterator.foreach(f)
  }
}

object ImplicitlyIssuedResourceEventsWorker {
  final val Empty = ImplicitlyIssuedResourceEventsWorker(scala.collection.mutable.Map())
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 *
 * @param ignoredFirstEvents
 * @param snapshotTime
 */
case class IgnoredFirstResourceEventsSnapshot(ignoredFirstEvents: List[ResourceEventModel],
                                              snapshotTime: Long) extends DataSnapshot {
  def toMutableWorker = {
    val map = scala.collection.mutable.Map[ResourceEventModel.FullResourceType, ResourceEventModel]()
    for(ignoredFirstEvent <- ignoredFirstEvents) {
      map(ignoredFirstEvent.fullResourceInfo) = ignoredFirstEvent
    }

    IgnoredFirstResourceEventsWorker(map)
  }
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 * @param ignoredFirstEventsMap
 */
case class IgnoredFirstResourceEventsWorker(ignoredFirstEventsMap: FullMutableResourceTypeMap) {
  def toImmutableSnapshot(snapshotTime: Long) =
    IgnoredFirstResourceEventsSnapshot(ignoredFirstEventsMap.valuesIterator.toList, snapshotTime)

  def findAndRemoveResourceEvent(resource: String, instanceId: String): Maybe[ResourceEventModel] = {
    findAndRemoveFromMap(ignoredFirstEventsMap, (resource, instanceId))
  }

  def updateResourceEvent(resourceEvent: ResourceEventModel): Unit = {
    ignoredFirstEventsMap((resourceEvent.resource, resourceEvent.instanceID)) = resourceEvent
  }

  def size = ignoredFirstEventsMap.size

  def foreach[U](f: ResourceEventModel => U): Unit = {
    ignoredFirstEventsMap.valuesIterator.foreach(f)
  }
}

object IgnoredFirstResourceEventsWorker {
  final val Empty = IgnoredFirstResourceEventsWorker(scala.collection.mutable.Map())
}