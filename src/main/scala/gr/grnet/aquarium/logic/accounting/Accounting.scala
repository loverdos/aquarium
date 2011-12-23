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

package gr.grnet.aquarium.logic.accounting

import dsl._
import gr.grnet.aquarium.logic.events.{WalletEntry, ResourceEvent}
import collection.immutable.SortedMap
import gr.grnet.aquarium.MasterConf._
import gr.grnet.aquarium.util.Loggable
import com.ckkloverdos.maybe.{Maybe, Failed, NoVal, Just}

/**
 * 
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait Accounting extends DSLUtils with Loggable {
  
  def chargeEvent(ev: ResourceEvent) : Maybe[List[WalletEntry]] = {

    if (!ev.validate())
      Failed(new AccountingException("Event not valid"))

    val userState = MasterConf.userStore.findUserStateByUserId(ev.userId) match {
      case Just(x) => x
      case NoVal =>
        return Failed(new AccountingException("Inexistent user: %s".format(ev.toJson)))
      case Failed(x,y) =>
        return Failed(new AccountingException("Error retrieving user state: %s".format(x)))
    }

    val agreement = userState.agreement.data.name

    val agr = Policy.policy.findAgreement(agreement) match {
      case Some(x) => x
      case None => return Failed(
        new AccountingException("Cannot find agreement:%s".format(agreement)))
    }

    val resource = Policy.policy.findResource(ev.resource).get

    //val chargeChunks = calcChangeChunks(agr, ev.value, ev.resource, )


    Just(List(WalletEntry.zero))
  }

  private[logic] case class ChargeChunk(value: Float, algorithm: String,
                                        price: Float, when: Timeslot) {
    assert(value > 0)
    assert(!algorithm.isEmpty)

    def cost(): Float = {
      //TODO: Apply the algorithm when we start parsing it
      value * price
    }
  }

  private def calcChangeChunks(agr: DSLAgreement, volume: Float,
                               res: DSLResource, t: Timeslot): List[ChargeChunk] = {

    val alg = resolveEffectiveAlgorithmsForTimeslot(t, agr)
    val pri = resolveEffectivePricelistsForTimeslot(t, agr)
    val chunks = splitChargeChunks(alg, pri)

    val algChunked = chunks._1
    val priChunked = chunks._2

    assert(algChunked.size == priChunked.size)
    val totalTime = t.from.getTime - t.to.getTime
    algChunked.keySet.map{
      x =>
        val amount = volume * (totalTime / (x.to.getTime - x.from.getTime))
        new ChargeChunk(amount,
          algChunked.get(x).get.algorithms.getOrElse(res, ""),
          priChunked.get(x).get.prices.getOrElse(res, 0F), x)
    }.toList
  }

  /**
   * Align charge timeslots between algorithms and pricelists. As algorithm
   * and pricelists can have different effectivity periods, this method
   * examines them and splits them as necessary.
   */
  private[logic] def splitChargeChunks(alg: SortedMap[Timeslot, DSLAlgorithm],
                        price: SortedMap[Timeslot, DSLPriceList]) :
    (Map[Timeslot, DSLAlgorithm], Map[Timeslot, DSLPriceList]) = {

    val zipped = alg.keySet.zip(price.keySet)

    zipped.find(p => !p._1.equals(p._2)) match {
      case None => (alg, price)
      case Some(x) =>
        val algTimeslot = x._1
        val priTimeslot = x._2

        assert(algTimeslot.from == priTimeslot.from)

        if (algTimeslot.endsAfter(priTimeslot)) {
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
}

/** An exception raised when something goes wrong with accounting */
class AccountingException(msg: String) extends Exception(msg)