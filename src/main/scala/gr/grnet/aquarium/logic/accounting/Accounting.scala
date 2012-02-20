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
import java.util.Date
import gr.grnet.aquarium.util.{CryptoUtils, Loggable}
import com.ckkloverdos.maybe.{NoVal, Maybe, Failed, Just}

/**
 * Methods for converting accounting events to wallet entries.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
trait Accounting extends DSLUtils with Loggable {
  
  def chargeEvent2( oldResourceEventM: Maybe[ResourceEvent],
                   newResourceEvent: ResourceEvent,
                   dslAgreement: DSLAgreement,
                   lastSnapshotDate: Date,
                   related: Traversable[WalletEntry]): Maybe[Traversable[WalletEntry]] = {
    Maybe {
      val dslPolicy: DSLPolicy = Policy.policy // TODO: query based on time
      val resourceEvent = newResourceEvent
      dslPolicy.findResource(resourceEvent.resource) match {
        case None ⇒
          throw new AccountingException("No resource [%s]".format(resourceEvent.resource))
        case Some(dslResource) ⇒

          val costPolicy = dslResource.costPolicy
          val isDiscrete = costPolicy.isDiscrete
          val oldValueM = oldResourceEventM.map(_.value)
          val newValue = newResourceEvent.value

          /* This is a safeguard against the special case where the last
          * resource state update, as marked by the lastUpdate parameter
          * is equal to the time of the event occurrence. This means that
          * this is the first time the resource state has been recorded.
          * Charging in this case only makes sense for discrete resources.
          */
          if (lastSnapshotDate.getTime == resourceEvent.occurredMillis && !isDiscrete) {
            Just(List())
          } else {
            val creditCalculationValueM = dslResource.costPolicy.getValueForCreditCalculation(oldValueM, newValue).forNoVal(Just(0.0))
            for {
              amount <- creditCalculationValueM
            } yield {
              // We don't do strict checking for all cases for OnOffPolicies as
              // above, since this point won't be reached in case of error.
              val isFinal = dslResource.costPolicy match {
                case OnOffCostPolicy =>
                  OnOffPolicyResourceState(oldValueM) match {
                    case OnResourceState => false
                    case OffResourceState => true
                  }
                case _ => true
              }

              val timeslot = dslResource.costPolicy match {
                case DiscreteCostPolicy => Timeslot(new Date(resourceEvent.occurredMillis),
                  new Date(resourceEvent.occurredMillis + 1))
                case _ => Timeslot(lastSnapshotDate, new Date(resourceEvent.occurredMillis))
              }

              val chargeChunks = calcChangeChunks(dslAgreement, amount, dslResource, timeslot)

              val timeReceived = System.currentTimeMillis

              val rel = related.map{x => x.sourceEventIDs}.flatten ++ Traversable(resourceEvent.id)

              val entries = chargeChunks.map {
                chargedChunk ⇒
                  WalletEntry(
                    id = CryptoUtils.sha1(chargedChunk.id),
                    occurredMillis = resourceEvent.occurredMillis,
                    receivedMillis = timeReceived,
                    sourceEventIDs = rel.toList,
                    value = chargedChunk.cost,
                    reason = chargedChunk.reason,
                    userId = resourceEvent.userId,
                    resource = resourceEvent.resource,
                    instanceId = resourceEvent.instanceId,
                    finalized = isFinal
                  )
              } // entries

              entries
            } // yield
          } // else
      }
    }.flatten1
  }

  /**
   * Create a list of wallet entries by charging for a resource event.
   *
   * @param currentResourceEvent The resource event to create charges for
   * @param agreements The user's agreement names, indexed by their
   *                   applicability timeslot
   * @param previousAmount The current state of the resource
   * @param previousOccurred The last time the resource state was updated
   */
  def chargeEvent(currentResourceEvent: ResourceEvent,
                  agreements: SortedMap[Timeslot, String],
                  previousAmount: Double,
                  previousOccurred: Date,
                  related: List[WalletEntry]): Maybe[List[WalletEntry]] = {

    assert(previousOccurred.getTime <= currentResourceEvent.occurredMillis)
    val occurredDate = new Date(currentResourceEvent.occurredMillis)

    /* The following makes sure that agreements exist between the start
     * and end days of the processed event. As agreement updates are
     * guaranteed not to leave gaps, this means that the event can be
     * processed correctly, as at least one agreement will be valid
     * throughout the event's life.
     */
    assert(
      agreements.keys.exists {
        p => p.includes(occurredDate)
      } && agreements.keys.exists {
        p => p.includes(previousOccurred)
      }
    )

    val t = Timeslot(previousOccurred, occurredDate)

    // Align policy and agreement validity timeslots to the event's boundaries
    val policyTimeslots = t.align(
      Policy.policies(previousOccurred, occurredDate).keys.toList)
    val agreementTimeslots = t.align(agreements.keys.toList)

    /*
     * Get a set of timeslot slices covering the different durations of
     * agreements and policies.
     */
    val aligned = alignTimeslots(policyTimeslots, agreementTimeslots)

    val walletEntries = aligned.map {
      x =>
        // Retrieve agreement from the policy valid at time of event
        val agreementName = agreements.find(y => y._1.contains(x)) match {
          case Some(x) => x
          case None => return Failed(new AccountingException(("Cannot find" +
            " user agreement for period %s").format(x)))
        }

        // Do the wallet entry calculation
        val entries = chargeEvent(
          currentResourceEvent,
          Policy.policy(x.from).findAgreement(agreementName._2).getOrElse(
            return Failed(new AccountingException("Cannot get agreement for %s".format()))
          ),
          previousAmount,
          previousOccurred,
          related,
          Some(x)
        ) match {
          case Just(x) => x
          case Failed(f, e) => return Failed(f,e)
          case NoVal => List()
        }
        entries
    }.flatten

    Just(walletEntries)
  }

  /**
   * Creates a list of wallet entries by applying the agreement provisions on
   * the resource state.
   *
   * @param event The resource event to create charges for
   * @param agr The agreement implementation to use
   * @param previousAmount The current state of the resource
   * @param previousOccurred The timestamp of the previous event
   * @param related Related wallet entries (TODO: should remove)
   * @param chargeFor The duration for which the charge should be done.
   *                  Should fall between the previous and current
   *                  resource event boundaries
   * @return A list of wallet entries, one for each
   */
  def chargeEvent(event: ResourceEvent,
                  agr: DSLAgreement,
                  previousAmount: Double,
                  previousOccurred: Date,
                  related: List[WalletEntry],
                  chargeFor: Option[Timeslot]): Maybe[List[WalletEntry]] = {

    // If chargeFor is not null, make sure it falls within
    // event time boundaries
    chargeFor.map{x => assert(true,
      Timeslot(previousOccurred, new Date(event.occurredMillis)))}

    if (!event.validate())
      return Failed(new AccountingException("Event not valid"))

    val policy = Policy.policy
    val dslResource = policy.findResource(event.resource) match {
      case Some(x) => x
      case None => return Failed(
        new AccountingException("No resource [%s]".format(event.resource)))
    }

    /* This is a safeguard against the special case where the last
     * resource state update, as marked by the lastUpdate parameter
     * is equal to the time of the event occurrence. This means that
     * this is the first time the resource state has been recorded.
     * Charging in this case only makes sense for discrete resources.
     */
    if (previousOccurred.getTime == event.occurredMillis) {
      dslResource.costPolicy match {
        case DiscreteCostPolicy => //Ok
        case _ => return Some(List())
      }
    }

    val creditCalculationValueM = dslResource.costPolicy.getValueForCreditCalculation(Just(previousAmount), event.value)
    val amount = creditCalculationValueM match {
      case failed @ Failed(_, _) ⇒
        return failed
      case Just(amount) ⇒
        amount
      case NoVal ⇒
        0.0
    }

    // We don't do strict checking for all cases for OnOffPolicies as
    // above, since this point won't be reached in case of error.
    val isFinal = dslResource.costPolicy match {
      case OnOffCostPolicy =>
        OnOffPolicyResourceState(previousAmount) match {
          case OnResourceState => false
          case OffResourceState => true
        }
      case _ => true
    }

    /*
     * Get the timeslot for which this event will be charged. In case we
     * have a discrete resource, we do not really care for the time duration
     * of an event. To process all events in a uniform way, we create an
     * artificial timeslot lasting the minimum amount of time. In all other
     * cases, we first check whether a desired charge period passed as
     * an argument.
     */
    val timeslot = dslResource.costPolicy match {
      case DiscreteCostPolicy => Timeslot(new Date(event.occurredMillis - 1),
        new Date(event.occurredMillis))
      case _ => chargeFor match {
        case Some(x) => x
        case None => Timeslot(previousOccurred, new Date(event.occurredMillis))
      }
    }

    /*
     * The following splits the chargable timeslot into smaller timeslots to
     * comply with different applicability periods for algorithms and
     * pricelists defined by the provided agreement.
     */
    val chargeChunks = calcChangeChunks(agr, amount, dslResource, timeslot)

    val timeReceived = System.currentTimeMillis

    val rel = related.map{x => x.sourceEventIDs}.flatten ++ List(event.id)

    /*
     * Convert charge chunks to wallet entries.
     */
    val entries = chargeChunks.map {
      c =>
        WalletEntry(
          id = CryptoUtils.sha1(c.id),
          occurredMillis = event.occurredMillis,
          receivedMillis = timeReceived,
          sourceEventIDs = rel,
          value = c.cost,
          reason = c.reason,
          userId = event.userId,
          resource = event.resource,
          instanceId = event.instanceId,
          finalized = isFinal
        )
    }
    Just(entries)
  }

  /**
   * Create a
   */
  def calcChangeChunks(agr: DSLAgreement, volume: Double,
                       res: DSLResource, t: Timeslot): List[ChargeChunk] = {

    val alg = resolveEffectiveAlgorithmsForTimeslot(t, agr)
    val pri = resolveEffectivePricelistsForTimeslot(t, agr)
    val chunks = splitChargeChunks(alg, pri)
    val algChunked = chunks._1
    val priChunked = chunks._2

    assert(algChunked.size == priChunked.size)

    res.costPolicy match {
      case DiscreteCostPolicy => calcChargeChunksDiscrete(algChunked, priChunked, volume, res)
      case _ => calcChargeChunksContinuous(algChunked, priChunked, volume, res)
    }
  }

  /**
   * Get a list of charge chunks for discrete resources.
   */
  private[logic]
  def calcChargeChunksDiscrete(algChunked: Map[Timeslot, DSLAlgorithm],
                               priChunked: Map[Timeslot, DSLPriceList],
                               volume: Double, res: DSLResource): List[ChargeChunk] = {
    // In case of descrete resources, we only a expect a
    assert(algChunked.size == 1)
    assert(priChunked.size == 1)
    assert(algChunked.keySet.head.compare(priChunked.keySet.head) == 0)

    List(ChargeChunk(volume,
      algChunked.valuesIterator.next.algorithms.getOrElse(res, ""),
      priChunked.valuesIterator.next.prices.getOrElse(res, 0),
      algChunked.keySet.head, res))
  }

  /**
   * Get a list of charge chunks for continuous resources.
   */
  private[logic]
  def calcChargeChunksContinuous(algChunked: Map[Timeslot, DSLAlgorithm],
                                 priChunked: Map[Timeslot, DSLPriceList],
                                 volume: Double, res: DSLResource): List[ChargeChunk] = {
    algChunked.keysIterator.map {
      x =>
        ChargeChunk(volume,
          algChunked.get(x).get.algorithms.getOrElse(res, ""),
          priChunked.get(x).get.prices.getOrElse(res, 0), x, res)
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

  /**
   * Given two lists of timeslots, produce a list which contains the
   * set of timeslot slices, as those are defined by
   * timeslot overlaps.
   *
   * For example, given the timeslots a and b below, split them as shown.
   *
   * a = |****************|
   *     ^                ^
   *   a.from            a.to
   * b = |*********|
   *     ^         ^
   *   b.from     b.to
   *
   * result: List(Timeslot(a.from, b.to), Timeslot(b.to, a.to))
   */
  private[logic] def alignTimeslots(a: List[Timeslot],
                                    b: List[Timeslot]): List[Timeslot] = {
    if (a.isEmpty) return b.tail
    if (b.isEmpty) return a.tail
    assert (a.head.from == b.head.from)

    if (a.head.endsAfter(b.head)) {
      a.head.slice(b.head.to) ::: alignTimeslots(a.tail, b.tail)
    } else if (b.head.endsAfter(a.head)) {
      b.head.slice(a.head.to) ::: alignTimeslots(a.tail, b.tail)
    } else {
      a.head :: alignTimeslots(a.tail, b.tail)
    }
  }
}

/**
 * Encapsulates a computation for a specific timeslot of
 * resource usage.
 */
case class ChargeChunk(value: Double, algorithm: String,
                       price: Double, when: Timeslot,
                       resource: DSLResource) {
  assert(value > 0)
  assert(!algorithm.isEmpty)
  assert(resource != null)

  def cost(): Double =
    //TODO: Apply the algorithm, when we start parsing it
    resource.costPolicy match {
      case DiscreteCostPolicy =>
        value * price
      case _ =>
        value * price * when.hours
    }

  def reason(): String =
    resource.costPolicy match {
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
