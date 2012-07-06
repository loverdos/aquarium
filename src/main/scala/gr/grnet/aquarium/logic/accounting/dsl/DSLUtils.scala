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

package gr.grnet.aquarium.logic.accounting.dsl

import java.util.{Date}
import scala.collection.immutable
import gr.grnet.aquarium.policy.{EffectiveUnitPrice, AdHocFullPriceTableRef, PolicyDefinedFullPriceTableRef, PolicyModel, ResourceType, EffectivePriceTable, UserAgreementModel}
import gr.grnet.aquarium.AquariumInternalError

/**
 * Utility functions to use when working with DSL types.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */

trait DSLUtils {
  /**
   * Resolves the effective price list for each chunk of the
   * provided timeslot and returns it as a Map
   */
  def resolveEffectiveUnitPricesForTimeslot(
      timeslot: Timeslot,
      policy: PolicyModel,
      agreement: UserAgreementModel,
      resourceType: ResourceType
  ): immutable.SortedMap[Timeslot, Double] = {

    val role = agreement.role
    val fullPriceTable = agreement.fullPriceTableRef match {
      case PolicyDefinedFullPriceTableRef ⇒
        policy.roleMapping.get(role) match {
          case Some(fullPriceTable) ⇒
            fullPriceTable

          case None ⇒
            throw new AquariumInternalError("Unknown role %s".format(role))
        }

      case AdHocFullPriceTableRef(fullPriceTable) ⇒
        fullPriceTable
    }

    val effectivePriceTable = fullPriceTable.perResource.get(resourceType.name) match {
      case None ⇒
        throw new AquariumInternalError("Unknown resource type %s".format(role))

      case Some(effectivePriceTable) ⇒
        effectivePriceTable
    }

    resolveEffective3(timeslot, effectivePriceTable)
  }


  def resolveEffective3(
      timeslot0: Timeslot,
      effectivePriceTable: EffectivePriceTable
  ): immutable.SortedMap[Timeslot, Double/*unit price*/] = {
//    assert(policy.toTimeslot contains timeslot0,"Policy does not contain timeslot")
    val timeslot = timeslot0 //TODO: timeslot0.align(5000)
    val subtimeslots_of_this_policy = Timeslot.mergeOverlaps(policy.effective intervalsOf timeslot)
    val subtimeslots_NOT_IN_this_policy = Timeslot.mergeOverlaps(timeslot.nonOverlappingTimeslots
                                                                                          (subtimeslots_of_this_policy))
    val policy_map =  subtimeslots_of_this_policy.foldLeft  (immutable.SortedMap[Timeslot, T]())
                                                            {(map,t) =>
                                                                  //Console.err.println("Adding timeslot" + t + " for policy " + policy.name)
                                                                  map + ((t,policy))
                                                            }
    val other_policy_map = policy.overrides match {
                                              case None =>
                                                 immutable.SortedMap[Timeslot, T]()
                                              case Some(parent_policy)=>
                                                  subtimeslots_NOT_IN_this_policy.foldLeft (
                                                      (immutable.SortedMap[Timeslot, T]()))
                                                      {(map,t) =>
                                                        //Console.err.println("Residual timeslot: " + t)
                                                        map ++ resolveEffective3(t,parent_policy)
                                                      }
                                            }
    val final_map = policy_map ++ other_policy_map
    final_map
  }

