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

package gr.grnet.aquarium.simulation

import java.util.Date
import com.ckkloverdos.maybe.Maybe
import gr.grnet.aquarium.store.RecordID
import gr.grnet.aquarium.event.ResourceEvent

/**
 * A simulator for an instance of the standard `bandwidth` resource.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class StdBandwidthInstanceSim(override val resource: StdBandwidthResourceSim,
                                   override val instanceId: String,
                                   override val owner: UserSim,
                                   override val client: ClientSim)
extends ResourceInstanceSim(resource, instanceId, owner, client) {
  def useBandwidth(occurredDate: Date,
                   megaBytes: Double,
                   details: Map[String, String] = Map(),
                   eventVersion: String = "1.0"): RecordID = {

    newResourceEvent(
     occurredDate.getTime,
     occurredDate.getTime,
     megaBytes,
     details,
     eventVersion
    )
  }
}