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
import java.util.Date
import gr.grnet.aquarium.logic.events.{WalletEntry, ResourceEvent}
import collection.immutable.SortedMap

/**
 * 
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait Accounting extends DSLUtils {

  def chargeEvent(ev: ResourceEvent) : WalletEntry = {

    if (!ev.validate())
      throw new AccountingException("Event not valid")

    //From now own, we can trust the event contents
    val resource = Policy.policy.findResource(ev.resource).get

    WalletEntry.zero
  }

  private case class ChargeChunk(value: Float, algorithm: DSLAlgorithm,
                         priceList: DSLPriceList) {
    def cost(): Float = {
      0F
    }
  }

  private def calcChangeChunks(agr: DSLAgreement, volume: Float,
                       from: Date, to: Date) : List[ChargeChunk] = {

    resolveEffectiveAlgorithmsForTimeslot(Timeslot(from, to), agr)
    resolveEffectivePricelistsForTimeslot(Timeslot(from, to), agr)

    List()
  }

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