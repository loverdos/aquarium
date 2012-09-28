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

package gr.grnet.aquarium.charging

import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.message.avro.gen.{ResourceTypeMsg, UserStateMsg, ResourceEventMsg}
import gr.grnet.aquarium.message.avro.{MessageHelpers, MessageFactory, AvroHelpers}
import gr.grnet.aquarium.util.LogHelpers.Debug
import gr.grnet.aquarium.util.LogHelpers.DebugSeq
import gr.grnet.aquarium.util.LogHelpers.Warn
import gr.grnet.aquarium.util.date.{MutableDateCalc, TimeHelpers}
import gr.grnet.aquarium.util.{Lifecycle, Loggable}
import gr.grnet.aquarium.{Real, AquariumInternalError, AquariumAwareSkeleton}
import java.util.{Map ⇒ JMap}
import gr.grnet.aquarium.charging.state.{UserStateModel, UserAgreementHistoryModel}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

final class ChargingService extends AquariumAwareSkeleton with Lifecycle with Loggable {
  lazy val policyStore = aquarium.policyStore
  lazy val userStateStore = aquarium.userStateStore
  lazy val resourceEventStore = aquarium.resourceEventStore

  //+ Lifecycle
  def start() {}

  def stop() {}
  //- Lifecycle

  def calculateRealtimeUserState(
      userStateModel: UserStateModel,
      resourceMapping: JMap[String, ResourceTypeMsg],
      realtimeMillis: Long
  ) {

    val userStateMsg = userStateModel.userStateMsg
    val userAgreementHistoryModel = userStateModel.userAgreementHistoryMsg

    import scala.collection.JavaConverters.mapAsScalaMapConverter

    val stateOfResources = userStateMsg.getStateOfResources.asScala

    for( (resourceName, workingResourcesState) ← stateOfResources) {
      resourceMapping.get(resourceName) match {
        case null ⇒
          // Ignore

        case resourceTypeMsg ⇒
          val chargingBehavior = aquarium.chargingBehaviorOf(resourceTypeMsg)
          val stateOfResourceInstance = workingResourcesState.getStateOfResourceInstance.asScala

          for((resourceInstanceID, resourceInstanceState) ← stateOfResourceInstance) {
            Debug(logger, "Realtime calculation for %s, %s", resourceName, resourceInstanceID)
            val virtualEvents = chargingBehavior.createVirtualEventsForRealtimeComputation(
              userStateMsg.getUserID,
              resourceName,
              resourceInstanceID,
              realtimeMillis,
              resourceInstanceState
            )
            DebugSeq(logger, "virtualEvents", virtualEvents, 1)

            processResourceEvents(
              realtimeMillis,
              virtualEvents,
              userStateModel,
              resourceMapping,
              realtimeMillis
            )
          }
      }
    }
  }