  /*def resolveEffective2[T <: DSLTimeBoundedItem[T]](timeslot0: Timeslot,policy: T):
  immutable.SortedMap[Timeslot, T] = {
    assert(policy.toTimeslot contains timeslot0,"Policy does not contain timeslot")

    /* generate mappings from timeslots -> policies
     * Algorithm: find next valid date (starting from timeslot.start) in this policy
     */
    val timeslot = timeslot0 //TODO: timeslot0.align(5000)
    def nextDate(d:Date,p:T) : Option[(Date,T,Boolean)] =
      (p.effective nextValidAfter d,p.overrides) match {
        case (None,None) => None
        case (None,Some(parent_policy)) =>
          val d1 = nextDate(d,parent_policy)
          d1
        case (Some(d1),_) => /* the next valid date cannot occur after the end of timeslot*/
           if (d1.before(timeslot.to)) Some((d1,p,true)) else Some((timeslot.to,p,false))
      }
    def genMap(map: immutable.SortedMap[Timeslot, T],d:Date) : immutable.SortedMap[Timeslot, T] = {
        val step = 1000L
        nextDate(d,policy) match {
        case None => map
        case Some((d1,policy,cont)) =>
          val t = Timeslot(d,d1)
          val map1 = map + (t -> policy)
          if(cont) genMap(map1,new Date(d1.getTime + step)) // 1 second after d1
          else map1 /* done */
      }
    }
    val map = genMap(immutable.SortedMap[Timeslot, T](),timeslot.from)
    map
  }

  /**
   * Splits the provided timeslot into chunks according to the validity
   * timeslots specified by the provided time bounded item. It
   * returns a map whose keys are the timeslot chunks and the values
   * correspond to the effective time bounded item (algorithm or pricelist).
   */
  def resolveEffective1[T <: DSLTimeBoundedItem[T]](timeslot: Timeslot,policy: T):
  immutable.SortedMap[Timeslot, T] = {
      assert(policy.toTimeslot contains timeslot,"Policy does not contain timeslot")

     /* Get a list of all effective/expanded policy timeslots within
        a timeslot specified by "variable timeslot".
      * NOTICE: The returned timeslots may be slightly out of "timeslot" bounds
      *         so we need to invoke overlappingTimeslots and nonOverlapping timeslots
      */
     val all_timeslots = allEffectiveTimeslots(policy.effective,timeslot)
     val timeslots_IN_policy = timeslot.overlappingTimeslots(all_timeslots)
     val timeslots_NOT_IN_policy = timeslot.nonOverlappingTimeslots(all_timeslots)

     val ret =  immutable.SortedMap[Timeslot, T]() ++
      /*add [timeslots -> policy] covered by this policy*/
      timeslots_IN_policy.flatMap {
         effective_timeslot =>  Map(effective_timeslot -> policy)
      } ++
      /*add [timeslots -> policy] covered by parent policies */
      timeslots_NOT_IN_policy.flatMap { /* search the policy hierarchy for effective timeslots not covered by this policy.*/
        not_effective_timeslot => policy.overrides match {
          case None => immutable.SortedMap[Timeslot, T]() /*Nothing to do. TODO: throw exception ?*/
          case Some(parent_policy) => resolveEffective1(not_effective_timeslot,parent_policy) /* search the policy hierarchy*/
        }
      }
    ret
  }*/

   /*
  /*
   * Get a list of all timeslots within which a timeframe
   * is effective, whithin the provided time bounds.
   */
  def allEffectiveTimeslots(spec: DSLTimeFrame, t: Timeslot):
  List[Timeslot] =
    if (spec.repeat.isEmpty) { //A  simple timeframe with no repetition defined
      val fromDate = if (spec.from.before(t.from)) t.from else spec.from
      val toDate = if (spec.to.getOrElse(t.to).after(t.to)) t.to else spec.to.getOrElse(t.to)
      List(Timeslot(fromDate, toDate))
    } /* otherwise for all repetitions determine timeslots*/
    else {
        val all =  for { r <- spec.repeat
                         ts <- effectiveTimeslots(r, t.from, t.to)
                      }  yield ts
      //for{ i <- all} Console.err.println(i)
        mergeOverlaps(all)
    }
  */



  /**
   * Merges overlapping timeslots.
   */

  /*/**
   * Get a list of all timeslots within which a time frame is active.
     The result is returned sorted by timeframe start date.
   */
  def effectiveTimeslots(spec: DSLTimeFrameRepeat, from: Date, to : Date):
    List[Timeslot] =
      for { (h1,h2) <- spec.start zip spec.end
            (d1,d2) <- DSLTimeSpec.expandTimeSpec(h1,h2,from,to)
          }
      yield Timeslot(d1,d2)
      */
}
