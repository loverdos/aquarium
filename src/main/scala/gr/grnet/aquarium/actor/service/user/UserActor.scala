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

import gr.grnet.aquarium.AquariumInternalError
import gr.grnet.aquarium.actor.message.GetUserBalanceRequest
import gr.grnet.aquarium.actor.message.GetUserBalanceResponse
import gr.grnet.aquarium.actor.message.GetUserBalanceResponseData
import gr.grnet.aquarium.actor.message.GetUserBillRequest
import gr.grnet.aquarium.actor.message.GetUserBillResponse
import gr.grnet.aquarium.actor.message.GetUserBillResponseData
import gr.grnet.aquarium.actor.message.GetUserStateRequest
import gr.grnet.aquarium.actor.message.GetUserStateResponse
import gr.grnet.aquarium.actor.message.GetUserWalletRequest
import gr.grnet.aquarium.actor.message.GetUserWalletResponse
import gr.grnet.aquarium.actor.message.GetUserWalletResponseData
import gr.grnet.aquarium.actor.message.config.AquariumPropertiesLoaded
import gr.grnet.aquarium.actor.message.config.InitializeUserActorState
import gr.grnet.aquarium.charging.bill.AbstractBillEntry
import gr.grnet.aquarium.charging.state.{UserStateModel, UserAgreementHistoryModel, UserStateBootstrap}
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.message.avro.gen.{IMEventMsg, ResourceEventMsg, UserStateMsg}
import gr.grnet.aquarium.message.avro.{ModelFactory, MessageFactory, MessageHelpers, AvroHelpers}
import gr.grnet.aquarium.service.event.BalanceEvent
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.util.{LogHelpers, shortClassNameOf}
import gr.grnet.aquarium.policy.{ResourceType, PolicyModel}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class UserActor extends ReflectiveRoleableActor {
  private[this] var _userID: String = "<?>"
  private[this] var _userState: UserStateModel = _
  private[this] var _userCreationIMEvent: IMEventMsg = _
  private[this] var _userAgreementHistoryModel: UserAgreementHistoryModel = _
  private[this] var _latestIMEventOriginalID: String = ""
  private[this] var _latestResourceEventOriginalID: String = ""
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

  private[this] def stdUserStateStoreFunc = (userState: UserStateMsg) ⇒ {
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
    (this._userAgreementHistoryModel ne null) && this._userAgreementHistoryModel.size > 0
  }

  @inline private[this] def haveUserState = {
    this._userState ne null
  }

  @inline private[this] def haveUserStateBootstrap = {
    this._userStateBootstrap ne null
  }

  private[this] def updateAgreementHistoryFrom(imEvent: IMEventMsg): Unit = {
    val isCreateUser = MessageHelpers.isIMEventCreate(imEvent)
    if(isCreateUser) {
      if(haveUserCreationIMEvent) {
        throw new AquariumInternalError(
          "Got user creation event (id=%s) but I already have one (id=%s)",
            this._userCreationIMEvent.getOriginalID,
            imEvent.getOriginalID
        )
      }

      this._userCreationIMEvent = imEvent
    }

    val effectiveFromMillis = imEvent.getOccurredMillis
    val role = imEvent.getRole
    // calling unsafe just for the side-effect
    assert(null ne aquarium.unsafeFullPriceTableForRoleAt(role, effectiveFromMillis))

    // add to model (will update the underlying messages as well)
    if(this._userAgreementHistoryModel eq null) {
      this._userAgreementHistoryModel = ModelFactory.newUserAgreementHistoryModelFromIMEvent(imEvent, imEvent.getOriginalID)
    } else {
      val newUserAgreementModel = ModelFactory.newUserAgreementModelFromIMEvent(imEvent, imEvent.getOriginalID)
      this._userAgreementHistoryModel += newUserAgreementModel
    }
  }

  private[this] def updateLatestIMEventIDFrom(imEvent: IMEventMsg): Unit = {
    this._latestIMEventOriginalID = imEvent.getOriginalID
  }

  private[this] def updateLatestResourceEventIDFrom(rcEvent: ResourceEventMsg): Unit = {
    this._latestResourceEventOriginalID = rcEvent.getOriginalID
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
      DEBUG("Initial agreement history %s", this._userAgreementHistoryModel.toJsonString)
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

  private[this] def loadUserStateAndUpdateAgreementHistory(): Unit = {
    assert(this.haveAgreements, "this.haveAgreements")
    assert(this.haveUserCreationIMEvent, "this.haveUserCreationIMEvent")

    if(!haveUserStateBootstrap) {
      this._userStateBootstrap = aquarium.getUserStateBootstrap(this._userCreationIMEvent)
    }
    logger.debug("#### this._userStateBootStrap %s".format(this._userStateBootstrap.toString))
    val now = TimeHelpers.nowMillis()
    this._userState = chargingService.replayMonthChargingUpTo(
      BillingMonthInfo.fromMillis(now),
      now,
      this._userStateBootstrap,
      aquarium.currentResourceTypesMap,
      aquarium.userStateStore.insertUserState
    )

    // Final touch: Update agreement history in the working user state.
    // The assumption is that all agreement changes go via IMEvents, so the
    // state this._workingAgreementHistory is always the authoritative source.
    if(haveUserState) {
      this._userState.userAgreementHistoryModel = this._userAgreementHistoryModel
      DEBUG("Computed working user state %s", AvroHelpers.jsonStringOfSpecificRecord(this._userState.msg))
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
    loadUserStateAndUpdateAgreementHistory()

    if(haveUserState) {
      DEBUG("Initial working user state %s", AvroHelpers.jsonStringOfSpecificRecord(this._userState.msg))
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
   * Process [[gr.grnet.aquarium.message.avro.gen.IMEventMsg]]s.
   * When this method is called, we assume that all proper checks have been made and it
   * is OK to proceed with the event processing.
   */
  def onIMEventMsg(imEvent: IMEventMsg): Unit = {
    val hadUserCreationIMEvent = haveUserCreationIMEvent

    if(!haveAgreements) {
      // This IMEvent has arrived after any ResourceEvents
      INFO("Arrived after any ResourceEvent: %s", AvroHelpers.jsonStringOfSpecificRecord(imEvent))
      initializeStateOfIMEvents()
    }
    else {
      if(this._latestIMEventOriginalID == imEvent.getOriginalID) {
        // This happens when the actor is brought to life, then immediately initialized, and then
        // sent the first IM event. But from the initialization procedure, this IM event will have
        // already been loaded from DB!
        INFO("Ignoring first %s", AvroHelpers.jsonStringOfSpecificRecord(imEvent))
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
      loadUserStateAndUpdateAgreementHistory()
    }

    logSeparator()
  }

  def onResourceEventMsg(rcEvent: ResourceEventMsg): Unit = {
    if(!shouldProcessResourceEvents) {
      // This means the user has not been created (at least, as far as Aquarium is concerned).
      // So, we do not process any resource event
      DEBUG("Not processing %s", AvroHelpers.jsonStringOfSpecificRecord(rcEvent))
      logSeparator()

      return
    }

    // Since the latest resource event per resource is recorded in the user state,
    // we do not need to query the store. Just query the in-memory state.
    // Note: This is a similar situation with the first IMEvent received right after the user
    //       actor is created.
    if(this._latestResourceEventOriginalID == rcEvent.getOriginalID) {
      INFO("Ignoring first %s", AvroHelpers.jsonStringOfSpecificRecord(rcEvent))
      logSeparator()

      return
    }

    val now = TimeHelpers.nowMillis()
    // TODO: Review this and its usage in user state.
    // TODO: The assumption is that the resource set increases all the time,
    // TODO: so the current map contains everything ever known (assuming we do not run backwards in time).
    val currentResourcesMap = aquarium.currentResourceTypesMap

    val nowBillingMonthInfo = BillingMonthInfo.fromMillis(now)
    val nowYear = nowBillingMonthInfo.year
    val nowMonth = nowBillingMonthInfo.month

    val eventOccurredMillis = rcEvent.getOccurredMillis
    val eventBillingMonthInfo = BillingMonthInfo.fromMillis(eventOccurredMillis)
    val eventYear = eventBillingMonthInfo.year
    val eventMonth = eventBillingMonthInfo.month

    def computeBatch(): Unit = {
      DEBUG("Going for out of sync charging for %s", rcEvent.getOriginalID)
      this._userState = chargingService.replayMonthChargingUpTo(
        nowBillingMonthInfo,
        // Take into account that the event may be out-of-sync.
        // TODO: Should we use this._latestResourceEventOccurredMillis instead of now?
        now max eventOccurredMillis,
        this._userStateBootstrap,
        currentResourcesMap,
        stdUserStateStoreFunc
      )

      updateLatestResourceEventIDFrom(rcEvent)
    }

    def computeRealtime(): Unit = {
      DEBUG("Going for in sync charging for %s", rcEvent.getOriginalID)
      chargingService.processResourceEvent(
        rcEvent,
        this._userState,
        nowBillingMonthInfo,
        true
      )

      updateLatestResourceEventIDFrom(rcEvent)
    }

    val oldTotalCredits =
      if(this._userState!=null)
        this._userState.totalCredits
      else
        0.0D
    // FIXME check these
    if(this._userState eq null) {
      computeBatch()
    }
    else if(nowYear != eventYear || nowMonth != eventMonth) {
      DEBUG(
        "nowYear(%s) != eventYear(%s) || nowMonth(%s) != eventMonth(%s)",
        nowYear, eventYear,
        nowMonth, eventMonth
      )
      computeBatch()
    }
    else if(this._userState.latestResourceEventOccurredMillis < rcEvent.getOccurredMillis) {
      DEBUG("this._workingUserState.latestResourceEventOccurredMillis < rcEvent.occurredMillis")
      DEBUG(
        "%s < %s",
        TimeHelpers.toYYYYMMDDHHMMSSSSS(this._userState.latestResourceEventOccurredMillis),
        TimeHelpers.toYYYYMMDDHHMMSSSSS(rcEvent.getOccurredMillis)
      )
      computeRealtime()
    }
    else {
      DEBUG("OUT OF ORDER! this._workingUserState.latestResourceEventOccurredMillis=%s  and rcEvent.occurredMillis=%s",
                TimeHelpers.toYYYYMMDDHHMMSSSSS(this._userState.latestResourceEventOccurredMillis),
                TimeHelpers.toYYYYMMDDHHMMSSSSS(rcEvent.getOccurredMillis))

      computeBatch()
    }
    val newTotalCredits = this._userState.totalCredits
    if(oldTotalCredits * newTotalCredits < 0)
      aquarium.eventBus ! new BalanceEvent(this._userState.userID,
                                           newTotalCredits>=0)
    DEBUG("Updated %s", this._userState)
    logSeparator()
  }

  def onGetUserBillRequest(event: GetUserBillRequest): Unit = {
    try{
      val timeslot = event.timeslot
      val resourceTypes = aquarium.policyStore.
                          loadSortedPolicyModelsWithin(timeslot.from.getTime,
                                                       timeslot.to.getTime).
                          values.headOption match {
          case None => Map[String,ResourceType]()
          case Some(policy:PolicyModel) => policy.resourceTypesMap
      }
      val state= if(haveUserState) Some(this._userState.msg) else None
      val billEntry = AbstractBillEntry.fromWorkingUserState(timeslot,this._userID,state,resourceTypes)
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

    (haveUserCreationIMEvent, haveUserState) match {
      case (true, true) ⇒
        // (User CREATEd, with balance state)
        val realtimeMillis = TimeHelpers.nowMillis()
        chargingService.calculateRealtimeUserState(
          this._userState,
          BillingMonthInfo.fromMillis(realtimeMillis),
          realtimeMillis
        )

        sender ! GetUserBalanceResponse(Right(GetUserBalanceResponseData(this._userID, this._userState.totalCredits)))

      case (true, false) ⇒
        // (User CREATEd, no balance state)
        // Return the default initial balance
        sender ! GetUserBalanceResponse(
          Right(
            GetUserBalanceResponseData(
              this._userID,
              aquarium.initialUserBalance(this._userCreationIMEvent.getRole, this._userCreationIMEvent.getOccurredMillis)
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
    haveUserState match {
      case true ⇒
        val realtimeMillis = TimeHelpers.nowMillis()
        chargingService.calculateRealtimeUserState(
          this._userState,
          BillingMonthInfo.fromMillis(realtimeMillis),
          realtimeMillis
        )

        sender ! GetUserStateResponse(Right(this._userState.msg))

      case false ⇒
        sender ! GetUserStateResponse(Left("No state for user %s [AQU-STA-0006]".format(event.userID)), 404)
    }
  }

  def onGetUserWalletRequest(event: GetUserWalletRequest): Unit = {
    haveUserState match {
      case true ⇒
        DEBUG("haveWorkingUserState: %s", event)
        val realtimeMillis = TimeHelpers.nowMillis()
        chargingService.calculateRealtimeUserState(
          this._userState,
          BillingMonthInfo.fromMillis(realtimeMillis),
          realtimeMillis
        )

        sender ! GetUserWalletResponse(
          Right(
            GetUserWalletResponseData(
              this._userID,
              this._userState.totalCredits,
              MessageFactory.newWalletEntriesMsg(this._userState.msg.getWalletEntries)
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
                  aquarium.initialUserBalance(this._userCreationIMEvent.getRole, this._userCreationIMEvent.getOccurredMillis),
                  MessageFactory.newWalletEntriesMsg()
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
