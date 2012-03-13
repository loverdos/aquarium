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
import gr.grnet.aquarium.logic.accounting.dsl.{DSLCostPolicy, DSLPolicy, DiscreteCostPolicy}


/**
 * A simulator for the standard `bandwidth` resource.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class StdBandwidthResourceSim(name: String = StdVMTimeResourceSim.DSLNames.name,
                              unit: String = StdVMTimeResourceSim.DSLNames.unit,
                              costPolicy: DSLCostPolicy = DiscreteCostPolicy,
                              isComplex: Boolean = false,
                              descriminatorField: String = StdDiskspaceResourceSim.DSLNames.descriminatorField)
extends ResourceSim(name,
                    unit,
                    costPolicy,
                    isComplex,
                    descriminatorField) {

override def newInstance(instanceId: String, owner: UserSim, client: ClientSim) =
    StdBandwidthInstanceSim(this, instanceId, owner, client)
}


object StdBandwidthResourceSim {
  object DSLNames {
    final val name = "bandwidth"
    final val unit = "MB/Hr"
    final val descriminatorField = "instanceId"
  }

  def fromPolicy(dslPolicy: DSLPolicy): StdBandwidthResourceSim = {
    val dslResource = dslPolicy.findResource(DSLNames.name).get
    new StdBandwidthResourceSim(
      dslResource.name,
      dslResource.unit,
      dslResource.costPolicy,
      dslResource.isComplex,
      dslResource.descriminatorField)
  }
}

