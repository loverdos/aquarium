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

import collection.immutable.SortedMap
import com.ckkloverdos.maybe.{NoVal, Maybe, Just}
import gr.grnet.aquarium.util.{ContextualLogger, Loggable}
import gr.grnet.aquarium.store.PolicyStore
import gr.grnet.aquarium.util.date.MutableDateCalc
import gr.grnet.aquarium.{AquariumInternalError, AquariumException}
import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import gr.grnet.aquarium.logic.accounting.algorithm.SimpleExecutableChargingBehaviorAlgorithm
import gr.grnet.aquarium.logic.accounting.dsl.{Timeslot, DSLUtils}
import gr.grnet.aquarium.policy.{EffectivePriceTable, PolicyModel, ResourceType, UserAgreementModel}

/**
 * Methods for converting accounting events to wallet entries.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
trait TimeslotComputations extends Loggable {
  // TODO: favour composition over inheritance until we decide what to do with DSLUtils (and TimeslotComputations).
  protected val dslUtils = new DSLUtils {}

  /**
   * Breaks a reference timeslot (e.g. billing period) according to policies and agreements.
   *
   * @param referenceTimeslot
   * @param policyTimeslots
   * @param agreementTimeslots
   * @return
   */
  protected
  def splitTimeslotByPoliciesAndAgreements(
      referenceTimeslot: Timeslot,
      policyTimeslots: List[Timeslot],
      agreementTimeslots: List[Timeslot],
      clogM: Maybe[ContextualLogger] = NoVal
  ): List[Timeslot] = {

    // Align policy and agreement validity timeslots to the referenceTimeslot
    val alignedPolicyTimeslots = referenceTimeslot.align(policyTimeslots)
    val alignedAgreementTimeslots = referenceTimeslot.align(agreementTimeslots)

    val result = alignTimeslots(alignedPolicyTimeslots, alignedAgreementTimeslots)

    result
  }

  /**
   * Given a reference timeslot, we have to break it up to a series of timeslots where a particular
   * algorithm and price unit is in effect.
   *
   */
  protected
  def resolveEffectiveUnitPrices(
      alignedTimeslot: Timeslot,
      policy: PolicyModel,
      agreement: UserAgreementModel,
      resourceType: ResourceType,
      clogOpt: Option[ContextualLogger] = None
  ): SortedMap[Timeslot, Double] = {

    val clog = ContextualLogger.fromOther(clogOpt, logger, "resolveEffectiveUnitPrices()")

    // Note that most of the code is taken from calcChangeChunks()
    dslUtils.resolveEffectiveUnitPricesForTimeslot(alignedTimeslot, policy, agreement, resourceType)
  }

  protected
  def computeInitialChargeslots(
      referenceTimeslot: Timeslot,
      resourceType: ResourceType,
      policyByTimeslot: SortedMap[Timeslot, PolicyModel],
      agreementByTimeslot: SortedMap[Timeslot, UserAgreementModel],
      clogOpt: Option[ContextualLogger] = None
  ): List[Chargeslot] = {

    val clog = ContextualLogger.fromOther(clogOpt, logger, "computeInitialChargeslots()")

    val policyTimeslots = policyByTimeslot.keySet
    val agreementTimeslots = agreementByTimeslot.keySet

    def getPolicyWithin(ts: Timeslot): PolicyModel = {
      policyByTimeslot.find(_._1.contains(ts)).get._2
    }
    def getAgreementWithin(ts: Timeslot): UserAgreementModel = {
      agreementByTimeslot.find(_._1.contains(ts)).get._2
    }

    // 1. Round ONE: split time according to overlapping policies and agreements.
    val alignedTimeslots = splitTimeslotByPoliciesAndAgreements(referenceTimeslot, policyTimeslots.toList, agreementTimeslots.toList, Just(clog))

    // 2. Round TWO: Use the aligned timeslots of Round ONE to produce even more
    //    fine-grained timeslots according to applicable algorithms.
    //    Then pack the info into charge slots.
    //    clog.begin("ROUND 2")
    val allChargeslots = for {
      alignedTimeslot <- alignedTimeslots
    } yield {
      val policy = getPolicyWithin(alignedTimeslot)
      //      clog.debug("dslPolicy = %s", dslPolicy)
      val userAgreement = getAgreementWithin(alignedTimeslot)

      // TODO: Factor this out, just like we did with:
      // TODO:  val alignedTimeslots = splitTimeslotByPoliciesAndAgreements
      // Note that most of the code is already taken from calcChangeChunks()
      val unitPriceByTimeslot = resolveEffectiveUnitPrices(alignedTimeslot, policy, userAgreement, resourceType, Some(clog))

      // Now, the timeslots must be the same
      val finegrainedTimeslots = unitPriceByTimeslot.keySet

      val chargeslots = for (finegrainedTimeslot ← finegrainedTimeslots) yield {
        Chargeslot(
          finegrainedTimeslot.from.getTime,
          finegrainedTimeslot.to.getTime,
          unitPriceByTimeslot(finegrainedTimeslot)
        )
      }

      chargeslots.toList
    }

    val result = allChargeslots.flatten

    result
  }

  /**
   * Compute the charge slots generated by a particular resource event.
   *
   */
  def computeFullChargeslots(
      previousResourceEventOpt: Option[ResourceEventModel],
      currentResourceEvent: ResourceEventModel,
      oldCredits: Double,
      oldTotalAmount: Double,
      newTotalAmount: Double,
      resourceType: ResourceType,
      agreementByTimeslot: SortedMap[Timeslot, UserAgreementModel],
      policyStore: PolicyStore,
      clogOpt: Option[ContextualLogger] = None
  ): (Timeslot, List[Chargeslot]) = {

    val clog = ContextualLogger.fromOther(clogOpt, logger, "computeFullChargeslots()")
    //    clog.begin()

    val occurredDate = currentResourceEvent.occurredDate
    val occurredMillis = currentResourceEvent.occurredMillis
    val chargingBehavior = resourceType.chargingBehavior

    val (referenceTimeslot, policyByTimeslot, previousValue) = chargingBehavior.needsPreviousEventForCreditAndAmountCalculation match {
      // We need a previous event
      case true ⇒
        previousResourceEventOpt match {
          // We have a previous event
          case Some(previousResourceEvent) ⇒
            val referenceTimeslot = Timeslot(previousResourceEvent.occurredDate, occurredDate)
            // all policies within the interval from previous to current resource event
            //            clog.debug("Calling policyStore.loadAndSortPoliciesWithin(%s)", referenceTimeslot)
            // TODO: store policies in mem?
            val policyByTimeslot = policyStore.loadAndSortPoliciesWithin(referenceTimeslot.from.getTime, referenceTimeslot.to.getTime)

            (referenceTimeslot, policyByTimeslot, previousResourceEvent.value)

          // We do not have a previous event
          case None ⇒
            throw new AquariumInternalError(
              "Unable to charge. No previous event given for %s".format(currentResourceEvent.toDebugString))
        }

      // We do not need a previous event
      case false ⇒
        // ... so we cannot compute timedelta from a previous event, there is just one chargeslot
        // referring to (almost) an instant in time
        val previousValue = chargingBehavior.getResourceInstanceUndefinedAmount

        val referenceTimeslot = Timeslot(new MutableDateCalc(occurredDate).goPreviousMilli.toDate, occurredDate)

        // TODO: store policies in mem?
        val relevantPolicyOpt: Option[PolicyModel] = policyStore.loadValidPolicyAt(occurredMillis)

        val policyByTimeslot = relevantPolicyOpt match {
          case Some(relevantPolicy) ⇒
            SortedMap(referenceTimeslot -> relevantPolicy)

          case None ⇒
            throw new AquariumInternalError("No relevant policy found for %s".format(referenceTimeslot))
        }

        (referenceTimeslot, policyByTimeslot, previousValue)
    }

    val initialChargeslots = computeInitialChargeslots(
      referenceTimeslot,
      resourceType,
      policyByTimeslot,
      agreementByTimeslot,
      Some(clog)
    )

    val fullChargeslots = initialChargeslots.map {
      case chargeslot@Chargeslot(startMillis, stopMillis, unitPrice, _) ⇒
        val valueMap = chargingBehavior.makeValueMap(
          oldCredits,
          oldTotalAmount,
          newTotalAmount,
          stopMillis - startMillis,
          previousValue,
          currentResourceEvent.value,
          unitPrice
        )

        //              clog.debug("execAlgorithm = %s", execAlgorithm)
        clog.debugMap("valueMap", valueMap, 1)

        // This is it
        val credits = SimpleExecutableChargingBehaviorAlgorithm.apply(valueMap)
        chargeslot.copyWithCredits(credits)
    }

    val result = referenceTimeslot -> fullChargeslots

    result
  }

  /**
   * Given two lists of timeslots, produce a list which contains the
   * set of timeslot slices, as those are defined by
   * timeslot overlaps.
   *
   * For example, given the timeslots a and b below, split them as shown.
   *
   * a = |****************|
   * ^                ^
   * a.from            a.to
   * b = |*********|
   * ^         ^
   * b.from     b.to
   *
   * result: List(Timeslot(a.from, b.to), Timeslot(b.to, a.to))
   */
  private[computation] def alignTimeslots(a: List[Timeslot],
                                    b: List[Timeslot]): List[Timeslot] = {

    def safeTail(foo: List[Timeslot]) = foo match {
      case Nil => List()
      case x :: Nil => List()
      case x :: rest => rest
    }

    if(a.isEmpty) return b
    if(b.isEmpty) return a

    assert(a.head.from == b.head.from)

    if(a.head.endsAfter(b.head)) {
      val slice = a.head.slice(b.head.to)
      slice.head :: alignTimeslots(slice.last :: a.tail, safeTail(b))
    } else if(b.head.endsAfter(a.head)) {
      val slice = b.head.slice(a.head.to)
      slice.head :: alignTimeslots(safeTail(a), slice.last :: b.tail)
    } else {
      a.head :: alignTimeslots(safeTail(a), safeTail(b))
    }
  }
}
