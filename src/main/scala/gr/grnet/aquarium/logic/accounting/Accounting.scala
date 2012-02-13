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
import gr.grnet.aquarium.store.PolicyStore

/**
 * Methods for converting accounting events to wallet entries.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
trait Accounting extends DSLUtils with Loggable {

  def computeWalletEntriesForAgreement(userId: String,
                                       totalCredits: Double,
                                       costPolicy: DSLCostPolicy,
                                       previousResourceEventM: Maybe[ResourceEvent],
                                       previousAccumulatingAmountM: Maybe[Double],
                                       currentResourceEvent: ResourceEvent,
                                       defaultResourcesMap: DSLResourcesMap,
                                       agreement: DSLAgreement): Maybe[Traversable[WalletEntry]] = Maybe {
    val resource   = currentResourceEvent.resource
    val instanceId = currentResourceEvent.instanceId
    val currentValue = currentResourceEvent.value
    val currentOccurredMillis = currentResourceEvent.occurredMillis

    /////////////////////////////////////////////////////////////////////
    // Validations
    /////////////////////////////////////////////////////////////////////
    // 1. Validate cost policy
    val actualCostPolicyM = currentResourceEvent.findCostPolicyM(defaultResourcesMap)
    val currentResourceEventDebugStr = currentResourceEvent.toDebugString(defaultResourcesMap, false)
    actualCostPolicyM match {
      case Just(actualCostPolicy) ⇒
        if(costPolicy != actualCostPolicy) {
          throw new Exception("Actual cost policy %s is not the same as provided %s for event %s".
            format(actualCostPolicy, costPolicy, currentResourceEventDebugStr))
        }
      case _ ⇒
        throw new Exception("Could not verify cost policy %s for event %s".
          format(costPolicy, currentResourceEventDebugStr))
    }

    // 2. Validate previous resource event
    previousResourceEventM match {
      case Just(previousResourceEvent) ⇒
        if(!costPolicy.needsPreviousEventForCreditAndAmountCalculation) {
          throw new Exception("Provided previous event but cost policy %s does not need one".format(costPolicy))
        }

        // 3. resource and instanceId
        val previousResource = previousResourceEvent.resource
        val previousInstanceId = previousResourceEvent.instanceId
        (resource == previousResource, instanceId == previousInstanceId) match {
          case (true, true)  ⇒

          case (true, false) ⇒
            throw new Exception("Resource instance IDs do not match (%s vs %s) for current and previous event".
              format(instanceId, previousInstanceId))

          case (false, true) ⇒
            throw new Exception("Resource types do not match (%s vs %s) for current and previous event".
              format(resource, previousResource))

          case (false, false) ⇒
            throw new Exception("Resource types and instance IDs do not match (%s vs %s) for current and previous event".
              format((resource, instanceId), (previousResource, previousInstanceId)))
        }

      case NoVal ⇒
        if(costPolicy.needsPreviousEventForCreditAndAmountCalculation) {
          throw new Exception("Did not provid previous event but cost policy %s needa one".format(costPolicy))
        }

      case failed @ Failed(e, m) ⇒
        throw new Exception("Error obtaining previous event".format(m), e)
    }

    // 4. Make sure this is billable.
    // It is the caller's responsibility to provide us with billable events
    costPolicy.isBillableEventBasedOnValue(currentValue) match {
      case true  ⇒
      case false ⇒
        throw new Exception("Event not billable %s".format(currentResourceEventDebugStr))
    }
    /////////////////////////////////////////////////////////////////////

    // Construct a proper value map
    val valueMap = costPolicy.makeValueMap(
      totalCredits,
      previousAccumulatingAmountM.getOr(0.0),
      currentOccurredMillis - previousResourceEventM.map(_.occurredMillis).getOr(currentOccurredMillis),
      previousResourceEventM.map(_.value).getOr(0.0),
      currentValue)
    // Now, we need to find the proper agreement(s) and feed the data to them.


    Nil
  }

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
   * Creates a list of wallet entries by applying the agreement provisions on
   * the resource state.
   *
   * @param resourceEvent The resource event to create charges for
   * @param agreement The agreement applicable to the user mentioned in the event
   * @param currentValue The current state of the resource
   * @param currentSnapshotDate The last time the resource state was updated
   */
  def chargeEvent(resourceEvent: ResourceEvent,
                  agreement: DSLAgreement,
                  currentValue: Double,
                  currentSnapshotDate: Date,
                  related: List[WalletEntry]): Maybe[List[WalletEntry]] = {

    assert(currentSnapshotDate.getTime <= resourceEvent.occurredMillis)

    if (!resourceEvent.validate())
      return Failed(new AccountingException("Event not valid"))

    val policy = Policy.policy
    val dslResource = policy.findResource(resourceEvent.resource) match {
      case Some(x) => x
      case None => return Failed(new AccountingException("No resource [%s]".format(resourceEvent.resource)))
    }

    /* This is a safeguard against the special case where the last
     * resource state update, as marked by the lastUpdate parameter
     * is equal to the time of the event occurrence. This means that
     * this is the first time the resource state has been recorded.
     * Charging in this case only makes sense for discrete resources.
     */
    if (currentSnapshotDate.getTime == resourceEvent.occurredMillis) {
      dslResource.costPolicy match {
        case DiscreteCostPolicy => //Ok
        case _ => return Some(List())
      }
    }

    val creditCalculationValueM = dslResource.costPolicy.getValueForCreditCalculation(Just(currentValue), resourceEvent.value)
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
        OnOffPolicyResourceState(currentValue) match {
          case OnResourceState => false
          case OffResourceState => true
        }
      case _ => true
    }

    val timeslot = dslResource.costPolicy match {
      case DiscreteCostPolicy => Timeslot(new Date(resourceEvent.occurredMillis),
        new Date(resourceEvent.occurredMillis + 1))
      case _ => Timeslot(currentSnapshotDate, new Date(resourceEvent.occurredMillis))
    }

    val chargeChunks = calcChangeChunks(agreement, amount, dslResource, timeslot)

    val timeReceived = System.currentTimeMillis

    val rel = related.map{x => x.sourceEventIDs}.flatten ++ List(resourceEvent.id)

    val entries = chargeChunks.map {
      c =>
        WalletEntry(
          id = CryptoUtils.sha1(c.id),
          occurredMillis = resourceEvent.occurredMillis,
          receivedMillis = timeReceived,
          sourceEventIDs = rel,
          value = c.cost,
          reason = c.reason,
          userId = resourceEvent.userId,
          resource = resourceEvent.resource,
          instanceId = resourceEvent.instanceId,
          finalized = isFinal
        )
    }
    Just(entries)
  }

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

  private[logic]
  def calcChargeChunksDiscrete(algChunked: Map[Timeslot, DSLAlgorithm],
                               priChunked: Map[Timeslot, DSLPriceList],
                               volume: Double, res: DSLResource): List[ChargeChunk] = {
    assert(algChunked.size == 1)
    assert(priChunked.size == 1)
    assert(algChunked.keySet.head.compare(priChunked.keySet.head) == 0)

    List(ChargeChunk(volume,
      algChunked.valuesIterator.next.algorithms.getOrElse(res, ""),
      priChunked.valuesIterator.next.prices.getOrElse(res, 0),
      algChunked.keySet.head, res))
  }

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
}

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
