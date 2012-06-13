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
import gr.grnet.aquarium.logic.accounting.algorithm.CostPolicyAlgorithmCompiler
import gr.grnet.aquarium.logic.accounting.dsl.{DSL, DSLResourcesMap, DSLPolicy, DSLResource, DSLPriceList, DSLAlgorithm, DSLAgreement, Timeslot, DSLUtils}

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
  def splitTimeslotByPoliciesAndAgreements(referenceTimeslot: Timeslot,
                                           policyTimeslots: List[Timeslot],
                                           agreementTimeslots: List[Timeslot],
                                           clogM: Maybe[ContextualLogger] = NoVal): List[Timeslot] = {

    //    val clog = ContextualLogger.fromOther(clogM, logger, "splitTimeslotByPoliciesAndAgreements()")
    //    clog.begin()

    // Align policy and agreement validity timeslots to the referenceTimeslot
    val alignedPolicyTimeslots = referenceTimeslot.align(policyTimeslots)
    val alignedAgreementTimeslots = referenceTimeslot.align(agreementTimeslots)

    //    clog.debug("referenceTimeslot = %s", referenceTimeslot)
    //    clog.debugSeq("alignedPolicyTimeslots", alignedPolicyTimeslots, 0)
    //    clog.debugSeq("alignedAgreementTimeslots", alignedAgreementTimeslots, 0)

    val result = alignTimeslots(alignedPolicyTimeslots, alignedAgreementTimeslots)
    //    clog.debugSeq("result", result, 1)
    //    clog.end()
    result
  }

  /**
   * Given a reference timeslot, we have to break it up to a series of timeslots where a particular
   * algorithm and price unit is in effect.
   *
   */
  protected
  def resolveEffectiveAlgorithmsAndPriceLists(alignedTimeslot: Timeslot,
                                              agreement: DSLAgreement,
                                              clogOpt: Option[ContextualLogger] = None):
  (Map[Timeslot, DSLAlgorithm], Map[Timeslot, DSLPriceList]) = {

    val clog = ContextualLogger.fromOther(clogOpt, logger, "resolveEffectiveAlgorithmsAndPriceLists()")

    // Note that most of the code is taken from calcChangeChunks()
    val alg = dslUtils.resolveEffectiveAlgorithmsForTimeslot(alignedTimeslot, agreement)
    val pri = dslUtils.resolveEffectivePricelistsForTimeslot(alignedTimeslot, agreement)
    val chargeChunks = splitChargeChunks(alg, pri)
    val algorithmByTimeslot = chargeChunks._1
    val pricelistByTimeslot = chargeChunks._2

    assert(algorithmByTimeslot.size == pricelistByTimeslot.size)

    (algorithmByTimeslot, pricelistByTimeslot)
  }

  protected
  def computeInitialChargeslots(referenceTimeslot: Timeslot,
                                dslResource: DSLResource,
                                policiesByTimeslot: Map[Timeslot, DSLPolicy],
                                agreementNamesByTimeslot: Map[Timeslot, String],
                                clogOpt: Option[ContextualLogger] = None): List[Chargeslot] = {

    val clog = ContextualLogger.fromOther(clogOpt, logger, "computeInitialChargeslots()")
    //    clog.begin()

    val policyTimeslots = policiesByTimeslot.keySet
    val agreementTimeslots = agreementNamesByTimeslot.keySet

    //    clog.debugMap("policiesByTimeslot", policiesByTimeslot, 1)
    //    clog.debugMap("agreementNamesByTimeslot", agreementNamesByTimeslot, 1)

    def getPolicy(ts: Timeslot): DSLPolicy = {
      policiesByTimeslot.find(_._1.contains(ts)).get._2
    }
    def getAgreementName(ts: Timeslot): String = {
      agreementNamesByTimeslot.find(_._1.contains(ts)).get._2
    }

    // 1. Round ONE: split time according to overlapping policies and agreements.
    //    clog.begin("ROUND 1")
    val alignedTimeslots = splitTimeslotByPoliciesAndAgreements(referenceTimeslot, policyTimeslots.toList, agreementTimeslots.toList, Just(clog))
    //    clog.debugSeq("alignedTimeslots", alignedTimeslots, 1)
    //    clog.end("ROUND 1")

    // 2. Round TWO: Use the aligned timeslots of Round ONE to produce even more
    //    fine-grained timeslots according to applicable algorithms.
    //    Then pack the info into charge slots.
    //    clog.begin("ROUND 2")
    val allChargeslots = for {
      alignedTimeslot <- alignedTimeslots
    } yield {
      //      val alignedTimeslotMsg = "alignedTimeslot = %s".format(alignedTimeslot)
      //      clog.begin(alignedTimeslotMsg)

      val dslPolicy = getPolicy(alignedTimeslot)
      //      clog.debug("dslPolicy = %s", dslPolicy)
      val agreementName = getAgreementName(alignedTimeslot)
      //      clog.debug("agreementName = %s", agreementName)
      val agreementOpt = dslPolicy.findAgreement(agreementName)
      //      clog.debug("agreementOpt = %s", agreementOpt)

      agreementOpt match {
        case None ⇒
          val errMsg = "Unknown agreement %s during %s".format(agreementName, alignedTimeslot)
          clog.error("%s", errMsg)
          throw new AquariumException(errMsg)

        case Some(agreement) ⇒
          // TODO: Factor this out, just like we did with:
          // TODO:  val alignedTimeslots = splitTimeslotByPoliciesAndAgreements
          // Note that most of the code is already taken from calcChangeChunks()
          val r = resolveEffectiveAlgorithmsAndPriceLists(alignedTimeslot, agreement, Some(clog))
          val algorithmByTimeslot: Map[Timeslot, DSLAlgorithm] = r._1
          val pricelistByTimeslot: Map[Timeslot, DSLPriceList] = r._2

          // Now, the timeslots must be the same
          val finegrainedTimeslots = algorithmByTimeslot.keySet

          val chargeslots = for {
            finegrainedTimeslot <- finegrainedTimeslots
          } yield {
            //            val finegrainedTimeslotMsg = "finegrainedTimeslot = %s".format(finegrainedTimeslot)
            //            clog.begin(finegrainedTimeslotMsg)

            val dslAlgorithm = algorithmByTimeslot(finegrainedTimeslot) // TODO: is this correct?
            //            clog.debug("dslAlgorithm = %s", dslAlgorithm)
            //            clog.debugMap("dslAlgorithm.algorithms", dslAlgorithm.algorithms, 1)
            val dslPricelist = pricelistByTimeslot(finegrainedTimeslot) // TODO: is this correct?
            //            clog.debug("dslPricelist = %s", dslPricelist)
            //            clog.debug("dslResource = %s", dslResource)
            val algorithmDefOpt = dslAlgorithm.algorithms.get(dslResource)
            //            clog.debug("algorithmDefOpt = %s", algorithmDefOpt)
            val priceUnitOpt = dslPricelist.prices.get(dslResource)
            //            clog.debug("priceUnitOpt = %s", priceUnitOpt)

            val chargeslot = (algorithmDefOpt, priceUnitOpt) match {
              case (None, None) ⇒
                throw new AquariumException(
                  "Unknown algorithm and price unit for resource %s during %s".
                    format(dslResource, finegrainedTimeslot))
              case (None, _) ⇒
                throw new AquariumException(
                  "Unknown algorithm for resource %s during %s".
                    format(dslResource, finegrainedTimeslot))
              case (_, None) ⇒
                throw new AquariumException(
                  "Unknown price unit for resource %s during %s".
                    format(dslResource, finegrainedTimeslot))
              case (Some(algorithmDefinition), Some(priceUnit)) ⇒
                Chargeslot(finegrainedTimeslot.from.getTime, finegrainedTimeslot.to.getTime, algorithmDefinition, priceUnit)
            }

            //            clog.end(finegrainedTimeslotMsg)
            chargeslot
          }

          //          clog.end(alignedTimeslotMsg)
          chargeslots.toList
      }
    }
    //    clog.end("ROUND 2")


    val result = allChargeslots.flatten
    //    clog.debugSeq("result", allChargeslots, 1)
    //    clog.end()
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
      dslResource: DSLResource,
      defaultResourceMap: DSLResourcesMap,
      agreementNamesByTimeslot: SortedMap[Timeslot, String],
      algorithmCompiler: CostPolicyAlgorithmCompiler,
      policyStore: PolicyStore,
      clogOpt: Option[ContextualLogger] = None
  ): (Timeslot, List[Chargeslot]) = {

    val clog = ContextualLogger.fromOther(clogOpt, logger, "computeFullChargeslots()")
    //    clog.begin()

    val occurredDate = currentResourceEvent.occurredDate
    val occurredMillis = currentResourceEvent.occurredMillis
    val costPolicy = dslResource.costPolicy

    val dsl = new DSL {}
    val (referenceTimeslot, relevantPolicies, previousValue) = costPolicy.needsPreviousEventForCreditAndAmountCalculation match {
      // We need a previous event
      case true ⇒
        previousResourceEventOpt match {
          // We have a previous event
          case Some(previousResourceEvent) ⇒
            //            clog.debug("Have previous event")
            //            clog.debug("previousValue = %s", previousResourceEvent.value)

            val referenceTimeslot = Timeslot(previousResourceEvent.occurredDate, occurredDate)
            //            clog.debug("referenceTimeslot = %s".format(referenceTimeslot))

            // all policies within the interval from previous to current resource event
            //            clog.debug("Calling policyStore.loadAndSortPoliciesWithin(%s)", referenceTimeslot)
            val relevantPolicies = policyStore.loadAndSortPoliciesWithin(referenceTimeslot.from.getTime, referenceTimeslot.to.getTime, dsl)
            //            clog.debugMap("==> relevantPolicies", relevantPolicies, 0)

            (referenceTimeslot, relevantPolicies, previousResourceEvent.value)

          // We do not have a previous event
          case None ⇒
            throw new AquariumException(
              "Unable to charge. No previous event given for %s".
                format(currentResourceEvent.toDebugString))
        }

      // We do not need a previous event
      case false ⇒
        // ... so we cannot compute timedelta from a previous event, there is just one chargeslot
        // referring to (almost) an instant in time
        //        clog.debug("DO NOT have previous event")
        val previousValue = costPolicy.getResourceInstanceUndefinedAmount
        //        clog.debug("previousValue = costPolicy.getResourceInstanceUndefinedAmount = %s", previousValue)

        val referenceTimeslot = Timeslot(new MutableDateCalc(occurredDate).goPreviousMilli.toDate, occurredDate)
        //        clog.debug("referenceTimeslot = %s".format(referenceTimeslot))

        //        clog.debug("Calling policyStore.loadValidPolicyEntryAt(%s)", new MutableDateCalc(occurredMillis))
        val relevantPolicyOpt = policyStore.loadValidPolicyAt(occurredMillis, dsl)
        //        clog.debug("  ==> relevantPolicyM = %s", relevantPolicyM)

        val relevantPolicies = relevantPolicyOpt match {
          case Some(relevantPolicy) ⇒
            Map(referenceTimeslot -> relevantPolicy)

          case None ⇒
            throw new AquariumInternalError("No relevant policy found for %s".format(referenceTimeslot))
        }

        (referenceTimeslot, relevantPolicies, previousValue)
    }

    val initialChargeslots = computeInitialChargeslots(
      referenceTimeslot,
      dslResource,
      relevantPolicies,
      agreementNamesByTimeslot,
      Some(clog)
    )

    val fullChargeslots = initialChargeslots.map {
      case chargeslot@Chargeslot(startMillis, stopMillis, algorithmDefinition, unitPrice, _) ⇒
        val execAlgorithm = algorithmCompiler.compile(algorithmDefinition)
        val valueMap = costPolicy.makeValueMap(
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
        val credits = execAlgorithm.apply(valueMap)
        chargeslot.copyWithCredits(credits)
    }

    val result = referenceTimeslot -> fullChargeslots

    result
  }

  /**
   * Align charge timeslots between algorithms and pricelists. As algorithm
   * and pricelists can have different effectivity periods, this method
   * examines them and splits them as necessary.
   */
  private[computation] def splitChargeChunks(alg: SortedMap[Timeslot, DSLAlgorithm],
                                       price: SortedMap[Timeslot, DSLPriceList]):
  (Map[Timeslot, DSLAlgorithm], Map[Timeslot, DSLPriceList]) = {

    val zipped = alg.keySet.zip(price.keySet)

    zipped.find(p => !p._1.equals(p._2)) match {
      case None => (alg, price)
      case Some(x) =>
        val algTimeslot = x._1
        val priTimeslot = x._2

        assert(algTimeslot.from == priTimeslot.from)

        if(algTimeslot.endsAfter(priTimeslot)) {
          val slices = algTimeslot.slice(priTimeslot.to)
          val algo = alg.get(algTimeslot).get
          val newalg = alg - algTimeslot ++ Map(slices.apply(0) -> algo) ++ Map(slices.apply(1) -> algo)
          splitChargeChunks(newalg, price)
        }
        else {
          val slices = priTimeslot.slice(priTimeslot.to)
          val pl = price.get(priTimeslot).get
          val newPrice = price - priTimeslot ++ Map(slices.apply(0) -> pl) ++ Map(slices.apply(1) -> pl)
          splitChargeChunks(alg, newPrice)
        }
    }
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