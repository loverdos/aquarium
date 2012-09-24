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

package gr.grnet.aquarium.charging.state

import gr.grnet.aquarium.message.avro.gen.UserStateMsg
import gr.grnet.aquarium.util.json.JsonSupport
import gr.grnet.aquarium.event.CreditsModel

/**
 *
 * A wrapper around [[gr.grnet.aquarium.message.avro.gen.UserStateMsg]] with convenient (sorted)
 * user agreement history.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

final case class UserStateModel(
    msg: UserStateMsg,
    userAgreementHistoryModel: UserAgreementHistoryModel
) extends JsonSupport {

  def updateLatestResourceEventOccurredMillis(millis: Long): Unit = {
    if(millis > this.msg.getLatestResourceEventOccurredMillis) {
      this.msg.setLatestResourceEventOccurredMillis(millis)
    }
  }

  def userID = msg.getUserID

  def latestResourceEventOccurredMillis = this.msg.getLatestResourceEventOccurredMillis

  def subtractCredits(credits: CreditsModel.Type) {
    val oldTotal = CreditsModel.from(msg.getTotalCredits)
    val newTotal = CreditsModel.-(oldTotal, credits)
    msg.setTotalCredits(CreditsModel.toTypeInMessage(newTotal))
  }

  @inline final def totalCredits: CreditsModel.Type = {
    CreditsModel.from(msg.getTotalCredits)
  }

//  def newForImplicitEndsAsPreviousEvents(
//      previousResourceEvents: mutable.Map[(String, String), ResourceEventModel]
//  ) = {
//
//    new WorkingUserState(
//      this.userID,
//      this.parentUserStateIDInStore,
//      this.chargingReason,
//      this.resourceTypesMap,
//      previousResourceEvents,
//      this.implicitlyIssuedStartEventOfResourceInstance,
//      this.accumulatingAmountOfResourceInstance,
//      this.chargingDataOfResourceInstance,
//      this.totalCredits,
//      this.workingAgreementHistory,
//      this.latestUpdateMillis,
//      this.latestResourceEventOccurredMillis,
//      this.billingPeriodOutOfSyncResourceEventsCounter,
//      this.walletEntries
//    )
//  }

//  def getChargingDataForResourceEvent(resourceAndInstanceInfo: (String, String)): mutable.Map[String, Any] = {
//    chargingDataOfResourceInstance.get(resourceAndInstanceInfo) match {
//      case Some(map) ⇒
//        map
//
//      case None ⇒
//        val map = mutable.Map[String, Any]()
//        chargingDataOfResourceInstance(resourceAndInstanceInfo) = map
//        map
//
//    }
//  }

//  def setChargingDataForResourceEvent(
//      resourceAndInstanceInfo: (String, String),
//      data: mutable.Map[String, Any]
//  ): Unit = {
//    chargingDataOfResourceInstance(resourceAndInstanceInfo) = data
//  }

  /**
  * Find those events from `implicitlyIssuedStartEvents` and `previousResourceEvents` that will generate implicit
  * end events along with those implicitly issued events. Before returning, remove the events that generated the
  * implicit ends from the internal state of this instance.
  *
  * @see [[gr.grnet.aquarium.charging.ChargingBehavior]]
  */
// def findAndRemoveGeneratorsOfImplicitEndEvents(
//     chargingBehaviorOfResourceType: ResourceType ⇒ ChargingBehavior,
//     /**
//      * The `occurredMillis` that will be recorded in the synthetic implicit OFFs.
//      * Normally, this will be the end of a billing month.
//      */
//     newOccuredMillis: Long
// ): (List[ResourceEventModel], List[ResourceEventModel]) = {
//
//   val buffer = mutable.ListBuffer[(ResourceEventModel, ResourceEventModel)]()
//   val checkSet = mutable.Set[ResourceEventModel]()
//
//   def doItFor(map: mutable.Map[(String, String), ResourceEventModel]): Unit = {
//     val resourceEvents = map.valuesIterator
//     for {
//       resourceEvent ← resourceEvents
//       resourceType ← resourceTypesMap.get(resourceEvent.safeResource)
//       chargingBehavior = chargingBehaviorOfResourceType.apply(resourceType)
//     } {
//       if(chargingBehavior.supportsImplicitEvents) {
//         if(chargingBehavior.mustConstructImplicitEndEventFor(resourceEvent)) {
//           val implicitEnd = chargingBehavior.constructImplicitEndEventFor(resourceEvent, newOccuredMillis)
//
//           if(!checkSet.contains(resourceEvent)) {
//             checkSet.add(resourceEvent)
//             buffer append ((resourceEvent, implicitEnd))
//           }
//
//           // remove it anyway
//           map.remove((resourceEvent.safeResource, resourceEvent.safeInstanceID))
//         }
//       }
//     }
//   }
//
//   doItFor(previousEventOfResourceInstance) // we give priority for previous events
//   doItFor(implicitlyIssuedStartEventOfResourceInstance) // ... over implicitly issued ones ...
//
//   (buffer.view.map(_._1).toList, buffer.view.map(_._2).toList)
// }
}
