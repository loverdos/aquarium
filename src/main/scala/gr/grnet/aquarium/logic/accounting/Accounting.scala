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
import com.ckkloverdos.maybe.{Maybe, Failed, Just}
import java.util.Date
import gr.grnet.aquarium.util.{CryptoUtils, Loggable}

/**
 * Methods for converting accounting events to wallet entries.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait Accounting extends DSLUtils with Loggable {

  /**
   * Creates a list of wallet entries by applying the agreement provisions on
   * the resource state
   *
   * @param ev The resource event to create charges for
   * @param agreement The agreement applicable to the user mentioned in the event
   * @param resState The current state of the resource
   * @param lastUpdate The last time the resource state was updated
   */
  def chargeEvent(ev: ResourceEvent,
                  agreement: DSLAgreement,
                  resState: Any,
                  lastUpdate: Date):
  Maybe[List[WalletEntry]] = {

    if (!ev.validate())
      Failed(new AccountingException("Event not valid"))

    val resource = Policy.policy.findResource(ev.resource) match {
      case Some(x) => x
      case None => return Failed(
        new AccountingException("No resource [%s]".format(ev.resource)))
    }

    val amount = resource.isComplex match {
      case true => 0
      case false => 1
    }

    val chargeChunks = calcChangeChunks(agreement, amount, resource,
      Timeslot(lastUpdate, new Date(ev.occurredMillis)))

    val entries = chargeChunks.map {
      c =>
        WalletEntry(
          id = CryptoUtils.sha1(c.id),
          occurredMillis = lastUpdate.getTime,
          receivedMillis = System.currentTimeMillis(),
          sourceEventIDs = List(ev.id),
          value = c.cost,
          reason = c.reason,
          userId = ev.userId,
          finalized = true
        )
    }
    Just(entries)
  }

 def calcChangeChunks(agr: DSLAgreement, volume: Float,
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
        ChargeChunk(amount,
          algChunked.get(x).get.algorithms.getOrElse(res, ""),
          priChunked.get(x).get.prices.getOrElse(res, 0F), x, res)
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

case class ChargeChunk(value: Float, algorithm: String,
                       price: Float, when: Timeslot,
                       resource: DSLResource) {
  assert(value > 0)
  assert(!algorithm.isEmpty)
  assert(resource != null)

  def cost(): Float = {
    //TODO: Apply the algorithm when we start parsing it
    value * price
  }

  def reason(): String =
    "%d %s of %s from %s to %s @ %d/%s".format(value, resource.unit,
      resource.name, when.from, when.to, price, resource.unit)

  def id(): String =
    "%d%s%d%s%s%d".format(value, algorithm, price, when.toString,
      resource.name, System.currentTimeMillis())
}

/** An exception raised when something goes wrong with accounting */
class AccountingException(msg: String) extends Exception(msg)