  def findOrCalculateWorkingUserStateAtEndOfBillingMonth(
      processingTimeMillis: Long,
      userStateModel: UserStateModel,
      billingMonthInfo: BillingMonthInfo,
      resourceMapping: JMap[String, ResourceTypeMsg],
      userStateRecorder: UserStateMsg ⇒ UserStateMsg
  ): UserStateMsg = {

    def computeFullMonthBillingAndSaveState(): UserStateMsg = {
      val fullMonthUserState = replayFullMonthBilling(
        processingTimeMillis,
        userStateModel,
        billingMonthInfo,
        resourceMapping,
        userStateRecorder
      )

      val monthlyUserState0 = UserStateMsg.newBuilder(fullMonthUserState).
        setIsForFullMonth(true).
        setBillingYear(billingMonthInfo.year).
        setBillingMonth(billingMonthInfo.month). // FIXME What about the billingMonthDay?
        setOriginalID("").
        build()

      // We always save the state when it is a full month billing
      val monthlyUserState1 = userStateRecorder.apply(monthlyUserState0)

      Debug(logger, "Stored full %s %s", billingMonthInfo.toDebugString, AvroHelpers.jsonStringOfSpecificRecord(monthlyUserState1))

      monthlyUserState1
    }

    val userID = userStateModel.userID
    val userCreationMillis = userStateModel.unsafeUserCreationMillis
    val userCreationDateCalc = new MutableDateCalc(userCreationMillis)
    val billingMonthStartMillis = billingMonthInfo.monthStartMillis
    val billingMonthStopMillis = billingMonthInfo.monthStopMillis

    if(billingMonthStopMillis < userCreationMillis) {
      // If the user did not exist for this billing month, piece of cake
      Debug(logger, "User did not exist before %s", userCreationDateCalc)

      // TODO: The initial user state might have already been created.
      //       First ask if it exists and compute only if not
      val initialUserState0 = MessageFactory.newInitialUserStateMsg(
        userID,
        Real.Zero,
        TimeHelpers.nowMillis()
      )

      Debug(logger, "Created (from bootstrap) initial user state %s", initialUserState0)

      // We always save the initial state
      val initialUserState1 = userStateRecorder.apply(initialUserState0)

      Debug(logger, "Stored initial state = %s", AvroHelpers.jsonStringOfSpecificRecord(initialUserState1))

      return initialUserState1
    }

    // Ask DB cache for the latest known user state for this billing period
    val latestUserStateOpt = userStateStore.findLatestUserStateForFullMonthBilling(
      userID,
      billingMonthInfo)

    latestUserStateOpt match {
      case None ⇒
        // Not found, must compute
        Debug(logger, "No user state found from cache, will have to (re)compute")
        computeFullMonthBillingAndSaveState

      case Some(latestUserState) ⇒
        // Found a "latest" user state but need to see if it is indeed the true and one latest.
        // For this reason, we must count the events again.
        val latestStateOOSEventsCounter = latestUserState.getBillingPeriodOutOfSyncResourceEventsCounter
        val actualOOSEventsCounter = resourceEventStore.countOutOfSyncResourceEventsForBillingPeriod(
          userID,
          billingMonthStartMillis,
          billingMonthStopMillis)

        val counterDiff = actualOOSEventsCounter - latestStateOOSEventsCounter
        counterDiff match {
          // ZERO, we are OK!
          case 0 ⇒
            // NOTE: Keep the caller's calculation reason
            latestUserState

          // We had more, so must recompute
          case n if n > 0 ⇒
            Debug(logger,
              "Found %s out of sync events (%s more), will have to (re)compute user state", actualOOSEventsCounter, n)
            computeFullMonthBillingAndSaveState

          // We had less????
          case n if n < 0 ⇒
            val errMsg = "Found %s out of sync events (%s less). DB must be inconsistent".format(actualOOSEventsCounter, n)
            Warn(logger, errMsg)
            throw new AquariumInternalError(errMsg)
        }
    }
  }
  /**
   * Processes one resource event and computes relevant, incremental charges.
   * If needed, it may go back in time and recompute stuff.
   *
   * @param resourceEvent
   */
  def processResourceEvent(
      processingTimeMillis: Long,
      resourceEvent: ResourceEventMsg,
      userStateModel: UserStateModel,
      resourceMapping: JMap[String, ResourceTypeMsg],
      updateLatestMillis: Boolean
  ) {
    val userStateMsg = userStateModel.userStateMsg
    val userAgreementHistoryModel = userStateModel.userAgreementHistoryMsg

    val resourceName = resourceEvent.getResource
    val resourceTypeMsg = resourceMapping.get(resourceName)

    val chargingBehavior = aquarium.chargingBehaviorOf(resourceTypeMsg)
    val resourcesChargingState = MessageHelpers.getOrInitializeResourcesChargingState(
      userStateMsg,
      resourceName,
      chargingBehavior.initialChargingDetails
    )

    val eventOccurredMillis = resourceEvent.getOccurredMillis
    val eventReceivedMillis = resourceEvent.getReceivedMillis
    val billingMonthInfo = BillingMonthInfo.fromMillis(eventOccurredMillis)


    // See if this is out-of-sync



    val m0 = TimeHelpers.nowMillis()
    val (walletEntriesCount, creditsToSubtract) = chargingBehavior.processResourceEvent(
      aquarium,
      resourceEvent,
      resourceTypeMsg,
      billingMonthInfo,
      resourcesChargingState,
      userStateModel,
      msg ⇒ userStateMsg.getWalletEntries.add(msg)
    )
    val m1 = TimeHelpers.nowMillis()

    if(updateLatestMillis) {
      userStateMsg.setLatestUpdateMillis(m1)
    }

    MessageHelpers.updateLatestResourceEventOccurredMillis(userStateMsg, resourceEvent.getOccurredMillis)
    MessageHelpers.subtractCredits(userStateMsg, creditsToSubtract)

    true
  }

  def processResourceEvents(
      processingTimeMillis: Long,
      resourceEvents: Traversable[ResourceEventMsg],
      userStateModel: UserStateModel,
      resourceMapping: JMap[String, ResourceTypeMsg],
      latestUpdateMillis: Long
  ): Unit = {

    var _counter = 0
    for(currentResourceEvent ← resourceEvents) {
      processResourceEvent(
        processingTimeMillis,
        currentResourceEvent,
        userStateModel,
        resourceMapping,
        false
      )

      _counter += 1
    }

    if(_counter > 0) {
      userStateModel.userStateMsg.setLatestUpdateMillis(latestUpdateMillis)
    }
  }

