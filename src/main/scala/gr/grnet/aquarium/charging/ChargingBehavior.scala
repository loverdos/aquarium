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

package gr.grnet.aquarium.charging

import gr.grnet.aquarium.policy.{ResourceType, EffectivePriceTable, FullPriceTable}
import scala.collection.mutable
import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.Aquarium
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.charging.state.AgreementHistory
import gr.grnet.aquarium.charging.wallet.WalletEntry
import com.ckkloverdos.key.TypedKeySkeleton

/**
 * A charging behavior indicates how charging for a resource will be done
 * wrt the various states a resource instance can be.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

trait ChargingBehavior {
  def alias: String

  def inputs: Set[ChargingInput]

  def selectEffectivePriceTable(
      fullPriceTable: FullPriceTable,
      chargingData: mutable.Map[String, Any],
      currentResourceEvent: ResourceEventModel,
      referenceTimeslot: Timeslot,
      previousValue: Double,
      totalCredits: Double,
      oldAccumulatingAmount: Double,
      newAccumulatingAmount: Double
  ): EffectivePriceTable


  /**
   *
   * @return The number of wallet entries recorded and the new total credits
   */
  def chargeResourceEvent(
      aquarium: Aquarium,
      currentResourceEvent: ResourceEventModel,
      resourceType: ResourceType,
      billingMonthInfo: BillingMonthInfo,
      previousResourceEventOpt: Option[ResourceEventModel],
      userAgreements: AgreementHistory,
      chargingData: mutable.Map[String, Any],
      totalCredits: Double,
      walletEntryRecorder: WalletEntry ⇒ Unit
  ): (Int, Double)

  def supportsImplicitEvents: Boolean

  def mustConstructImplicitEndEventFor(resourceEvent: ResourceEventModel): Boolean

  def constructImplicitEndEventFor(resourceEvent: ResourceEventModel, newOccuredMillis: Long): ResourceEventModel
}

object ChargingBehavior {
  final case class ChargingBehaviorKey[T: Manifest](override val name: String) extends TypedKeySkeleton[T](name)

  /**
   * Keys used to save information between calls of `chargeResourceEvent`
   */
  object EnvKeys {
    final val ResourceInstanceAccumulatingAmount = ChargingBehaviorKey[Double]("resource.instance.accumulating.amount")
  }

  def makeValueMapFor(
      chargingBehavior: ChargingBehavior,
      totalCredits: Double,
      oldTotalAmount: Double,
      newTotalAmount: Double,
      timeDelta: Double,
      previousValue: Double,
      currentValue: Double,
      unitPrice: Double
  ): Map[ChargingInput, Any] = {

    val inputs = chargingBehavior.inputs
    var map = Map[ChargingInput, Any]()

    if(inputs contains ChargingBehaviorNameInput) map += ChargingBehaviorNameInput -> chargingBehavior.alias
    if(inputs contains TotalCreditsInput        ) map += TotalCreditsInput   -> totalCredits
    if(inputs contains OldTotalAmountInput      ) map += OldTotalAmountInput -> oldTotalAmount
    if(inputs contains NewTotalAmountInput      ) map += NewTotalAmountInput -> newTotalAmount
    if(inputs contains TimeDeltaInput           ) map += TimeDeltaInput      -> timeDelta
    if(inputs contains PreviousValueInput       ) map += PreviousValueInput  -> previousValue
    if(inputs contains CurrentValueInput        ) map += CurrentValueInput   -> currentValue
    if(inputs contains UnitPriceInput           ) map += UnitPriceInput      -> unitPrice

    map
  }
}
