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

import scala.collection.immutable
import scala.collection.mutable

import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import gr.grnet.aquarium.{Aquarium, AquariumInternalError}
import gr.grnet.aquarium.policy.{FullPriceTable, EffectivePriceTable, UserAgreementModel, ResourceType}
import com.ckkloverdos.key.TypedKey
import gr.grnet.aquarium.util._
import gr.grnet.aquarium.util.LogHelpers.Debug
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.charging.wallet.WalletEntry
import gr.grnet.aquarium.computation.{TimeslotComputations, BillingMonthInfo}
import gr.grnet.aquarium.charging.state.AgreementHistory
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.store.PolicyStore
import gr.grnet.aquarium.charging.ChargingBehavior.EnvKeys

/**
 * A charging behavior indicates how charging for a resource will be done
 * wrt the various states a resource can be.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

abstract class ChargingBehaviorSkeleton(
    final val alias: String,
    final val inputs: Set[ChargingInput],
    final val selectorHierarchy: List[List[String]] = Nil
) extends ChargingBehavior with Loggable {

  final val inputNames = inputs.map(_.name)

  @inline private[this] def hrs(millis: Double) = {
    val hours = millis / 1000 / 60 / 60
    val roundedHours = hours
    roundedHours
  }

  protected def computeCreditsToSubtract(
      oldCredits: Double,
      oldAccumulatingAmount: Double,
      newAccumulatingAmount: Double,
      timeDeltaMillis: Long,
      previousValue: Double,
      currentValue: Double,
      unitPrice: Double,
      details: Map[String, String]
  ): (Double, String) = {
    alias match {
     case ChargingBehaviorAliases.continuous ⇒
       val credits = hrs(timeDeltaMillis) * oldAccumulatingAmount * unitPrice
       val explanation = "Time(%s) * OldTotal(%s) * Unit(%s)".format(
         hrs(timeDeltaMillis),
         oldAccumulatingAmount,
         unitPrice
       )

       (credits, explanation)

     case ChargingBehaviorAliases.vmtime ⇒
       val credits = hrs(timeDeltaMillis) * unitPrice
       val explanation = "Time(%s) * Unit(%s)".format(hrs(timeDeltaMillis), unitPrice)

       (credits, explanation)

     case ChargingBehaviorAliases.once ⇒
       val credits = currentValue
       val explanation = "Value(%s)".format(currentValue)

       (credits, explanation)

     case name ⇒
       throw new AquariumInternalError("Cannot compute credit diff for charging behavior %s".format(name))
    }
  }

  protected def rcDebugInfo(rcEvent: ResourceEventModel) = {
    rcEvent.toDebugString
  }

  protected def computeSelectorPath(
      chargingData: mutable.Map[String, Any],
      currentResourceEvent: ResourceEventModel,
      referenceTimeslot: Timeslot,
      previousValue: Double,
      totalCredits: Double,
      oldAccumulatingAmount: Double,
      newAccumulatingAmount: Double
  ): List[String]

  /**
   *
   * @param chargingData
   * @param previousResourceEventOpt
   * @param currentResourceEvent
   * @param billingMonthInfo
   * @param referenceTimeslot
   * @param resourceType
   * @param agreementByTimeslot
   * @param previousValue
   * @param totalCredits
   * @param policyStore
   * @param walletEntryRecorder
   * @return The number of wallet entries recorded and the new total credits
   */
  protected def computeChargeslots(
      chargingData: mutable.Map[String, Any],
      previousResourceEventOpt: Option[ResourceEventModel],
      currentResourceEvent: ResourceEventModel,
      billingMonthInfo: BillingMonthInfo,
      referenceTimeslot: Timeslot,
      resourceType: ResourceType,
      agreementByTimeslot: immutable.SortedMap[Timeslot, UserAgreementModel],
      previousValue: Double,
      totalCredits: Double,
      policyStore: PolicyStore,
      walletEntryRecorder: WalletEntry ⇒ Unit
  ): (Int, Double) = {

    val currentValue = currentResourceEvent.value
    val userID = currentResourceEvent.userID
    val currentDetails = currentResourceEvent.details

    var _oldAccumulatingAmount = getChargingData(
      chargingData,
      EnvKeys.ResourceInstanceAccumulatingAmount
    ).getOrElse(getResourceInstanceInitialAmount)

    var _oldTotalCredits = totalCredits

    var _newAccumulatingAmount = this.computeNewAccumulatingAmount(_oldAccumulatingAmount, currentValue, currentDetails)
    setChargingData(chargingData, EnvKeys.ResourceInstanceAccumulatingAmount, _newAccumulatingAmount)

    val policyByTimeslot = policyStore.loadAndSortPoliciesWithin(
      referenceTimeslot.from.getTime,
      referenceTimeslot.to.getTime
    )

    val effectivePriceTableSelector: FullPriceTable ⇒ EffectivePriceTable = fullPriceTable ⇒ {
      this.selectEffectivePriceTable(
        fullPriceTable,
        chargingData,
        currentResourceEvent,
        referenceTimeslot,
        previousValue,
        totalCredits,
        _oldAccumulatingAmount,
        _newAccumulatingAmount
      )
    }

    val initialChargeslots = TimeslotComputations.computeInitialChargeslots(
      referenceTimeslot,
      resourceType,
      policyByTimeslot,
      agreementByTimeslot,
      effectivePriceTableSelector
    )

    val fullChargeslots = initialChargeslots.map {
      case chargeslot@Chargeslot(startMillis, stopMillis, unitPrice, _, _) ⇒
        val timeDeltaMillis = stopMillis - startMillis

        val (creditsToSubtract, explanation) = this.computeCreditsToSubtract(
          _oldTotalCredits,       // FIXME ??? Should recalculate ???
          _oldAccumulatingAmount, // FIXME ??? Should recalculate ???
          _newAccumulatingAmount, // FIXME ??? Should recalculate ???
          timeDeltaMillis,
          previousValue,
          currentValue,
          unitPrice,
          currentDetails
        )

        val newChargeslot = chargeslot.copyWithCreditsToSubtract(creditsToSubtract, explanation)
        newChargeslot
    }

    if(fullChargeslots.length == 0) {
      throw new AquariumInternalError("No chargeslots computed for resource event %s".format(currentResourceEvent.id))
    }

    val sumOfCreditsToSubtract = fullChargeslots.map(_.creditsToSubtract).sum
    val newTotalCredits = _oldTotalCredits - sumOfCreditsToSubtract

    val newWalletEntry = WalletEntry(
      userID,
      sumOfCreditsToSubtract,
      _oldTotalCredits,
      newTotalCredits,
      TimeHelpers.nowMillis(),
      referenceTimeslot,
      billingMonthInfo.year,
      billingMonthInfo.month,
      fullChargeslots,
      previousResourceEventOpt.map(List(_, currentResourceEvent)).getOrElse(List(currentResourceEvent)),
      resourceType,
      currentResourceEvent.isSynthetic
    )

    logger.debug("newWalletEntry = {}", newWalletEntry.toJsonString)

    walletEntryRecorder.apply(newWalletEntry)

    (1, newTotalCredits)
  }

  protected def removeChargingData[T: Manifest](
      chargingData: mutable.Map[String, Any],
      envKey: TypedKey[T]
  ) = {

    chargingData.remove(envKey.name).asInstanceOf[Option[T]]
  }

  protected def getChargingData[T: Manifest](
      chargingData: mutable.Map[String, Any],
      envKey: TypedKey[T]
  ) = {

    chargingData.get(envKey.name).asInstanceOf[Option[T]]
  }

  protected def setChargingData[T: Manifest](
      chargingData: mutable.Map[String, Any],
      envKey: TypedKey[T],
      value: T
  ) = {

    chargingData(envKey.name) = value
  }

  def selectEffectivePriceTable(
      fullPriceTable: FullPriceTable,
      chargingData: mutable.Map[String, Any],
      currentResourceEvent: ResourceEventModel,
      referenceTimeslot: Timeslot,
      previousValue: Double,
      totalCredits: Double,
      oldAccumulatingAmount: Double,
      newAccumulatingAmount: Double
  ): EffectivePriceTable = {

    val selectorPath = computeSelectorPath(
      chargingData,
      currentResourceEvent,
      referenceTimeslot,
      previousValue,
      totalCredits,
      oldAccumulatingAmount,
      newAccumulatingAmount
    )

    fullPriceTable.effectivePriceTableOfSelectorForResource(selectorPath, currentResourceEvent.safeResource)
  }

  /**
   * A generic implementation for charging a resource event.
   * TODO: Ditch this in favor of completely ahdoc behaviors.
   *
   * @param aquarium
   * @param currentResourceEvent
   * @param resourceType
   * @param billingMonthInfo
   * @param previousResourceEventOpt
   * @param userAgreements
   * @param chargingData
   * @param totalCredits
   * @param walletEntryRecorder
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
  ): (Int, Double) = {

    val currentResourceEventDebugInfo = rcDebugInfo(currentResourceEvent)

    val isBillable = this.isBillableEvent(currentResourceEvent)
    val retval = if(!isBillable) {
      // The resource event is not billable.
      Debug(logger, "Ignoring not billable %s", currentResourceEventDebugInfo)
      (0, totalCredits)
    } else {
      // The resource event is billable.
      // Find the previous event if needed.
      // This is (potentially) needed to calculate new credit amount and new resource instance amount
      if(this.needsPreviousEventForCreditAndAmountCalculation) {
        if(previousResourceEventOpt.isDefined) {
          val previousResourceEvent = previousResourceEventOpt.get
          val previousValue = previousResourceEvent.value

          Debug(logger, "I have previous event %s", previousResourceEvent.toDebugString)

          computeChargeslots(
            chargingData,
            previousResourceEventOpt,
            currentResourceEvent,
            billingMonthInfo,
            Timeslot(previousResourceEvent.occurredMillis, currentResourceEvent.occurredMillis),
            resourceType,
            userAgreements.agreementByTimeslot,
            previousValue,
            totalCredits,
            aquarium.policyStore,
            walletEntryRecorder
          )
        } else {
          // We do not have the needed previous event, so this must be the first resource event of its kind, ever.
          // Let's see if we can create a dummy previous event.
          val actualFirstEvent = currentResourceEvent

          // FIXME: Why && ?
          if(this.isBillableFirstEvent(actualFirstEvent) && this.mustGenerateDummyFirstEvent) {
            Debug(logger, "First event of its kind %s", currentResourceEventDebugInfo)

            val dummyFirst = this.constructDummyFirstEventFor(currentResourceEvent, billingMonthInfo.monthStartMillis)
            Debug(logger, "Dummy first event %s", dummyFirst.toDebugString)

            val previousResourceEvent = dummyFirst
            val previousValue = previousResourceEvent.value

            computeChargeslots(
              chargingData,
              Some(previousResourceEvent),
              currentResourceEvent,
              billingMonthInfo,
              Timeslot(previousResourceEvent.occurredMillis, currentResourceEvent.occurredMillis),
              resourceType,
              userAgreements.agreementByTimeslot,
              previousValue,
              totalCredits,
              aquarium.policyStore,
              walletEntryRecorder
            )
          } else {
            Debug(logger, "Ignoring first event of its kind %s", currentResourceEventDebugInfo)
            // userStateWorker.updateIgnored(currentResourceEvent)
            (0, totalCredits)
          }
        }
      } else {
        // No need for previous event. One event does it all.
        computeChargeslots(
          chargingData,
          None,
          currentResourceEvent,
          billingMonthInfo,
          Timeslot(currentResourceEvent.occurredMillis, currentResourceEvent.occurredMillis + 1),
          resourceType,
          userAgreements.agreementByTimeslot,
          this.getResourceInstanceUndefinedAmount,
          totalCredits,
          aquarium.policyStore,
          walletEntryRecorder
        )
      }
    }

    retval
  }

  /**
   * Generate a map where the key is a [[gr.grnet.aquarium.charging.ChargingInput]]
   * and the value the respective value. This map will be used to do the actual credit charge calculation
   * by the respective algorithm.
   *
   * Values are obtained from a corresponding context, which is provided by the parameters. We assume that this context
   * has been validated before the call to `makeValueMap` is made.
   *
   * @param totalCredits   the value for [[gr.grnet.aquarium.charging.TotalCreditsInput.]]
   * @param oldTotalAmount the value for [[gr.grnet.aquarium.charging.OldTotalAmountInput]]
   * @param newTotalAmount the value for [[gr.grnet.aquarium.charging.NewTotalAmountInput]]
   * @param timeDelta      the value for [[gr.grnet.aquarium.charging.TimeDeltaInput]]
   * @param previousValue  the value for [[gr.grnet.aquarium.charging.PreviousValueInput]]
   * @param currentValue   the value for [[gr.grnet.aquarium.charging.CurrentValueInput]]
   * @param unitPrice      the value for [[gr.grnet.aquarium.charging.UnitPriceInput]]
   *
   * @return a map from [[gr.grnet.aquarium.charging.ChargingInput]]s to respective values.
   */
  def makeValueMap(
      totalCredits: Double,
      oldTotalAmount: Double,
      newTotalAmount: Double,
      timeDelta: Double,
      previousValue: Double,
      currentValue: Double,
      unitPrice: Double
  ): Map[ChargingInput, Any] = {

    ChargingBehavior.makeValueMapFor(
      this,
      totalCredits,
      oldTotalAmount,
      newTotalAmount,
      timeDelta,
      previousValue,
      currentValue,
      unitPrice)
  }

  def needsPreviousEventForCreditAndAmountCalculation: Boolean = {
    // If we need any variable that is related to the previous event
    // then we do need a previous event
    inputs.exists(_.isDirectlyRelatedToPreviousEvent)
  }

  /**
   * Given the old amount of a resource instance, the value arriving in a new resource event and the new details,
   * compute the new instance amount.
   *
   * Note that the `oldAmount` does not make sense for all types of [[gr.grnet.aquarium.charging.ChargingBehavior]],
   * in which case it is ignored.
   *
   * @param oldAccumulatingAmount     the old accumulating amount
   * @param newEventValue the value contained in a newly arrived
   *                      [[gr.grnet.aquarium.event.model.resource.ResourceEventModel]]
   * @param newDetails       the `details` of the newly arrived
   *                      [[gr.grnet.aquarium.event.model.resource.ResourceEventModel]]
   * @return
   */
  def computeNewAccumulatingAmount(
      oldAccumulatingAmount: Double,
      newEventValue: Double,
      newDetails: Map[String, String]
  ): Double

  /**
   * The initial amount.
   */
  def getResourceInstanceInitialAmount: Double

  /**
   * The amount used when no amount is meant to be relevant.
   *
   * For example, when there is no need for a previous event but an API requires the amount of the previous event.
   *
   * Normally, this value will never be used by client code (= charge computation code).
   */
  def getResourceInstanceUndefinedAmount: Double = Double.NaN

  /**
   * An event carries enough info to characterize it as billable or not.
   *
   * Typically all events are billable by default and indeed this is the default implementation
   * provided here.
   *
   * The only exception to the rule is ON events for [[gr.grnet.aquarium.charging.VMChargingBehavior]].
   */
  def isBillableEvent(event: ResourceEventModel): Boolean = true

  /**
   * This is called when we have the very first event for a particular resource instance, and we want to know
   * if it is billable or not.
   */
  def isBillableFirstEvent(event: ResourceEventModel): Boolean

  def mustGenerateDummyFirstEvent: Boolean

  def dummyFirstEventValue: Double = 0.0 // FIXME read from configuration

  def constructDummyFirstEventFor(actualFirst: ResourceEventModel, newOccurredMillis: Long): ResourceEventModel = {
    if(!mustGenerateDummyFirstEvent) {
      throw new AquariumInternalError("constructDummyFirstEventFor() Not compliant with %s", this)
    }

    val newDetails = Map(
      ResourceEventModel.Names.details_aquarium_is_synthetic   -> "true",
      ResourceEventModel.Names.details_aquarium_is_dummy_first -> "true",
      ResourceEventModel.Names.details_aquarium_reference_event_id -> actualFirst.id,
      ResourceEventModel.Names.details_aquarium_reference_event_id_in_store -> actualFirst.stringIDInStoreOrEmpty
    )

    actualFirst.withDetailsAndValue(newDetails, dummyFirstEventValue, newOccurredMillis)
  }

  /**
   * There are resources (cost policies) for which implicit events must be generated at the end of the billing period
   * and also at the beginning of the next one. For these cases, this method must return `true`.
   *
   * The motivating example comes from the [[gr.grnet.aquarium.charging.VMChargingBehavior]] for which we
   * must implicitly assume `OFF` events at the end of the billing period and `ON` events at the beginning of the next
   * one.
   *
   */
  def supportsImplicitEvents: Boolean

  def mustConstructImplicitEndEventFor(resourceEvent: ResourceEventModel): Boolean

  @throws(classOf[Exception])
  def constructImplicitEndEventFor(resourceEvent: ResourceEventModel, newOccurredMillis: Long): ResourceEventModel
}
