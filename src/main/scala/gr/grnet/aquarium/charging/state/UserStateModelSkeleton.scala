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
import gr.grnet.aquarium.charging.wallet.WalletEntry
import gr.grnet.aquarium.policy.ResourceType

/**
 * Provides common, helper operations for a [[gr.grnet.aquarium.charging.state.UserStateModel]].
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

abstract class UserStateModelSkeleton extends UserStateModel {
  protected def mutableMap[Vin, Vout](
      inputMap: Map[String, Vin],
      vInOut: Vin ⇒ Vout
  ): mutable.Map[(String, String), Vout] = {
    val items = for {
      (resourceAndInstanceID, vIn) ← inputMap.toSeq
    } yield {
      StdUserState.resourceAndInstanceIDOfString(resourceAndInstanceID) -> vInOut(vIn)
    }

    mutable.Map(items: _*)
  }

  protected def mutableAccumulatingAmountMap: mutable.Map[(String, String), Double] = {
    mutableMap(accumulatingAmountOfResourceInstance, identity[Double])
  }

  protected def mutableChargingDataMap: mutable.Map[(String, String), mutable.Map[String, Any]] = {
    mutableMap(chargingDataOfResourceInstance, (vIn: Map[String, Any]) ⇒ mutable.Map(vIn.toSeq: _*))
  }

  protected def mutableImplicitlyIssuedStartMap: mutable.Map[(String, String), ResourceEventModel] = {
    mutable.Map(implicitlyIssuedStartEvents.map(rem ⇒ (rem.safeResource, rem.safeInstanceID) -> rem): _*)
  }

  protected def mutablePreviousEventsMap: mutable.Map[(String, String), ResourceEventModel] = {
    mutable.Map(previousResourceEvents.map(rem ⇒ (rem.safeResource, rem.safeInstanceID) -> rem): _*)
  }

  protected def mutableWalletEntries = {
    val buffer = new mutable.ListBuffer[WalletEntry]
    buffer ++= this.walletEntries
    buffer
  }

  protected def mutableAgreementHistory = {
    this.agreementHistory.toWorkingAgreementHistory
  }

  def toWorkingUserState(resourceTypesMap: Map[String, ResourceType]): WorkingUserState = {
    new WorkingUserState(
      this.userID,
      this.parentIDInStore,
      this.chargingReason,
      resourceTypesMap,
      mutablePreviousEventsMap,
      mutableImplicitlyIssuedStartMap,
      mutableAccumulatingAmountMap,
      mutableChargingDataMap,
      this.totalCredits,
      mutableAgreementHistory,
      this.occurredMillis,
      this.latestResourceEventOccurredMillis,
      this.billingPeriodOutOfSyncResourceEventsCounter,
      mutableWalletEntries
    )
  }
}
