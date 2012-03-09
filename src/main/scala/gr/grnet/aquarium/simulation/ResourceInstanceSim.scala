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

package gr.grnet.aquarium.simulation

import gr.grnet.aquarium.logic.events.ResourceEvent

/**
 * A simulator for a resource instance.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class ResourceInstanceSim (val resource: ResourceSim,
                           val instanceId: String,
                           val owner: UserSim,
                           val client: ClientSim) {

  def uidGen = client.uidGen

  def newResourceEvent(occurredMillis: Long,
                       receivedMillis: Long,
                       value: Double,
                       details: ResourceEvent.Details,
                       eventVersion: String = "1.0") = {

    val event = ResourceEvent(
      uidGen.nextUID(),
      occurredMillis,
      receivedMillis,
      owner.userId,
      client.clientId,
      resource.name,
      instanceId,
      eventVersion,
      value,
      details
    )

    owner._addResourceEvent(event)
  }
}

object ResourceInstanceSim {
  def apply(resource: ResourceSim, instanceId: String, owner: UserSim, client: ClientSim) =
    new ResourceInstanceSim(resource, instanceId, owner, client)
}