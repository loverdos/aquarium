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

package gr.grnet.aquarium.actor
package service
package user

import gr.grnet.aquarium.actor._

import gr.grnet.aquarium.actor.message.event.{ProcessResourceEvent, ProcessIMEvent}
import gr.grnet.aquarium.actor.message.config.{InitializeUserActorState, AquariumPropertiesLoaded}
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.service.event.BalanceEvent
import gr.grnet.aquarium.event.model.im.IMEventModel
import message._
import config.AquariumPropertiesLoaded
import config.InitializeUserActorState
import event.ProcessIMEvent
import event.ProcessResourceEvent
import gr.grnet.aquarium.util.{LogHelpers, shortClassNameOf}
import gr.grnet.aquarium.{Aquarium, AquariumInternalError}
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.charging.state.UserStateBootstrap
import gr.grnet.aquarium.charging.state.{WorkingAgreementHistory, WorkingUserState, UserStateModel}
import gr.grnet.aquarium.charging.reason.{InitialUserActorSetup, RealtimeChargingReason}
import gr.grnet.aquarium.policy.{PolicyDefinedFullPriceTableRef, StdUserAgreement}
import gr.grnet.aquarium.event.model.resource.ResourceEventModel
import message.GetUserBalanceRequest
import message.GetUserBalanceResponse
import message.GetUserBalanceResponseData
import message.GetUserStateRequest
import message.GetUserStateResponse
import message.GetUserWalletRequest
import message.GetUserWalletResponse
import message.GetUserWalletResponseData
import scala.Left
import gr.grnet.aquarium.charging.state.WorkingAgreementHistory
import scala.Some
import scala.Right
import gr.grnet.aquarium.policy.StdUserAgreement
import gr.grnet.aquarium.charging.state.UserStateBootstrap
import gr.grnet.aquarium.charging.bill.BillEntry

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class UserActor extends ReflectiveRoleableActor {
  private[this] var _userID: String = "<?>"
  private[this] var _workingUserState: WorkingUserState = _
  private[this] var _userCreationIMEvent: IMEventModel = _
  private[this] val _workingAgreementHistory: WorkingAgreementHistory = new WorkingAgreementHistory
  private[this] var _latestIMEventID: String = ""
  private[this] var _latestResourceEventID: String = ""
  private[this] var _userStateBootstrap: UserStateBootstrap = _

  def unsafeUserID = {
    if(!haveUserID) {
      throw new AquariumInternalError("%s not initialized")
    }

    this._userID
  }

  override def postStop() {
    DEBUG("I am finally stopped (in postStop())")
    aquarium.akkaService.notifyUserActorPostStop(this)
  }

  private[this] def shutmedown(): Unit = {
    if(haveUserID) {
      aquarium.akkaService.invalidateUserActor(this)
    }
  }

  override protected def onThrowable(t: Throwable, message: AnyRef) = {
    LogHelpers.logChainOfCauses(logger, t)
    ERROR(t, "Terminating due to: %s(%s)", shortClassNameOf(t), t.getMessage)

    shutmedown()
  }

  def role = UserActorRole

  private[this] def chargingService = aquarium.chargingService

  private[this] def stdUserStateStoreFunc = (userState: UserStateModel) ⇒ {
    aquarium.userStateStore.insertUserState(userState)
  }

  @inline private[this] def haveUserID = {
    this._userID ne null
  }

  @inline private[this] def haveUserCreationIMEvent = {
    this._userCreationIMEvent ne null
  }

  def onAquariumPropertiesLoaded(event: AquariumPropertiesLoaded): Unit = {
  }

  @inline private[this] def haveAgreements = {
    this._workingAgreementHistory.size > 0
  }

  @inline private[this] def haveWorkingUserState = {
    this._workingUserState ne null
  }

  @inline private[this] def haveUserStateBootstrap = {
    this._userStateBootstrap ne null
  }

  private[this] def updateAgreementHistoryFrom(imEvent: IMEventModel): Unit = {
    if(imEvent.isCreateUser) {
      if(haveUserCreationIMEvent) {
        throw new AquariumInternalError(
          "Got user creation event (id=%s) but I already have one (id=%s)",
            this._userCreationIMEvent.id,
            imEvent.id
        )
      }

      this._userCreationIMEvent = imEvent
    }

    val effectiveFromMillis = imEvent.occurredMillis
    val role = imEvent.role
    // calling unsafe just for the side-effect
    assert(null ne aquarium.unsafePriceTableForRoleAt(role, effectiveFromMillis))

    val newAgreement = StdUserAgreement(
      imEvent.id,
      Some(imEvent.id),
      effectiveFromMillis,
      Long.MaxValue,
      role,
      PolicyDefinedFullPriceTableRef
    )

    this._workingAgreementHistory += newAgreement
  }

  private[this] def updateLatestIMEventIDFrom(imEvent: IMEventModel): Unit = {
    this._latestIMEventID = imEvent.id
  }

  private[this] def updateLatestResourceEventIDFrom(rcEvent: ResourceEventModel): Unit = {
    this._latestResourceEventID = rcEvent.id
  }

  /**
   * Creates the initial state that is related to IMEvents.
   */
  private[this] def initializeStateOfIMEvents(): Unit = {
    // NOTE: this._userID is already set up by onInitializeUserActorState()
    aquarium.imEventStore.foreachIMEventInOccurrenceOrder(this._userID) { imEvent ⇒
      DEBUG("Replaying %s", imEvent)

      updateAgreementHistoryFrom(imEvent)
      updateLatestIMEventIDFrom(imEvent)
    }

    if(haveAgreements) {
      DEBUG("Initial agreement history %s", this._workingAgreementHistory.toJsonString)
      logSeparator()
    }
  }

  /**
   * Resource events are processed only if the user has been created and has agreements.
   * Otherwise nothing can be computed.
   */
  private[this] def shouldProcessResourceEvents: Boolean = {
    haveUserCreationIMEvent && haveAgreements && haveUserStateBootstrap
  }

  private[this] def loadWorkingUserStateAndUpdateAgreementHistory(): Unit = {
    assert(this.haveAgreements, "this.haveAgreements")
    assert(this.haveUserCreationIMEvent, "this.haveUserCreationIMEvent")

    val userCreationMillis = this._userCreationIMEvent.occurredMillis
    val userCreationRole = this._userCreationIMEvent.role // initial role
    val userCreationIMEventID = this._userCreationIMEvent.id

    if(!haveUserStateBootstrap) {
      this._userStateBootstrap = UserStateBootstrap(
        this._userID,
        userCreationMillis,
        aquarium.initialUserAgreement(userCreationRole, userCreationMillis, Some(userCreationIMEventID)),
        aquarium.initialUserBalance(userCreationRole, userCreationMillis)
      )
    }

    val now = TimeHelpers.nowMillis()
    this._workingUserState = chargingService.replayMonthChargingUpTo(
      BillingMonthInfo.fromMillis(now),
      now,
      this._userStateBootstrap,
      aquarium.currentResourceTypesMap,
      InitialUserActorSetup(),
      aquarium.userStateStore.insertUserState,
      None
    )

    // Final touch: Update agreement history in the working user state.
    // The assumption is that all agreement changes go via IMEvents, so the
    // state this._workingAgreementHistory is always the authoritative source.
    if(haveWorkingUserState) {
      this._workingUserState.workingAgreementHistory.setFrom(this._workingAgreementHistory)
      DEBUG("Computed working user state %s", this._workingUserState.toJsonString)
    }
  }

  private[this] def initializeStateOfResourceEvents(event: InitializeUserActorState): Unit = {
    if(!this.haveAgreements) {
      DEBUG("Cannot initializeResourceEventsState() from %s. There are no agreements", event)
      return
    }

    if(!this.haveUserCreationIMEvent) {
      DEBUG("Cannot initializeResourceEventsState() from %s. I never got a CREATE IMEvent", event)
      return
    }

    // We will also need this functionality when receiving IMEvents, so we place it in a method
    loadWorkingUserStateAndUpdateAgreementHistory()

    if(haveWorkingUserState) {
      DEBUG("Initial working user state %s", this._workingUserState.toJsonString)
      logSeparator()
    }
  }

  def onInitializeUserActorState(event: InitializeUserActorState): Unit = {
    this._userID = event.userID
    DEBUG("Got %s", event)

    initializeStateOfIMEvents()
    initializeStateOfResourceEvents(event)
  }

  /**
   * Process [[gr.grnet.aquarium.event.model.im.IMEventModel]]s.
   * When this method is called, we assume that all proper checks have been made and it
   * is OK to proceed with the event processing.
   */
  def onProcessIMEvent(processEvent: ProcessIMEvent): Unit = {
    val imEvent = processEvent.imEvent
    val hadUserCreationIMEvent = haveUserCreationIMEvent

    if(!haveAgreements) {
      // This IMEvent has arrived after any ResourceEvents
      INFO("Arrived after any ResourceEvent: %s", imEvent.toDebugString)
      initializeStateOfIMEvents()
    }
    else {
      if(this._latestIMEventID == imEvent.id) {
        // This happens when the actor is brought to life, then immediately initialized, and then
        // sent the first IM event. But from the initialization procedure, this IM event will have
        // already been loaded from DB!
        INFO("Ignoring first %s", imEvent.toDebugString)
        logSeparator()

        //this._latestIMEventID = imEvent.id
        return
      }

      updateAgreementHistoryFrom(imEvent)
      updateLatestIMEventIDFrom(imEvent)
    }

    // Must also update user state if we know when in history the life of a user begins
    if(!hadUserCreationIMEvent && haveUserCreationIMEvent) {
      INFO("Processing user state, since we had a CREATE IMEvent")
      loadWorkingUserStateAndUpdateAgreementHistory()
    }

    logSeparator()
  }

  def onProcessResourceEvent(event: ProcessResourceEvent): Unit = {
    val rcEvent = event.rcEvent

    if(!shouldProcessResourceEvents) {
      // This means the user has not been created (at least, as far as Aquarium is concerned).
      // So, we do not process any resource event
      DEBUG("Not processing %s", rcEvent.toJsonString)
      logSeparator()

      return
    }

    // Since the latest resource event per resource is recorded in the user state,
    // we do not need to query the store. Just query the in-memory state.
    // Note: This is a similar situation with the first IMEvent received right after the user
    //       actor is created.
    if(this._latestResourceEventID == rcEvent.id) {
      INFO("Ignoring first %s", rcEvent.toDebugString)
      logSeparator()

      return
    }

    val now = TimeHelpers.nowMillis()
    val currentResourcesMap = aquarium.currentResourceTypesMap
    val chargingReason = RealtimeChargingReason(None, now)

    val nowBillingMonthInfo = BillingMonthInfo.fromMillis(now)
    val nowYear = nowBillingMonthInfo.year
    val nowMonth = nowBillingMonthInfo.month

    val eventOccurredMillis = rcEvent.occurredMillis
    val eventBillingMonthInfo = BillingMonthInfo.fromMillis(eventOccurredMillis)
    val eventYear = eventBillingMonthInfo.year
    val eventMonth = eventBillingMonthInfo.month

    def computeBatch(): Unit = {
      DEBUG("Going for out of sync charging")
      this._workingUserState = chargingService.replayMonthChargingUpTo(
        nowBillingMonthInfo,
        // Take into account that the event may be out-of-sync.
        // TODO: Should we use this._latestResourceEventOccurredMillis instead of now?
        now max eventOccurredMillis,
        this._userStateBootstrap,
        currentResourcesMap,
        chargingReason,
        stdUserStateStoreFunc,
        None
      )

      updateLatestResourceEventIDFrom(rcEvent)
    }

    def computeRealtime(): Unit = {
      DEBUG("Going for in sync charging")
      chargingService.processResourceEvent(
        rcEvent,
        this._workingUserState,
        chargingReason,
        nowBillingMonthInfo,
        None,
        true
      )

      updateLatestResourceEventIDFrom(rcEvent)
    }

    var oldTotalCredits = this._workingUserState.totalCredits
    // FIXME check these
    if(nowYear != eventYear || nowMonth != eventMonth) {
      DEBUG(
        "nowYear(%s) != eventYear(%s) || nowMonth(%s) != eventMonth(%s)",
        nowYear, eventYear,
        nowMonth, eventMonth
      )
      computeBatch()
    }
    else if(this._workingUserState.latestResourceEventOccurredMillis < rcEvent.occurredMillis) {
      DEBUG("this._workingUserState.latestResourceEventOccurredMillis < rcEvent.occurredMillis")
      DEBUG(
        "%s < %s",
        TimeHelpers.toYYYYMMDDHHMMSSSSS(this._workingUserState.latestResourceEventOccurredMillis),
        TimeHelpers.toYYYYMMDDHHMMSSSSS(rcEvent.occurredMillis)
      )
      computeRealtime()
    }
    else {
      computeBatch()
    }
    if(oldTotalCredits * this._workingUserState.totalCredits < 0)
      aquarium.eventBus ! new BalanceEvent(this._workingUserState.userID,
                                           this._workingUserState.totalCredits>=0)
    DEBUG("Updated %s", this._workingUserState)
    logSeparator()
  }

  def onGetUserBillRequest(event: GetUserBillRequest): Unit = {
    try{
      val timeslot = event.timeslot
      val state= if(haveWorkingUserState) Some(this._workingUserState) else None
      val billEntry = BillEntry.fromWorkingUserState(timeslot,state)
      val billData = GetUserBillResponseData(this._userID,billEntry)
      sender ! GetUserBillResponse(Right(billData))
    } catch {
      case e:Exception =>
       e.printStackTrace()
       sender ! GetUserBillResponse(Left("Internal Server Error [AQU-BILL-0001]"), 500)
    }
  }

  def onGetUserBalanceRequest(event: GetUserBalanceRequest): Unit = {
    val userID = event.userID

    (haveUserCreationIMEvent, haveWorkingUserState) match {
      case (true, true) ⇒
        // (User CREATEd, with balance state)
        sender ! GetUserBalanceResponse(Right(GetUserBalanceResponseData(this._userID, this._workingUserState.totalCredits)))

      case (true, false) ⇒
        // (User CREATEd, no balance state)
        // Return the default initial balance
        sender ! GetUserBalanceResponse(
          Right(
            GetUserBalanceResponseData(
              this._userID,
              aquarium.initialUserBalance(this._userCreationIMEvent.role, this._userCreationIMEvent.occurredMillis)
        )))

      case (false, true) ⇒
        // (Not CREATEd, with balance state)
        // Clearly this is internal error
        sender ! GetUserBalanceResponse(Left("Internal Server Error [AQU-BAL-0001]"), 500)

      case (false, false) ⇒
        // (Not CREATEd, no balance state)
        // The user is completely unknown
        sender ! GetUserBalanceResponse(Left("Unknown user %s [AQU-BAL-0004]".format(userID)), 404/*Not found*/)
    }
  }

  def onGetUserStateRequest(event: GetUserStateRequest): Unit = {
    haveWorkingUserState match {
      case true ⇒
        sender ! GetUserStateResponse(Right(this._workingUserState))

      case false ⇒
        sender ! GetUserStateResponse(Left("No state for user %s [AQU-STA-0006]".format(event.userID)), 404)
    }
  }

  def onGetUserWalletRequest(event: GetUserWalletRequest): Unit = {
    haveWorkingUserState match {
      case true ⇒
        DEBUG("haveWorkingUserState: %s", event)
        sender ! GetUserWalletResponse(
          Right(
            GetUserWalletResponseData(
              this._userID,
              this._workingUserState.totalCredits,
              this._workingUserState.walletEntries.toList
        )))

      case false ⇒
        DEBUG("!haveWorkingUserState: %s", event)
        haveUserCreationIMEvent match {
          case true ⇒
            DEBUG("haveUserCreationIMEvent: %s", event)
            sender ! GetUserWalletResponse(
              Right(
                GetUserWalletResponseData(
                  this._userID,
                  aquarium.initialUserBalance(this._userCreationIMEvent.role, this._userCreationIMEvent.occurredMillis),
                  Nil
            )))

          case false ⇒
            DEBUG("!haveUserCreationIMEvent: %s", event)
            sender ! GetUserWalletResponse(Left("No wallet for user %s [AQU-WAL-00 8]".format(event.userID)), 404)
        }
    }
  }

  private[this] def D_userID = {
    this._userID
  }

  private[this] def DEBUG(fmt: String, args: Any*) =
    logger.debug("[%s] - %s".format(D_userID, fmt.format(args: _*)))

  private[this] def INFO(fmt: String, args: Any*) =
    logger.info("[%s] - %s".format(D_userID, fmt.format(args: _*)))

  private[this] def WARN(fmt: String, args: Any*) =
    logger.warn("[%s] - %s".format(D_userID, fmt.format(args: _*)))

  private[this] def ERROR(fmt: String, args: Any*) =
    logger.error("[%s] - %s".format(D_userID, fmt.format(args: _*)))

  private[this] def ERROR(t: Throwable, fmt: String, args: Any*) =
    logger.error("[%s] - %s".format(D_userID, fmt.format(args: _*)), t)
}
