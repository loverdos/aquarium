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
import gr.grnet.aquarium.charging.state.{UserStateModel, UserAgreementHistoryModel, UserStateBootstrap}
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.message.avro.gen.{ResourceTypeMsg, UserAgreementHistoryMsg, IMEventMsg, ResourceEventMsg, UserStateMsg}
import gr.grnet.aquarium.message.avro.{ModelFactory, MessageFactory, MessageHelpers, AvroHelpers}
import gr.grnet.aquarium.service.event.BalanceEvent
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.util.{LogHelpers, shortClassNameOf}
import gr.grnet.aquarium.policy.{ResourceType, PolicyModel}
import gr.grnet.aquarium.charging.bill.BillEntryMsg
import gr.grnet.aquarium.event.CreditsModel
import java.util

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class UserActor extends ReflectiveRoleableActor {
  private[this] var _rcMsgCount = 0
  private[this] var _imMsgCount = 0
  private[this] var _userID: String = "???"
  private[this] var _userStateMsg: UserStateMsg = _
  private[this] var _userAgreementHistoryModel: UserAgreementHistoryModel = _

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

  def onAquariumPropertiesLoaded(event: AquariumPropertiesLoaded): Unit = {
  }

  private[this] def haveUserID = this._userID ne null
  private[this] def unsafeUserCreationIMEventMsg = this._userAgreementHistoryModel.unsafeUserCreationIMEvent
  private[this] def haveAgreements = this._userAgreementHistoryModel ne null
  private[this] def isUserCreated = haveAgreements && this._userAgreementHistoryModel.hasUserCreationEvent
  private[this] def haveUserState = this._userStateMsg ne null

  private[this] def createInitialUserStateMsgFromCreateIMEvent() {
    assert(haveAgreements, "haveAgreements")
    assert(isUserCreated, "isUserCreated")
    assert(this._userAgreementHistoryModel.hasUserCreationEvent, "this._userAgreementHistoryModel.hasUserCreationEvent")

    val userCreationIMEventMsg = unsafeUserCreationIMEventMsg
    val userStateBootstrap = aquarium.getUserStateBootstrap(userCreationIMEventMsg)

    this._userStateMsg = MessageFactory.newInitialUserStateMsg(
      this._userID,
      CreditsModel.from(0.0),
      TimeHelpers.nowMillis()
    )
  }

  /**
   * Creates the agreement history from all the stored IMEvents.
   *
   * @return (`true` iff there was a user CREATE event, the number of events processed)
   */
  private[this] def createUserAgreementHistoryFromStoredIMEvents(): (Boolean, Int) = {
    DEBUG("createUserAgreementHistoryFromStoredIMEvents()")
    val historyMsg = MessageFactory.newUserAgreementHistoryMsg(this._userID)
    this._userAgreementHistoryModel = ModelFactory.newUserAgreementHistoryModel(historyMsg)

    var _imcounter = 0

    val hadCreateEvent = aquarium.imEventStore.foreachIMEventInOccurrenceOrder(this._userID) { imEvent ⇒
      _imcounter += 1
      DEBUG("Replaying [%s/%s] %s", shortClassNameOf(imEvent), _imcounter, imEvent)

      if(_imcounter == 1 && !MessageHelpers.isIMEventCreate(imEvent)) {
        // The very first event must be a CREATE event. Otherwise we abort initialization.
        // This will normally happen during devops :)
        INFO("Ignoring first %s since it is not CREATE", shortClassNameOf(imEvent))
        false
      }
      else {
        val effectiveFromMillis = imEvent.getOccurredMillis
        val role = imEvent.getRole
        // calling unsafe just for the side-effect
        assert(
          aquarium.unsafeFullPriceTableForRoleAt(role, effectiveFromMillis) ne null,
          "aquarium.unsafeFullPriceTableForRoleAt(%s, %s) ne null".format(role, effectiveFromMillis)
        )

        this._userAgreementHistoryModel.insertUserAgreementMsgFromIMEvent(imEvent)
        true
      }
    }

    DEBUG("Agreements: %s", this._userAgreementHistoryModel)
    (hadCreateEvent, _imcounter)
  }

  /**
   * Processes [[gr.grnet.aquarium.message.avro.gen.IMEventMsg]]s that come directly from the
   * messaging hub (rabbitmq).
   */
  def onIMEventMsg(imEvent: IMEventMsg) {
    if(!isUserCreated && MessageHelpers.isIMEventCreate(imEvent)) {
      assert(this._imMsgCount == 0, "this._imMsgCount == 0")
      // Create the full agreement history from the original sources (IMEvents)
      val (userCreated, imEventsCount) = createUserAgreementHistoryFromStoredIMEvents()

      this._imMsgCount = imEventsCount
      return
    }

    // Check for out of sync (regarding IMEvents)
    val isOutOfSyncIM = imEvent.getOccurredMillis < this._userAgreementHistoryModel.latestIMEventOccurredMillis
    if(isOutOfSyncIM) {
      // clear all resource state
      // FIXME implement

      return
    }

    // Check out of sync (regarding ResourceEvents)
    val isOutOfSyncRC = false // FIXME implement
    if(isOutOfSyncRC) {
      // TODO

      return
    }

    // OK, seems good
    assert(!MessageHelpers.isIMEventCreate(imEvent), "!MessageHelpers.isIMEventCreate(imEvent)")

    // Make new agreement
    this._userAgreementHistoryModel.insertUserAgreementMsgFromIMEvent(imEvent)
    this._imMsgCount += 1
    DEBUG("Agreements: %s", this._userAgreementHistoryModel)
  }

  def onResourceEventMsg(rcEvent: ResourceEventMsg) {
    if(!isUserCreated) {
      DEBUG("No agreements. Ignoring %s", rcEvent)

      return
    }

    val now = TimeHelpers.nowMillis()
    val resourceMapping = aquarium.resourceMappingAtMillis(now)

    val nowBillingMonthInfo = BillingMonthInfo.fromMillis(now)
    val nowYear = nowBillingMonthInfo.year
    val nowMonth = nowBillingMonthInfo.month

    val eventOccurredMillis = rcEvent.getOccurredMillis
    val eventBillingMonthInfo = BillingMonthInfo.fromMillis(eventOccurredMillis)
    val eventYear = eventBillingMonthInfo.year
    val eventMonth = eventBillingMonthInfo.month

    def computeBatch(): Unit = {
      DEBUG("Going for out of sync charging for %s", rcEvent.getOriginalID)

      this._userStateMsg = chargingService.replayMonthChargingUpTo(
        this._userAgreementHistoryModel,
        nowBillingMonthInfo,
        // Take into account that the event may be out-of-sync.
        // TODO: Should we use this._latestResourceEventOccurredMillis instead of now?
        now max eventOccurredMillis,
        resourceMapping,
        stdUserStateStoreFunc
      )

    }

    def computeRealtime(): Unit = {
      DEBUG("Going for in sync charging for %s", rcEvent.getOriginalID)
      chargingService.processResourceEvent(
        rcEvent,
        this._userAgreementHistoryModel,
        this._userStateMsg,
        nowBillingMonthInfo,
        true,
        resourceMapping
      )

      this._rcMsgCount += 1
    }

    val oldTotalCredits =
      if(this._userStateMsg!=null)
        this._userStateMsg.totalCredits
      else
        0.0D
    // FIXME check these
    if(this._userStateMsg eq null) {
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
    else if(this._userStateMsg.latestResourceEventOccurredMillis < rcEvent.getOccurredMillis) {
      DEBUG("this._workingUserState.latestResourceEventOccurredMillis < rcEvent.occurredMillis")
      DEBUG(
        "%s < %s",
        TimeHelpers.toYYYYMMDDHHMMSSSSS(this._userStateMsg.latestResourceEventOccurredMillis),
        TimeHelpers.toYYYYMMDDHHMMSSSSS(rcEvent.getOccurredMillis)
      )
      computeRealtime()
    }
    else {
      DEBUG("OUT OF ORDER! this._workingUserState.latestResourceEventOccurredMillis=%s  and rcEvent.occurredMillis=%s",
        TimeHelpers.toYYYYMMDDHHMMSSSSS(this._userStateMsg.latestResourceEventOccurredMillis),
        TimeHelpers.toYYYYMMDDHHMMSSSSS(rcEvent.getOccurredMillis))

      computeBatch()
    }
    val newTotalCredits = this._userStateMsg.totalCredits
    if(oldTotalCredits * newTotalCredits < 0)
      aquarium.eventBus ! new BalanceEvent(this._userStateMsg.userID,
        newTotalCredits>=0)
    DEBUG("Updated %s", this._userStateMsg)
    logSeparator()
  }

  def onGetUserBillRequest(event: GetUserBillRequest): Unit = {
    try{
      val timeslot = event.timeslot
      val resourceTypes: Map[String, ResourceType] = aquarium.policyStore.
                          loadSortedPolicyModelsWithin(timeslot.from.getTime,
                                                       timeslot.to.getTime).
                          values.headOption match {
          case None => Map[String,ResourceType]()
          case Some(policy:PolicyModel) => policy.resourceTypesMap
      }
      val state= if(haveUserState) Some(this._userStateMsg) else None
      val billEntryMsg = BillEntryMsg.fromWorkingUserState(timeslot,this._userID,state,resourceTypes)
      //val billEntryMsg = MessageFactory.createBillEntryMsg(billEntry)
      //logger.debug("BILL ENTRY MSG: " + billEntryMsg.toString)
      val billData = GetUserBillResponseData(this._userID,billEntryMsg)
      sender ! GetUserBillResponse(Right(billData))
    } catch {
      case e:Exception =>
        e.printStackTrace()
        sender ! GetUserBillResponse(Left("Internal Server Error [AQU-BILL-0001]"), 500)
    }
  }

  def onGetUserBalanceRequest(event: GetUserBalanceRequest): Unit = {
    val userID = event.userID

    (haveAgreements, haveUserState) match {
      case (true, true) ⇒
        // (User CREATEd, with balance state)
        val realtimeMillis = TimeHelpers.nowMillis()
        chargingService.calculateRealtimeUserState(
          this._userAgreementHistoryModel,
          this._userStateMsg,
          BillingMonthInfo.fromMillis(realtimeMillis),
          aquarium.resourceMappingAtMillis(realtimeMillis),
          realtimeMillis
        )

        sender ! GetUserBalanceResponse(Right(GetUserBalanceResponseData(this._userID, this._userStateMsg.totalCredits)))

      case (true, false) ⇒
        // (User CREATEd, no balance state)
        // Return the default initial balance
        sender ! GetUserBalanceResponse(
          Right(
            GetUserBalanceResponseData(
              this._userID,
              aquarium.initialUserBalance(this.unsafeUserCreationIMEventMsg.getRole, this.unsafeUserCreationIMEventMsg.getOccurredMillis)
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
          this._userAgreementHistoryModel,
          this._userStateMsg,
          BillingMonthInfo.fromMillis(realtimeMillis),
          aquarium.resourceMappingAtMillis(realtimeMillis),
          realtimeMillis
        )

        sender ! GetUserStateResponse(Right(this._userStateMsg))

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
          this._userAgreementHistoryModel,
          this._userStateMsg,
          BillingMonthInfo.fromMillis(realtimeMillis),
          aquarium.resourceMappingAtMillis(realtimeMillis),
          realtimeMillis
        )

        sender ! GetUserWalletResponse(
          Right(
            GetUserWalletResponseData(
              this._userID,
              this._userStateMsg.totalCredits,
              MessageFactory.newWalletEntriesMsg(this._userStateMsg.getWalletEntries)
            )))

      case false ⇒
        DEBUG("!haveWorkingUserState: %s", event)
        haveAgreements match {
          case true ⇒
            DEBUG("haveAgreements: %s", event)
            sender ! GetUserWalletResponse(
              Right(
                GetUserWalletResponseData(
                  this._userID,
                  aquarium.initialUserBalance(this.unsafeUserCreationIMEventMsg.getRole, this.unsafeUserCreationIMEventMsg.getOccurredMillis),
                  MessageFactory.newWalletEntriesMsg()
                )))

          case false ⇒
            DEBUG("!haveUserCreationIMEvent: %s", event)
            sender ! GetUserWalletResponse(Left("No wallet for user %s [AQU-WAL-00 8]".format(event.userID)), 404)
        }
    }
  }

  def onSetUserActorUserID(userID: String) {
    this._userID = userID
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
