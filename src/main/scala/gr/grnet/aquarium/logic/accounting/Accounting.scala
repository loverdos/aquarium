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
   * the resource state.
   *
   * @param ev The resource event to create charges for
   * @param agreement The agreement applicable to the user mentioned in the event
   * @param resState The current state of the resource
   * @param lastUpdate The last time the resource state was updated
   */
  def chargeEvent(ev: ResourceEvent,
                  agreement: DSLAgreement,
                  resState: Float,
                  lastUpdate: Date,
                  related: List[WalletEntry]):
  Maybe[List[WalletEntry]] = {

    assert(lastUpdate.getTime <= ev.occurredMillis)

    if (!ev.validate())
      return Failed(new AccountingException("Event not valid"))

    val policy = Policy.policy
    val resource = policy.findResource(ev.resource) match {
      case Some(x) => x
      case None => return Failed(
        new AccountingException("No resource [%s]".format(ev.resource)))
    }

    val amount = resource.costpolicy match {
      case ContinuousCostPolicy => resState.asInstanceOf[Float]
      case DiscreteCostPolicy => ev.value
      case OnOffCostPolicy =>
        OnOffPolicyResourceState(resState) match {
          case OnResourceState =>
            OnOffPolicyResourceState(ev.value) match {
              case OnResourceState =>
                return Failed(new AccountingException(("Resource state " +
                  "transition error(was:%s, now:%s)").format(OnResourceState,
                  OnResourceState)))
              case OffResourceState => 0
            }
          case OffResourceState =>
            OnOffPolicyResourceState(ev.value) match {
              case OnResourceState => 1
              case OffResourceState =>
                return Failed(new AccountingException(("Resource state " +
                  "transition error(was:%s, now:%s)").format(OffResourceState,
                  OffResourceState)))
            }
        }
    }

    // We don't do strict checking for all cases for OnOffPolicies as
    // above, since this point won't be reached in case of error.
    val isFinal = resource.costpolicy match {
      case OnOffCostPolicy =>
        OnOffPolicyResourceState(resState) match {
          case OnResourceState => false
          case OffResourceState => true
        }
      case _ => true
    }

    val ts = resource.costpolicy match {
      case DiscreteCostPolicy => Timeslot(new Date(ev.occurredMillis),
        new Date(ev.occurredMillis + 1))
      case _ => Timeslot(lastUpdate, new Date(ev.occurredMillis))
    }

    val chargeChunks = calcChangeChunks(agreement, amount, resource, ts)

    val timeReceived = System.currentTimeMillis

    val rel = related.map{x => x.sourceEventIDs}.flatten ++ List(ev.id)

    val entries = chargeChunks.map {
      c =>
        WalletEntry(
          id = CryptoUtils.sha1(c.id),
          occurredMillis = ev.occurredMillis,
          receivedMillis = timeReceived,
          sourceEventIDs = rel,
          value = c.cost,
          reason = c.reason,
          userId = ev.userId,
          resource = ev.resource,
          instanceId = ev.getInstanceId(policy),
          finalized = isFinal
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

    res.costpolicy match {
      case DiscreteCostPolicy => calcChargeChunksDiscrete(algChunked, priChunked, volume, res)
      case _ => calcChargeChunksContinuous(algChunked, priChunked, volume, res)
    }
  }

  private[logic]
  def calcChargeChunksDiscrete(algChunked: Map[Timeslot, DSLAlgorithm],
                               priChunked: Map[Timeslot, DSLPriceList],
                               volume: Float, res: DSLResource): List[ChargeChunk] = {
    assert(algChunked.size == 1)
    assert(priChunked.size == 1)
    assert(algChunked.keySet.head.compare(priChunked.keySet.head) == 0)

    List(ChargeChunk(volume,
      algChunked.valuesIterator.next.algorithms.getOrElse(res, ""),
      priChunked.valuesIterator.next.prices.getOrElse(res, 0F),
      algChunked.keySet.head, res))
  }

  private[logic]
  def calcChargeChunksContinuous(algChunked: Map[Timeslot, DSLAlgorithm],
                                 priChunked: Map[Timeslot, DSLPriceList],
                                 volume: Float, res: DSLResource): List[ChargeChunk] = {
    algChunked.keySet.map {
      x =>
        ChargeChunk(volume,
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

  def cost(): Float =
    //TODO: Apply the algorithm, when we start parsing it
    resource.costpolicy match {
      case DiscreteCostPolicy =>
        value * price
      case _ =>
        value * price * when.hours
    }

  def reason(): String =
    resource.costpolicy match {
      case DiscreteCostPolicy =>
        "%f %s at %s @ %f/%s".format(value, resource.unit, when.from, price,
          resource.unit)
      case ContinuousCostPolicy =>
        "%f %s of %s from %s to %s @ %f/%s".format(value, resource.unit,
          resource.name, when.from, when.to, price, resource.unit)
      case OnOffCostPolicy =>
        "%f %s of %s from %s to %s @ %f/%s".format(when.hours, resource.unit,
          resource.name, when.from, when.to, price, resource.unit)
    }

  def id(): String =
    CryptoUtils.sha1("%f%s%f%s%s%d".format(value, algorithm, price, when.toString,
      resource.name, System.currentTimeMillis()))
}

/** An exception raised when something goes wrong with accounting */
class AccountingException(msg: String) extends Exception(msg)
