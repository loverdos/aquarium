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

import scala.collection.mutable
import gr.grnet.aquarium.logic.accounting.dsl.DSLResourcesMap
import gr.grnet.aquarium.logic.accounting.Accounting
import gr.grnet.aquarium.computation.data.{LatestResourceEventsWorker, ImplicitlyIssuedResourceEventsWorker, IgnoredFirstResourceEventsWorker}
import gr.grnet.aquarium.util.ContextualLogger
import gr.grnet.aquarium.event.model.resource.ResourceEventModel

/**
 * A helper object holding intermediate state/results during resource event processing.
 *
 * @param previousResourceEvents
 * This is a collection of all the latest resource events.
 * We want these in order to correlate incoming resource events with their previous (in `occurredMillis` time)
 * ones. Will be updated on processing the next resource event.
 *
 * @param implicitlyIssuedStartEvents
 * The implicitly issued resource events at the beginning of the billing period.
 *
 * @param ignoredFirstResourceEvents
 * The resource events that were first (and unused) of their kind.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class UserStateWorker(userID: String,
                           previousResourceEvents: LatestResourceEventsWorker,
                           implicitlyIssuedStartEvents: ImplicitlyIssuedResourceEventsWorker,
                           ignoredFirstResourceEvents: IgnoredFirstResourceEventsWorker,
                           resourcesMap: DSLResourcesMap) {

  /**
   * Finds the previous resource event by checking two possible sources: a) The implicitly terminated resource
   * events and b) the explicit previous resource events. If the event is found, it is removed from the
   * respective source.
   *
   * If the event is not found, then this must be for a new resource instance.
   * (and probably then some `zero` resource event must be implied as the previous one)
   *
   * @param resource
   * @param instanceId
   * @return
   */
  def findAndRemovePreviousResourceEvent(resource: String, instanceId: String): Option[ResourceEventModel] = {
    // implicitly issued events are checked first
    implicitlyIssuedStartEvents.findAndRemoveResourceEvent(resource, instanceId) match {
      case some@Some(_) ⇒
        some
      case None ⇒
        // explicit previous resource events are checked second
        previousResourceEvents.findAndRemoveResourceEvent(resource, instanceId) match {
          case some@Some(_) ⇒
            some
          case _ ⇒
            None
        }
    }
  }

  def updateIgnored(resourceEvent: ResourceEventModel): Unit = {
    ignoredFirstResourceEvents.updateResourceEvent(resourceEvent)
  }

  def updatePrevious(resourceEvent: ResourceEventModel): Unit = {
    previousResourceEvents.updateResourceEvent(resourceEvent)
  }

  def debugTheMaps(clog: ContextualLogger)(rcDebugInfo: ResourceEventModel ⇒ String): Unit = {
    if(previousResourceEvents.size > 0) {
      val map = previousResourceEvents.latestEventsMap.map {
        case (k, v) => (k, rcDebugInfo(v))
      }
      clog.debugMap("previousResourceEvents", map, 0)
    }
    if(implicitlyIssuedStartEvents.size > 0) {
      val map = implicitlyIssuedStartEvents.implicitlyIssuedEventsMap.map {
        case (k, v) => (k, rcDebugInfo(v))
      }
      clog.debugMap("implicitlyTerminatedResourceEvents", map, 0)
    }
    if(ignoredFirstResourceEvents.size > 0) {
      val map = ignoredFirstResourceEvents.ignoredFirstEventsMap.map {
        case (k, v) => (k, rcDebugInfo(v))
      }
      clog.debugMap("ignoredFirstResourceEvents", map, 0)
    }
  }

  //  private[this]
  //  def allPreviousAndAllImplicitlyStarted: List[ResourceEvent] = {
  //    val buffer: FullMutableResourceTypeMap = scala.collection.mutable.Map[FullResourceType, ResourceEvent]()
  //
  //    buffer ++= implicitlyIssuedStartEvents.implicitlyIssuedEventsMap
  //    buffer ++= previousResourceEvents.latestEventsMap
  //
  //    buffer.valuesIterator.toList
  //  }

  /**
   * Find those events from `implicitlyIssuedStartEvents` and `previousResourceEvents` that will generate implicit
   * end events along with those implicitly issued events. Before returning, remove the events that generated the
   * implicit ends from the internal state of this instance.
   *
   * @see [[gr.grnet.aquarium.logic.accounting.dsl.DSLCostPolicy]]
   */
  def findAndRemoveGeneratorsOfImplicitEndEvents(
      newOccuredMillis: Long
  ): (List[ResourceEventModel], List[ResourceEventModel]) = {

    val buffer = mutable.ListBuffer[(ResourceEventModel, ResourceEventModel)]()
    val checkSet = mutable.Set[ResourceEventModel]()

    def doItFor(map: ResourceEventModel.FullMutableResourceTypeMap): Unit = {
      val resourceEvents = map.valuesIterator
      for {
        resourceEvent ← resourceEvents
        dslResource ← resourcesMap.findResource(resourceEvent.safeResource)
        costPolicy = dslResource.costPolicy
      } {
        if(costPolicy.supportsImplicitEvents) {
          if(costPolicy.mustConstructImplicitEndEventFor(resourceEvent)) {
            val implicitEnd = costPolicy.constructImplicitEndEventFor(resourceEvent, newOccuredMillis)

            if(!checkSet.contains(resourceEvent)) {
              checkSet.add(resourceEvent)
              buffer append ((resourceEvent, implicitEnd))
            }

            // remove it anyway
            map.remove((resourceEvent.safeResource, resourceEvent.safeInstanceId))
          }
        }
      }
    }

    doItFor(previousResourceEvents.latestEventsMap) // we give priority for previous
    doItFor(implicitlyIssuedStartEvents.implicitlyIssuedEventsMap) // ... over implicitly issued...

    (buffer.view.map(_._1).toList, buffer.view.map(_._2).toList)
  }
}

object UserStateWorker {
  def fromUserState(userState: UserState, resourcesMap: DSLResourcesMap): UserStateWorker = {
    UserStateWorker(
      userState.userID,
      userState.latestResourceEventsSnapshot.toMutableWorker,
      userState.implicitlyIssuedSnapshot.toMutableWorker,
      IgnoredFirstResourceEventsWorker.Empty,
      resourcesMap
    )
  }
}
