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

import gr.grnet.aquarium.event.model.resource.ResourceEventModel

/**
 * Keeps the implicit OFF events when a billing period ends.
 * This is normally recorded in the [[gr.grnet.aquarium.computation.UserState]].
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class ImplicitlyIssuedResourceEventsSnapshot(implicitlyIssuedEvents: List[ResourceEventModel]) {
  /**
   * The gateway to playing with mutable state.
   *
   * @return A fresh instance of [[gr.grnet.aquarium.computation.data.ImplicitlyIssuedResourceEventsWorker]].
   */
  def toMutableWorker = {
    val map = scala.collection.mutable.Map[ResourceEventModel.FullResourceType, ResourceEventModel]()
    for(implicitEvent <- implicitlyIssuedEvents) {
      map(implicitEvent.fullResourceInfo) = implicitEvent
    }

    ImplicitlyIssuedResourceEventsWorker(map)
  }
}

object ImplicitlyIssuedResourceEventsSnapshot {
  final val Empty = ImplicitlyIssuedResourceEventsSnapshot(Nil)
}

