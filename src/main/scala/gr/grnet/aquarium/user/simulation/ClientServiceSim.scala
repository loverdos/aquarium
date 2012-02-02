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
package gr.grnet.aquarium.user.simulation

import gr.grnet.aquarium.util.date.DateCalculator
import gr.grnet.aquarium.logic.events.ResourceEvent
import java.util.Date
import gr.grnet.aquarium.logic.accounting.dsl.OnOffCostPolicyValues
import gr.grnet.aquarium.store.memory.MemStore
import com.ckkloverdos.maybe.Maybe
import gr.grnet.aquarium.store.RecordID

/**
 * A simulator for an Aquarium client service, which is an event generator.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class ClientServiceSim(clientId: String) {
  private[this] val UserProto      = UserSim("", new Date(0), new MemStore().resourceEventStore)
  private[this] val VMTimeProto    = VMTimeSim(UserProto, "")
  private[this] val DiskspaceProto = DiskspaceSim(UserProto, "")

  private[this] var _resources = List[ResourceSim]()

  sealed abstract class ResourceSim(_resource: String, val owner: UserSim, val instanceId: String = "") {
    def resource = _resource
  }

  case class VMTimeSim(override val owner: UserSim,
                       override val instanceId: String = "") extends ResourceSim("vmtime", owner, instanceId) {

    def newON(occurredDate: Date, id: String = IDGen.nextUID()): Maybe[RecordID] = {
      val time = occurredDate.getTime
      val occurredTime = time
      val receivedTime = time
      val event = ResourceEvent(
        id,
        occurredTime,
        receivedTime,
        owner.userId,
        clientId,
        resource,
        instanceId,
        "1.0",
        OnOffCostPolicyValues.ON,
        Map())

      owner._addResourceEvent(event)
    }

    def newON_OutOfSync(occuredDate: Date, outOfSyncHours: Int, id: String = IDGen.nextUID()): Maybe[RecordID] = {
      val occurredDateCalc = new DateCalculator(occuredDate)
      val occurredTime = occurredDateCalc.toMillis
      val receivedTime = occurredDateCalc.plusHours(outOfSyncHours).toMillis

      val event = ResourceEvent(
        id,
        occurredTime,
        receivedTime,
        owner.userId,
        clientId,
        resource,
        instanceId,
        "1.0",
        OnOffCostPolicyValues.ON,
        Map())

      owner._addResourceEvent(event)
    }

    def newOFF(occurredDate: Date, id: String = IDGen.nextUID()): Maybe[RecordID] = {
      val time = occurredDate.getTime
      val occurredTime = time
      val receivedTime = time
      val event = ResourceEvent(
        id,
        occurredTime,
        receivedTime,
        owner.userId,
        clientId,
        resource,
        instanceId,
        "1.0",
        OnOffCostPolicyValues.OFF,
        Map())

      owner._addResourceEvent(event)
    }

    def newOFF_OutOfSync(occuredDate: Date, outOfSyncHours: Int, id: String = IDGen.nextUID()): Maybe[RecordID] = {
      val occurredDateCalc = new DateCalculator(occuredDate)
      val occurredTime = occurredDateCalc.toMillis
      val receivedTime = occurredDateCalc.plusHours(outOfSyncHours).toMillis

      val event = ResourceEvent(
        id,
        occurredTime,
        receivedTime,
        owner.userId,
        clientId,
        resource,
        instanceId,
        "1.0",
        OnOffCostPolicyValues.OFF,
        Map())

      owner._addResourceEvent(event)
    }

    def newONOFF(occurredDateForON: Date,
                 totalVMTimeInHours: Int,
                 idON: String = IDGen.nextUID(),
                 idOFF: String = IDGen.nextUID()): (Maybe[RecordID], Maybe[RecordID]) = {
      val onID = newON(occurredDateForON, idON)
      val offDate = new DateCalculator(occurredDateForON).plusHours(totalVMTimeInHours).toDate
      val offID = newOFF(offDate, idOFF)

      (onID, offID)
    }

    def newONOFF_OutOfSync(occurredDateForON: Date,
                           totalVMTimeInHours: Int,
                           outOfSyncONHours: Int,
                           outOfSyncOFFHours: Int,
                           idON: String = IDGen.nextUID(),
                           idOFF: String = IDGen.nextUID()): (Maybe[RecordID], Maybe[RecordID]) = {
      val onID = newON_OutOfSync(occurredDateForON, outOfSyncONHours, idON)
      val occurredDateCalcForOFF = new DateCalculator(occurredDateForON).plusHours(totalVMTimeInHours)
      val occurredDateForOFF = occurredDateCalcForOFF.toDate
      val offID = newOFF_OutOfSync(occurredDateForOFF, outOfSyncOFFHours, idOFF)

      (onID, offID)
    }
  }

  case class DiskspaceSim(override val owner: UserSim,
                          override val instanceId: String = "") extends ResourceSim("diskspace", owner, instanceId) {
  }

  private[simulation]
  def _addVMTime(vmtime: VMTimeSim): VMTimeSim = {
    _resources = vmtime :: _resources
    vmtime
  }

  private[simulation]
  def _addDiskspace(diskspace: DiskspaceSim): DiskspaceSim = {
    _resources = diskspace :: _resources
    diskspace
  }

  def qualifyResource(resource: String, instanceId: String) = {
    "%s/%s/%s".format(clientId, resource, instanceId)
  }

  def newVMTime(owner: UserSim, _instanceId: String = IDGen.nextUID()): VMTimeSim = {
    owner._addServiceClient(this)
    _addVMTime(VMTimeSim(owner, this.qualifyResource(VMTimeProto.resource, _instanceId)))
  }

  def newDiskspace(owner: UserSim, _instanceId: String = IDGen.nextUID()): DiskspaceSim = {
    owner._addServiceClient(this)
    _addDiskspace(DiskspaceSim(owner, this.qualifyResource(DiskspaceProto.resource, _instanceId)))
  }

  def myResources: List[ResourceSim] = _resources
}