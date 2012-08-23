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

package gr.grnet.aquarium.charging.state

import scala.collection.mutable
import gr.grnet.aquarium.event.model.resource.ResourceEventModel

/**
 * Working (mutable state) for resource instances of the same resource type.
 *
 * @param details Generic state related to the type of resource as a whole
 * @param stateOfResourceInstance A map from `instanceID` to
 *                                [[gr.grnet.aquarium.charging.state.WorkingResourceInstanceChargingState]].
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
final class WorkingResourcesChargingState(
    val details: mutable.Map[String /* any string */, Any],
    val stateOfResourceInstance: mutable.Map[String /* InstanceID */, WorkingResourceInstanceChargingState]
) {

  def immutableDetails = Map(this.details.toSeq: _*)

  def immutableStateOfResourceInstance = Map((
      for((k, v) ← this.stateOfResourceInstance)
        yield (k, v.toResourceInstanceChargingState)
    ).toSeq:_*)

  def toResourcesChargingState = {
    ResourcesChargingState(
      details = immutableDetails,
      stateOfResourceInstance = immutableStateOfResourceInstance
    )
  }

  /**
   * Find the most recent (latest) holder of a resource event.
   */
//  def findResourceInstanceOfLatestEvent: Option[WorkingResourceInstanceChargingState] = {
//    stateOfResourceInstance.values.toArray.sortWith { (a, b) ⇒
//      (a.previousEvents, b.previousEvents
//    }
//  }
}