  def replayFullMonthBilling(
      processingTimeMillis: Long,
      userStateModel: UserStateModel,
      billingMonthInfo: BillingMonthInfo,
      resourceMapping: JMap[String, ResourceTypeMsg],
      userStateRecorder: UserStateMsg ⇒ UserStateMsg
  ): UserStateMsg = {

    replayMonthChargingUpTo(
      processingTimeMillis,
      userStateModel,
      billingMonthInfo,
      billingMonthInfo.monthStopMillis,
      resourceMapping,
      userStateRecorder
    )
  }

  /**
   * Replays the charging procedure over the set of resource events that happened within the given month and up to
   * the specified point in time.
   *
   * @param billingMonthInfo Which month to bill.
   * @param billingEndTimeMillis Bill from start of month up to (and including) this time.
   * @param userStateRecorder
   * @return
   */
  def replayMonthChargingUpTo(
      processingTimeMillis: Long,
      userStateModel: UserStateModel,
      billingMonthInfo: BillingMonthInfo,
      billingEndTimeMillis: Long,
      resourceMapping: JMap[String, ResourceTypeMsg],
      userStateRecorder: UserStateMsg ⇒ UserStateMsg
  ): UserStateMsg = {

    val userAgreementHistoryMsg = userStateModel.userAgreementHistoryMsg

    val isFullMonthBilling = billingEndTimeMillis == billingMonthInfo.monthStopMillis
    val userID = userStateModel.userID

    // In order to replay the full month, we start with the state at the beginning of the month.
    val previousBillingMonthInfo = billingMonthInfo.previousMonth
    val userStateMsg = findOrCalculateWorkingUserStateAtEndOfBillingMonth(
      processingTimeMillis,
      userStateModel,
      previousBillingMonthInfo,
      resourceMapping,
      userStateRecorder
    )

    // FIXME the below comments
    // Keep the working (current) user state. This will get updated as we proceed with billing for the month
    // specified in the parameters.
    // NOTE: The calculation reason is not the one we get from the previous user state but the one our caller specifies

    Debug(logger, "workingUserState=%s", userStateMsg)
    Debug(logger, "previousBillingMonthUserState(%s) = %s",
      previousBillingMonthInfo.toShortDebugString,
      userStateMsg
    )

    var _rcEventsCounter = 0
    resourceEventStore.foreachResourceEventOccurredInPeriod(
      userID,
      billingMonthInfo.monthStartMillis, // from start of month
      billingEndTimeMillis               // to requested time
    ) { currentResourceEvent ⇒

      Debug(logger, "Processing %s", currentResourceEvent)

      processResourceEvent(
        processingTimeMillis,
        currentResourceEvent,
        userStateModel,
        resourceMapping,
        false
      )

      _rcEventsCounter += 1
    }

    if(_rcEventsCounter > 0) {
      userStateMsg.setLatestUpdateMillis(TimeHelpers.nowMillis())
    }

    Debug(logger, "Found %s resource events for month %s",
      _rcEventsCounter,
      billingMonthInfo.toShortDebugString
    )

    // FIXME Reuse the logic here...Do not erase the comment...
    /*if(isFullMonthBilling) {
      // For the remaining events which must contribute an implicit OFF, we collect those OFFs
      // ... in order to generate an implicit ON later (during the next billing cycle).
      val (generatorsOfImplicitEnds, theirImplicitEnds) = workingUserState.findAndRemoveGeneratorsOfImplicitEndEvents(
        aquarium.chargingBehaviorOf(_),
        billingMonthInfo.monthStopMillis
      )

      if(generatorsOfImplicitEnds.lengthCompare(1) >= 0 || theirImplicitEnds.lengthCompare(1) >= 0) {
        Debug(logger, "")
        Debug(logger, "Process implicitly issued events")
        DebugSeq(logger, "generatorsOfImplicitEnds", generatorsOfImplicitEnds, 0)
        DebugSeq(logger, "theirImplicitEnds", theirImplicitEnds, 0)
      }

      // Now, the previous and implicitly started must be our base for the following computation, so we create an
      // appropriate worker
      val specialWorkingUserState = workingUserState.newForImplicitEndsAsPreviousEvents(
        WorkingUserState.makePreviousResourceEventMap(generatorsOfImplicitEnds)
      )

      processResourceEvents(
        theirImplicitEnds,
        specialWorkingUserState,
        chargingReason,
        billingMonthInfo
      )

      workingUserState.walletEntries ++= specialWorkingUserState.walletEntries
      workingUserState.totalCredits    = specialWorkingUserState.totalCredits
    }*/

    userStateMsg
  }
}
