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

package gr.grnet.aquarium.message.avro

import gr.grnet.aquarium.AquariumInternalError
import gr.grnet.aquarium.event.DetailsModel
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.message.MessageConstants
import gr.grnet.aquarium.message.avro.gen.{AnyValueMsg, WalletEntryMsg, EffectivePriceTableMsg, FullPriceTableMsg, ResourceInstanceChargingStateMsg, UserStateMsg, IMEventMsg, ResourceEventMsg}
import gr.grnet.aquarium.policy.EffectivePriceTableModel
import gr.grnet.aquarium.uid.UUIDGenerator
import gr.grnet.aquarium.util.LogHelpers.Debug
import gr.grnet.aquarium.util.shortNameOfType
import java.util.{Map ⇒ JMap}
import org.slf4j.Logger
import scala.annotation.tailrec

/**
 * Provides helper methods related to messages.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
final object MessageHelpers {
  final val UserAgreementMsgIDGenerator = UUIDGenerator
  final val VirtualEventsIDGen = UUIDGenerator

  def stringOfAnyValueMsg(msg: AnyValueMsg): String = {
    String.valueOf(msg.getAnyValue)
  }

  @inline final def isOccurredWithinMillis(
      event: ResourceEventMsg,
      fromMillis: Long,
      toMillis: Long
  ): Boolean = {
    require(fromMillis <= toMillis, "fromMillis <= toMillis")
    val eventOccurredMillis = event.getOccurredMillis
    fromMillis <= eventOccurredMillis && eventOccurredMillis <= toMillis
  }

  @inline final def isReceivedWithinMillis(
      event: ResourceEventMsg,
      fromMillis: Long,
      toMillis: Long
  ): Boolean = {
    require(fromMillis <= toMillis, "fromMillis <= toMillis")
    val eventReceivedMillis = event.getReceivedMillis
    fromMillis <= eventReceivedMillis && eventReceivedMillis <= toMillis
  }

  @inline final def isOutOfSyncForBillingPeriod(
      event: ResourceEventMsg,
      billingStartMillis: Long,
      billingStopMillis: Long
  ): Boolean = {

    // Out of sync events are those that were received within the billing period
    // but actually occurred outside the billing period.
    isReceivedWithinMillis(event, billingStartMillis, billingStopMillis) &&
    !isOccurredWithinMillis(event, billingStartMillis, billingStopMillis)
  }

  @inline final def isIMEventCreate(msg: IMEventMsg) = {
    msg.getEventType().toLowerCase() == MessageConstants.IMEventMsg.EventTypes.create
  }

  @inline final def isIMEventModify(msg: IMEventMsg) = {
    msg.getEventType().toLowerCase() == MessageConstants.IMEventMsg.EventTypes.modify
  }

  @inline final def findResourceType(msg: UserStateMsg, name: String) = {
    msg.getResourceTypesMap.get(name) match {
      case null         ⇒ None
      case resourceType ⇒ Some(resourceType)
    }
  }

  final def setOnePreviousEvent(
      resourceInstanceChargingState: ResourceInstanceChargingStateMsg,
      msg: ResourceEventMsg
  ) {
    val previousEvents = resourceInstanceChargingState.getPreviousEvents
    previousEvents.clear()
    previousEvents.add(msg)
  }

  final def setAquariumSyntheticAndImplicitEnd(details: DetailsModel.Type) {
    DetailsModel.setBoolean(details, MessageConstants.DetailsKeys.aquarium_is_synthetic, true)
    DetailsModel.setBoolean(details, MessageConstants.DetailsKeys.aquarium_is_implicit_end, true)
  }

  /**
   *
   * See [[gr.grnet.aquarium.policy.FullPriceTableModel]]
   */
  final def effectivePriceTableOfSelectorForResource(
      fullPriceTable: FullPriceTableMsg,
      selectorPath: List[String],
      resource: String,
      logger: Logger
  ): EffectivePriceTableMsg = {

    // Most of the code is for exceptional cases, which we identify in detail.
    @tailrec
    def find(
        partialSelectorPath: List[String],
        partialSelectorData: Any // type SelectorValueMsg = ju.Map[String, SelectorValueMsg] | EffectivePriceTableMsg
    ): EffectivePriceTableMsg = {

      Debug(logger, "find: ")
      Debug(logger, "  partialSelectorPath = %s", partialSelectorPath.mkString("/"))
      Debug(logger, "  partialSelectorData = %s", partialSelectorData)

      partialSelectorPath match {
        case selector :: Nil ⇒
          // One selector, meaning that the selectorData must be a Map[String, EffectivePriceTable]
          partialSelectorData match {
            case selectorMap: JMap[_,_] ⇒
              // The selectorData is a map indeed
              selectorMap.asInstanceOf[JMap[String, _]].get(selector) match {
                case null ⇒
                  // The selectorData is a map but it does nto contain the selector
                  throw new AquariumInternalError(
                    "[AQU-SEL-002] Cannot select path %s for resource %s. Nothing found at partial selector path %s",
                      selectorPath.mkString("/"),
                      resource,
                      partialSelectorPath.mkString("/")
                  )

                case selected: EffectivePriceTableMsg ⇒
                  // Yes, it is a map of the right type (OK, we assume keys are always Strings)
                  // (we only check the value type)
                  selected

                case badSelected ⇒
                  // The selectorData is a map but the value is not of the required type
                  throw new AquariumInternalError(
                    "[AQU-SEL-001] Cannot select path %s for resource %s. Found %s instead of an %s at partial selector path %s",
                      selectorPath.mkString("/"),
                      resource,
                      badSelected,
                      shortNameOfType[EffectivePriceTableModel],
                      partialSelectorPath.mkString("/")
                  )

              }


            case badData ⇒
              // The selectorData is not a map. So we have just one final selector but no map to select from.
              throw new AquariumInternalError(
                "[AQU-SEL-003] Cannot select path %s for resource %s. Found %s instead of a Map at partial selector path %s",
                  selectorPath.mkString("/"),
                  resource,
                  badData,
                  partialSelectorPath.mkString("/")
              )
          }

        case selector :: selectorTail ⇒
          // More than one selector in the path, meaning that the selectorData must be a Map[String, Map[String, SelectorValueMsg]]
          partialSelectorData match {
            case selectorMap: JMap[_,_] ⇒
             // The selectorData is a map indeed
              selectorMap.asInstanceOf[JMap[String,_]].get(selector) match {
                case null ⇒
                  // The selectorData is a map but it does not contain the selector
                  throw new AquariumInternalError(
                    "[AQU-SEL-005] Cannot select path %s for resource %s. Nothing found at partial selector path %s",
                      selectorPath.mkString("/"),
                      resource,
                      partialSelectorPath.mkString("/")
                  )

                case furtherSelectorMap: JMap[_,_] ⇒
                  // The selectorData is a map and we found the respective value for the selector to be a map.
                  find(selectorTail, furtherSelectorMap)

                case furtherBad ⇒
                  // The selectorData is a map but the respective value is not a map, so that
                  // the selectorTail path cannot be used.
                  throw new AquariumInternalError(
                    "[AQU-SEL-004] Cannot select path %s for resource %s. Found %s instead of a Map at partial selector path %s",
                      selectorPath.mkString("/"),
                      resource,
                      furtherBad,
                      partialSelectorPath.mkString("/")
                  )

              }

            case badData ⇒
              // The selectorData is not a Map. So we have more than one selectors but no map to select from.
              throw new AquariumInternalError(
                "[AQU-SEL-006] Cannot select path %s for resource %s. Found %s instead of a Map at partial selector path %s",
                  selectorPath.mkString("/"),
                  resource,
                  badData,
                  partialSelectorPath.mkString("/")
              )
          }

        case Nil ⇒
          throw new AquariumInternalError(
            "[AQU-SEL-007] No selector path for resource %s".format(resource)
          )

      }
    }

    Debug(logger, "effectivePriceTableOfSelectorForResource:")
    Debug(logger, "selectorPath = %s", selectorPath.mkString("/"))

    val perResource = fullPriceTable.getPerResource
    val selectorData = perResource.get(resource)
    if(selectorData eq null) {
      throw new AquariumInternalError(
        "[AQU-SEL-007] Cannot select path %s for unknown resource %s",
          selectorPath.mkString("/"),
          resource
      )
    }

    Debug(logger, "  selectorData = %s", selectorData)
    find(selectorPath, selectorData)
  }

  @inline final def referenceTimeslotOf(msg: WalletEntryMsg) = {
    Timeslot(msg.getReferenceStartMillis, msg.getReferenceStopMillis)
  }

  @inline final def chargeslotCountOf(msg: WalletEntryMsg) = {
    msg.getChargeslots.size()
  }

  final def currentResourceEventOf(msg: WalletEntryMsg): ResourceEventMsg = {
    val resourceEvents = msg.getResourceEvents
    resourceEvents.size() match {
      case n if n >= 1 ⇒ resourceEvents.get(0)
      case _ ⇒ throw new AquariumInternalError("No resource events in wallet entry")
    }
  }

//  final def splitEffectiveUnitPriceTimeslot(
//      effectiveUnitPrice: EffectiveUnitPriceMsg,
//      t: Timeslot
//  ): (List[Timeslot], List[Timeslot]) = {
//
//  }
}
