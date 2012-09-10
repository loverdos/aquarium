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
import gr.grnet.aquarium.util.Loggable
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.policy._
import collection.immutable
import gr.grnet.aquarium.policy.EffectiveUnitPriceModel
import gr.grnet.aquarium.message.avro.gen.{EffectiveUnitPriceMsg, PolicyMsg, UserAgreementMsg, FullPriceTableMsg, EffectivePriceTableMsg, ChargeslotMsg}

/**
 * Methods for converting accounting events to wallet entries.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object TimeslotComputations extends Loggable {

  /**
   * Breaks a reference timeslot (e.g. billing period) according to policies and agreements.
   *
   * @param referenceTimeslot
   * @param policyTimeslots
   * @param agreementTimeslots
   * @return
   */
  private[this] def splitTimeslotByPoliciesAndAgreements(
      referenceTimeslot: Timeslot,
      policyTimeslots: List[Timeslot],
      agreementTimeslots: List[Timeslot]
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
  private[this] def resolveEffectiveUnitPrices(
      alignedTimeslot: Timeslot,
      policy: PolicyModel,
      agreement: UserAgreementModel,
      fullPriceTableModelGetter: (UserAgreementModel, PolicyModel) ⇒ FullPriceTableModel,
      effectivePriceTableModelSelector: FullPriceTableModel ⇒ EffectivePriceTableModel
  ): SortedMap[Timeslot, Double] = {

    // Note that most of the code is taken from calcChangeChunks()
    val ret = resolveEffectiveUnitPricesForTimeslot(
      alignedTimeslot,
      policy,
      agreement,
      fullPriceTableModelGetter,
      effectivePriceTableModelSelector)
    ret map {case (t,p) => (t,p.unitPrice)}
  }

  def computeInitialChargeslots(
      referenceTimeslot: Timeslot,
      policyByTimeslot: SortedMap[Timeslot, PolicyModel],
      agreementByTimeslot: SortedMap[Timeslot, UserAgreementModel],
      fullPriceTableModelGetter: (UserAgreementModel, PolicyModel) ⇒ FullPriceTableModel,
      effectivePriceTableModelSelector: FullPriceTableModel ⇒ EffectivePriceTableModel
  ): List[ChargeslotMsg] = {

    val policyTimeslots = policyByTimeslot.keySet
    val agreementTimeslots = agreementByTimeslot.keySet

    def getPolicyWithin(ts: Timeslot): PolicyModel = {
      policyByTimeslot.find(_._1.contains(ts)).get._2
    }
    def getAgreementWithin(ts: Timeslot): UserAgreementModel = {
      agreementByTimeslot.find(_._1.contains(ts)).get._2
    }

    // 1. Round ONE: split time according to overlapping policies and agreements.
    //val alignedTimeslots = List(referenceTimeslot) //splitTimeslotByPoliciesAndAgreements(referenceTimeslot, policyTimeslots.toList, agreementTimeslots.toList, Just(clog))
    val alignedTimeslots = splitTimeslotByPoliciesAndAgreements(referenceTimeslot, policyTimeslots.toList, agreementTimeslots.toList)

    // 2. Round TWO: Use the aligned timeslots of Round ONE to produce even more
    //    fine-grained timeslots according to applicable algorithms.
    //    Then pack the info into charge slots.
    //    clog.begin("ROUND 2")
    val allChargeslots = for {
      alignedTimeslot <- alignedTimeslots
    } yield {
      //val policy = policyByTimeslot.valuesIterator.next()//getPolicyWithin(alignedTimeslot)
      val policy = getPolicyWithin(alignedTimeslot)
      //      clog.debug("dslPolicy = %s", dslPolicy)
      //val userAgreement = agreementByTimeslot.valuesIterator.next()//getAgreementWithin(alignedTimeslot)
      val userAgreement = getAgreementWithin(alignedTimeslot)

      val unitPriceByTimeslot = resolveEffectiveUnitPrices(
        alignedTimeslot,
        policy,
        userAgreement,
        fullPriceTableModelGetter,
        effectivePriceTableModelSelector
      )

      // Now, the timeslots must be the same
      val finegrainedTimeslots = unitPriceByTimeslot.keySet

      val chargeslots = for(finegrainedTimeslot ← finegrainedTimeslots) yield {
        val cs = new ChargeslotMsg

        cs.setStartMillis(finegrainedTimeslot.from.getTime)
        cs.setStopMillis(finegrainedTimeslot.to.getTime)
        cs.setUnitPrice(unitPriceByTimeslot(finegrainedTimeslot))

        cs
      }

      chargeslots.toList
    }

    val result = allChargeslots.flatten

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
  private[this] def alignTimeslots(
      a: List[Timeslot],
      b: List[Timeslot]
  ): List[Timeslot] = {

    def safeTail(foo: List[Timeslot]) = foo match {
      case Nil => List()
      case x :: Nil => List()
      case x :: rest => rest
    }

    if(a.isEmpty) return b
    if(b.isEmpty) return a
    if(a.head.from != b.head.from)
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

    type PriceMap =  immutable.SortedMap[Timeslot, EffectiveUnitPriceModel]
    private type PriceList = List[EffectiveUnitPriceModel]
    private def emptyMap = immutable.SortedMap[Timeslot,EffectiveUnitPriceModel]()

    /**
     * Resolves the effective price list for each chunk of the
     * provided timeslot and returns it as a Map
     */
    private[this] def resolveEffectiveUnitPricesForTimeslot(
        alignedTimeslot: Timeslot,
        policy: PolicyModel,
        userAgreement: UserAgreementModel,
        fullPriceTableModelGetter: (UserAgreementModel, PolicyModel) ⇒ FullPriceTableModel,
        effectivePriceTableModelSelector: FullPriceTableModel ⇒ EffectivePriceTableModel
    ): PriceMap = {

      val fullPriceTable = fullPriceTableModelGetter(userAgreement, policy)
      val effectivePriceTable = effectivePriceTableModelSelector(fullPriceTable)

      resolveEffective(alignedTimeslot, effectivePriceTable.priceOverrides)
      //immutable.SortedMap(alignedTimeslot -> effectivePriceTable.priceOverrides.head)
    }

  private[this] def printPriceList(p: PriceList) : Unit = {
      Console.err.println("BEGIN PRICE LIST")
      for { p1 <- p } Console.err.println(p1)
      Console.err.println("END PRICE LIST")
    }

  private[this] def printPriceMap(m: PriceMap) = {
      Console.err.println("BEGIN PRICE MAP")
      for { (t,p) <- m.toList } Console.err.println("Timeslot " + t + "\t\t" + p)
      Console.err.println("END PRICE MAP")
    }

  private[this] def resolveEffective(alignedTimeslot: Timeslot,p:PriceList): PriceMap = {
      //Console.err.println("\n\nInput timeslot: " + alignedTimeslot + "\n\n")
      //printPriceList(p)
      val ret =  resolveEffective3(alignedTimeslot,p) //HERE
      //printPriceMap(ret)
      ret
    }


  private[this] def resolveEffective3(alignedTimeslot: Timeslot, effectiveUnitPrices: PriceList): PriceMap =
      effectiveUnitPrices match {
        case Nil =>
          emptyMap
        case hd::tl =>
          val (satisfied,notSatisfied) = hd splitTimeslot alignedTimeslot
          val satisfiedMap = satisfied.foldLeft (emptyMap)  {(map,t) =>
          //Console.err.println("Adding timeslot" + t +
          // " for policy " + policy.name)
            map + ((t,hd))
          }
          val notSatisfiedMap = notSatisfied.foldLeft (emptyMap) {(map,t) =>
            val otherMap = resolveEffective3(t,tl)
            //Console.err.println("Residual timeslot: " + t)
            val ret = map ++ otherMap
            ret
          }
          val ret = satisfiedMap ++ notSatisfiedMap
          ret
      }
}
