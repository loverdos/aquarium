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

import scala.collection.mutable
import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.charging.state.{WorkingResourcesChargingState, UserStateBootstrap, WorkingUserState, UserStateModel, StdUserState}
import gr.grnet.aquarium.policy.ResourceType
import gr.grnet.aquarium.util.{Lifecycle, Loggable}
import gr.grnet.aquarium.util.LogHelpers.Debug
import gr.grnet.aquarium.util.LogHelpers.Warn
import gr.grnet.aquarium.util.date.{MutableDateCalc, TimeHelpers}
import gr.grnet.aquarium.{AquariumInternalError, AquariumAwareSkeleton}
import gr.grnet.aquarium.charging.reason.{MonthlyBillChargingReason, InitialUserStateSetup, ChargingReason}

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


  //+ Utility methods
  protected def rcDebugInfo(rcEvent: ResourceEventModel) = {
    rcEvent.toDebugString
  }
  //- Utility methods

  def findOrCalculateWorkingUserStateAtEndOfBillingMonth(
      billingMonthInfo: BillingMonthInfo,
      userStateBootstrap: UserStateBootstrap,
      defaultResourceTypesMap: Map[String, ResourceType],
      chargingReason: ChargingReason,
      userStateRecorder: UserStateModel ⇒ UserStateModel
  ): WorkingUserState = {

    def computeFullMonthBillingAndSaveState(): WorkingUserState = {
      val workingUserState = replayFullMonthBilling(
        userStateBootstrap,
        billingMonthInfo,
        defaultResourceTypesMap,
        chargingReason,
        userStateRecorder
      )

      val newChargingReason = MonthlyBillChargingReason(chargingReason, billingMonthInfo)
      workingUserState.chargingReason = newChargingReason
      val monthlyUserState0 = workingUserState.toUserState(
        true,
        billingMonthInfo.year,
        billingMonthInfo.month,
        ""
      )

      // We always save the state when it is a full month billing
      val monthlyUserState1 = userStateRecorder.apply(monthlyUserState0)

      Debug(logger, "Stored full %s %s", billingMonthInfo.toDebugString, monthlyUserState1.toJsonString)

      workingUserState
    }

    val userID = userStateBootstrap.userID
    val userCreationMillis = userStateBootstrap.userCreationMillis
    val userCreationDateCalc = new MutableDateCalc(userCreationMillis)
    val billingMonthStartMillis = billingMonthInfo.monthStartMillis
    val billingMonthStopMillis = billingMonthInfo.monthStopMillis

    if(billingMonthStopMillis < userCreationMillis) {
      // If the user did not exist for this billing month, piece of cake
      Debug(logger, "User did not exist before %s", userCreationDateCalc)

      // TODO: The initial user state might have already been created.
      //       First ask if it exists and compute only if not
      val initialUserState0 = StdUserState.createInitialUserStateFromBootstrap(
        userStateBootstrap,
        TimeHelpers.nowMillis(),
        InitialUserStateSetup(Some(chargingReason)) // we record the originating calculation reason
      )

      Debug(logger, "Created (from bootstrap) initial user state %s", initialUserState0)

      // We always save the initial state
      val initialUserState1 = userStateRecorder.apply(initialUserState0)

      Debug(logger, "Stored initial state = %s", initialUserState1.toJsonString)

      return initialUserState1.toWorkingUserState(defaultResourceTypesMap)
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
        val latestStateOOSEventsCounter = latestUserState.billingPeriodOutOfSyncResourceEventsCounter
        val actualOOSEventsCounter = resourceEventStore.countOutOfSyncResourceEventsForBillingPeriod(
          userID,
          billingMonthStartMillis,
          billingMonthStopMillis)

        val counterDiff = actualOOSEventsCounter - latestStateOOSEventsCounter
        counterDiff match {
          // ZERO, we are OK!
          case 0 ⇒
            // NOTE: Keep the caller's calculation reason
            val userStateModel = latestUserState.newWithChargingReason(chargingReason)
            userStateModel.toWorkingUserState(defaultResourceTypesMap)

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
   *
   * @param resourceEvent
   * @param workingUserState
   * @param chargingReason
   * @param billingMonthInfo
   */
  def processResourceEvent(
      resourceEvent: ResourceEventModel,
      workingUserState: WorkingUserState,
      chargingReason: ChargingReason,
      billingMonthInfo: BillingMonthInfo,
      updateLatestMillis: Boolean
  ): Boolean = {

    val resourceTypeName = resourceEvent.resource
    val resourceTypeOpt = workingUserState.findResourceType(resourceTypeName)
    if(resourceTypeOpt.isEmpty) {
      // Unknown (yet) resource, ignoring event.
      return false
    }
    val resourceType = resourceTypeOpt.get

    val chargingBehavior = aquarium.chargingBehaviorOf(resourceType)
    val workingResourcesState = workingUserState.workingStateOfResources.get(resourceTypeName) match {
      case Some(existingState) ⇒
        existingState

      case None ⇒
        // First time for this ChargingBehavior.
        val newState = new WorkingResourcesChargingState(
          details = mutable.Map(chargingBehavior.initialChargingDetails.toSeq:_*),
          stateOfResourceInstance = mutable.Map()
        )

        workingUserState.workingStateOfResources(resourceTypeName) = newState
        newState
    }

    val m0 = TimeHelpers.nowMillis()
    val (walletEntriesCount, newTotalCredits) = chargingBehavior.processResourceEvent(
      aquarium,
      resourceEvent,
      resourceType,
      billingMonthInfo,
      workingResourcesState,
      workingUserState.workingAgreementHistory,
      workingUserState.totalCredits,
      workingUserState.walletEntries += _
    )
    val m1 = TimeHelpers.nowMillis()

    if(updateLatestMillis) {
      workingUserState.latestUpdateMillis = m1
    }

    workingUserState.updateLatestResourceEventOccurredMillis(resourceEvent.occurredMillis)
    workingUserState.totalCredits = newTotalCredits

    true
  }

  def processResourceEvents(
      resourceEvents: Traversable[ResourceEventModel],
      workingUserState: WorkingUserState,
      chargingReason: ChargingReason,
      billingMonthInfo: BillingMonthInfo
  ): Unit = {

    var _counter = 0
    for(currentResourceEvent ← resourceEvents) {
      processResourceEvent(
        currentResourceEvent,
        workingUserState,
        chargingReason,
        billingMonthInfo,
        false
      )

      _counter += 1
    }

    if(_counter > 0) {
      workingUserState.latestUpdateMillis = TimeHelpers.nowMillis()
    }
  }

  def replayFullMonthBilling(
      userStateBootstrap: UserStateBootstrap,
      billingMonthInfo: BillingMonthInfo,
      defaultResourceTypesMap: Map[String, ResourceType],
      chargingReason: ChargingReason,
      userStateRecorder: UserStateModel ⇒ UserStateModel
  ): WorkingUserState = {

    replayMonthChargingUpTo(
      billingMonthInfo,
      billingMonthInfo.monthStopMillis,
      userStateBootstrap,
      defaultResourceTypesMap,
      chargingReason,
      userStateRecorder
    )
  }

  /**
   * Replays the charging procedure over the set of resource events that happened within the given month and up to
   * the specified point in time.
   *
   * @param billingMonthInfo Which month to bill.
   * @param billingEndTimeMillis Bill from start of month up to (and including) this time.
   * @param userStateBootstrap
   * @param resourceTypesMap
   * @param chargingReason
   * @param userStateRecorder
   * @return
   */
  def replayMonthChargingUpTo(
      billingMonthInfo: BillingMonthInfo,
      billingEndTimeMillis: Long,
      userStateBootstrap: UserStateBootstrap,
      resourceTypesMap: Map[String, ResourceType],
      chargingReason: ChargingReason,
      userStateRecorder: UserStateModel ⇒ UserStateModel
  ): WorkingUserState = {

    val isFullMonthBilling = billingEndTimeMillis == billingMonthInfo.monthStopMillis
    val userID = userStateBootstrap.userID

    Debug(logger, "%s", chargingReason)

    // In order to replay the full month, we start with the state at the beginning of the month.
    val previousBillingMonthInfo = billingMonthInfo.previousMonth
    val workingUserState = findOrCalculateWorkingUserStateAtEndOfBillingMonth(
      previousBillingMonthInfo,
      userStateBootstrap,
      resourceTypesMap,
      chargingReason,
      userStateRecorder
    )

    // FIXME the below comments
    // Keep the working (current) user state. This will get updated as we proceed with billing for the month
    // specified in the parameters.
    // NOTE: The calculation reason is not the one we get from the previous user state but the one our caller specifies

    Debug(logger, "workingUserState=%s", workingUserState)
    Debug(logger, "previousBillingMonthUserState(%s) = %s",
      previousBillingMonthInfo.toShortDebugString,
      workingUserState
    )

    var _rcEventsCounter = 0
    resourceEventStore.foreachResourceEventOccurredInPeriod(
      userID,
      billingMonthInfo.monthStartMillis, // from start of month
      billingEndTimeMillis               // to requested time
    ) { currentResourceEvent ⇒

      Debug(logger, "Processing %s", currentResourceEvent)

      processResourceEvent(
        currentResourceEvent,
        workingUserState,
        chargingReason,
        billingMonthInfo,
        false
      )

      _rcEventsCounter += 1
    }

    if(_rcEventsCounter > 0) {
      workingUserState.latestUpdateMillis = TimeHelpers.nowMillis()
    }

    Debug(logger, "Found %s resource events for month %s",
      _rcEventsCounter,
      billingMonthInfo.toShortDebugString
    )

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

    workingUserState
  }
}
