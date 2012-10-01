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

import gr.grnet.aquarium.{Real, AquariumInternalError}
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
import gr.grnet.aquarium.charging.state.UserStateModel
import gr.grnet.aquarium.message.avro.gen.{UserAgreementHistoryMsg, IMEventMsg, ResourceEventMsg}
import gr.grnet.aquarium.message.avro.{ModelFactory, MessageFactory, MessageHelpers}
import gr.grnet.aquarium.service.event.BalanceEvent
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.util.{LogHelpers, shortClassNameOf}
import gr.grnet.aquarium.policy.{ResourceType, PolicyModel}
import gr.grnet.aquarium.charging.bill.BillEntryMsg

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class UserActor extends ReflectiveRoleableActor {
  private[this] var _imMsgCount = 0
  private[this] var _userStateModel: UserStateModel = _

  def userID = {
    if(!haveUserState) {
      throw new AquariumInternalError("%s not initialized")
    }

    this._userStateModel.userID
  }

  override def postStop() {
    DEBUG("I am finally stopped (in postStop())")
    aquarium.akkaService.notifyUserActorPostStop(this)
  }

  private[this] def shutmedown(): Unit = {
    if(haveUserState) {
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

  def onAquariumPropertiesLoaded(event: AquariumPropertiesLoaded): Unit = {
  }

  private[this] def unsafeUserCreationIMEventMsg = {
    this._userStateModel.unsafeUserCreationIMEvent
  }

  private[this] def haveAgreements = {
    (this._userStateModel ne null)
  }

  private[this] def haveUserCreationEvent = {
    haveAgreements &&
    this._userStateModel.hasUserCreationEvent
  }

  private[this] def haveUserState = {
    (this._userStateModel ne null)
  }

  private[this] def isInitial = this._userStateModel.isInitial

  /**
   * Creates the agreement history from all the stored IMEvents.
   *
   * @return (`true` iff there was a user CREATE event, the number of events processed)
   */
  private[this] def createUserAgreementHistoryFromIMEvents(userID: String): (Boolean, Int) = {
    DEBUG("createUserAgreementHistoryFromStoredIMEvents()")
    assert(haveUserState, "haveUserState")


    var _imcounter = 0

    val hadCreateEvent = aquarium.imEventStore.foreachIMEventInOccurrenceOrder(userID) { imEvent ⇒
      _imcounter += 1
      DEBUG("Replaying [%s/%s] %s", shortClassNameOf(imEvent), _imcounter, imEvent)

      if(_imcounter == 1 && !MessageHelpers.isUserCreationIMEvent(imEvent)) {
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

        this._userStateModel.insertUserAgreementMsgFromIMEvent(imEvent)
        true
      }
    }

    this._imMsgCount = _imcounter

    DEBUG("Agreements: %s", this._userStateModel.userAgreementHistoryMsg)
    (hadCreateEvent, _imcounter)
  }

  private[this] def saveFirstUserState(userID: String) {
    this._userStateModel.userStateMsg.setIsFirst(true)
    this._userStateModel.updateUserStateMsg(
      aquarium.userStateStore.insertUserState(this._userStateModel.userStateMsg)
    )
  }

  private[this] def saveSubsequentUserState() {
    this._userStateModel.userStateMsg.setIsFirst(false)
    this._userStateModel.updateUserStateMsg(
      aquarium.userStateStore.insertUserState(this._userStateModel.userStateMsg)
    )
  }

  private[this] def loadLastKnownUserStateAndUpdateAgreements() {
    val userID = this._userStateModel.userID
    aquarium.userStateStore.findLatestUserState(userID) match {
      case None ⇒
        // First user state ever
        saveFirstUserState(userID)

      case Some(latestUserState) ⇒
        this._userStateModel.updateUserStateMsg(latestUserState)
    }
  }

  private[this] def processResourceEventsAfterLastKnownUserState() {
    // Update the user state snapshot with fresh (ie not previously processed) events.

  }

  private[this] def makeUserStateMsgUpToDate() {
    loadLastKnownUserStateAndUpdateAgreements()
    processResourceEventsAfterLastKnownUserState()
  }

  private[this] def checkInitial(nextThing: () ⇒ Any = () ⇒ {}): Boolean = {
    if(!isInitial) {
      return false
    }

    val (userCreated, imEventsCount) = createUserAgreementHistoryFromIMEvents(userID)

    if(userCreated) {
      makeUserStateMsgUpToDate()
    }

    nextThing()

    true
  }

  /**
   * Processes [[gr.grnet.aquarium.message.avro.gen.IMEventMsg]]s that come directly from the
   * messaging hub (rabbitmq).
   */
  def onIMEventMsg(imEvent: IMEventMsg) {
    if(checkInitial()) {
      return
    }

    // Check for out of sync (regarding IMEvents)
    val isOutOfSyncIM = imEvent.getOccurredMillis < this._userStateModel.latestIMEventOccurredMillis
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

    // Make new agreement
    this._userStateModel.insertUserAgreementMsgFromIMEvent(imEvent)
    this._imMsgCount += 1

    if(MessageHelpers.isUserCreationIMEvent(imEvent)) {
      makeUserStateMsgUpToDate()
    }

    DEBUG("Agreements: %s", this._userStateModel.userAgreementHistoryMsg)
  }

  def onResourceEventMsg(rcEvent: ResourceEventMsg) {
    if(checkInitial()) {
      return
    }

    if(!haveUserCreationEvent) {
      DEBUG("No agreements. Ignoring %s", rcEvent)

      return
    }

    assert(haveUserState, "haveUserState")

    val oldTotalCredits = this._userStateModel.totalCreditsAsReal

    chargingService.processResourceEvent(
      rcEvent.getReceivedMillis,
      rcEvent,
      this._userStateModel,
      aquarium.currentResourceMapping,
      true
    )

    val newTotalCredits = this._userStateModel.totalCreditsAsReal

    if(oldTotalCredits.signum * newTotalCredits.signum < 0) {
      aquarium.eventBus ! new BalanceEvent(userID, newTotalCredits >= 0)
    }

    DEBUG("Updated %s", this._userStateModel)
  }

  def onGetUserBillRequest(event: GetUserBillRequest): Unit = {
    checkInitial()

    try{
      val timeslot = event.timeslot
      val resourceTypes: Map[String, ResourceType] = aquarium.policyStore.
                          loadSortedPolicyModelsWithin(timeslot.from.getTime,
                                                       timeslot.to.getTime).
                          values.headOption match {
          case None => Map[String,ResourceType]()
          case Some(policy:PolicyModel) => policy.resourceTypesMap
      }
      val state= if(haveUserState) Some(this._userStateModel.userStateMsg) else None
      val billEntryMsg = BillEntryMsg.fromWorkingUserState(timeslot,this.userID,state,resourceTypes)
      //val billEntryMsg = MessageFactory.createBillEntryMsg(billEntry)
      //logger.debug("BILL ENTRY MSG: " + billEntryMsg.toString)
      val billData = GetUserBillResponseData(this.userID,billEntryMsg)
      sender ! GetUserBillResponse(Right(billData))
    } catch {
      case e:Exception =>
        e.printStackTrace()
        sender ! GetUserBillResponse(Left("Internal Server Error [AQU-BILL-0001]"), 500)
    }
  }

  def onGetUserBalanceRequest(event: GetUserBalanceRequest): Unit = {
    checkInitial()

    val userID = event.userID

    (haveAgreements, haveUserState) match {
      case (true, true) ⇒
        // (User CREATEd, with balance state)
        val realtimeMillis = TimeHelpers.nowMillis()
        chargingService.calculateRealtimeUserState(
          this._userStateModel,
          aquarium.currentResourceMapping,
          realtimeMillis
        )

        sender ! GetUserBalanceResponse(Right(GetUserBalanceResponseData(this.userID, this._userStateModel.totalCredits)))

      case (true, false) ⇒
        // (User CREATEd, no balance state)
        // Return the default initial balance
        sender ! GetUserBalanceResponse(
          Right(
            GetUserBalanceResponseData(
              this.userID,
              aquarium.initialUserBalance(this.unsafeUserCreationIMEventMsg.getRole, this.unsafeUserCreationIMEventMsg.getOccurredMillis).toString()
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
    checkInitial()

    haveUserState match {
      case true ⇒
        val realtimeMillis = TimeHelpers.nowMillis()
        chargingService.calculateRealtimeUserState(
          this._userStateModel,
          aquarium.currentResourceMapping,
          realtimeMillis
        )

        sender ! GetUserStateResponse(Right(this._userStateModel.userStateMsg))

      case false ⇒
        sender ! GetUserStateResponse(Left("No state for user %s [AQU-STA-0006]".format(event.userID)), 404)
    }
  }

  def onGetUserWalletRequest(event: GetUserWalletRequest): Unit = {
    checkInitial()

    haveUserState match {
      case true ⇒
        DEBUG("haveWorkingUserState: %s", event)
        val realtimeMillis = TimeHelpers.nowMillis()
        chargingService.calculateRealtimeUserState(
          this._userStateModel,
          aquarium.currentResourceMapping,
          realtimeMillis
        )

        sender ! GetUserWalletResponse(
          Right(
            GetUserWalletResponseData(
              this.userID,
              this._userStateModel.totalCredits,
              MessageFactory.newWalletEntriesMsg(this._userStateModel.userStateMsg.getWalletEntries)
            )))

      case false ⇒
        DEBUG("!haveWorkingUserState: %s", event)
        haveAgreements match {
          case true ⇒
            DEBUG("haveAgreements: %s", event)
            sender ! GetUserWalletResponse(
              Right(
                GetUserWalletResponseData(
                  this.userID,
                  Real.toMsgField(aquarium.initialUserBalance(this.unsafeUserCreationIMEventMsg.getRole, this.unsafeUserCreationIMEventMsg.getOccurredMillis)),
                  MessageFactory.newWalletEntriesMsg()
                )))

          case false ⇒
            DEBUG("!haveUserCreationIMEvent: %s", event)
            sender ! GetUserWalletResponse(Left("No wallet for user %s [AQU-WAL-00 8]".format(event.userID)), 404)
        }
    }
  }

  /**
   * Initializes the actor's internal state.
   *
   * @param userID
   */
  def onSetUserActorUserID(userID: String) {
    // Create the full agreement history from the original sources (IMEvents)
    this._userStateModel = ModelFactory.newInitialUserStateModel(
      userID,
      Real(0),
      TimeHelpers.nowMillis()
    )

    require(this._userStateModel.isInitial, "this._userStateModel.isInitial")
  }

  private[this] def D_userID = {
    if(haveUserState) userID else "???"
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
