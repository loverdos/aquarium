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

import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.policy.UserAgreementModel
import gr.grnet.aquarium.util.json.JsonSupport
import scala.collection.immutable
import gr.grnet.aquarium.message.avro.gen.{UserAgreementMsg, IMEventMsg, UserAgreementHistoryMsg}
import gr.grnet.aquarium.message.avro.{MessageFactory, MessageHelpers, ModelFactory}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

final class UserAgreementHistoryModel(msg: UserAgreementHistoryMsg) {

  private[this] var _latestIMEventOccurredMillis = 0L

  private[this] var (_userAgreementModels, _userCreationIMEventMsgOpt): (immutable.SortedSet[UserAgreementModel], Option[IMEventMsg]) = {
    var userAgreementModels = immutable.SortedSet[UserAgreementModel]()
    var userCreationIMEventMsg: Option[IMEventMsg] = None
    val userAgreements = msg.getAgreements.iterator()
    while(userAgreements.hasNext) {
      val userAgreement = userAgreements.next()
      val userAgreementModel = ModelFactory.newUserAgreementModel(userAgreement)
      userAgreementModels += userAgreementModel

      userAgreement.getRelatedIMEventMsg match {
        case null ⇒

        case msg ⇒
          if(MessageHelpers.isIMEventCreate(msg)) {
            userCreationIMEventMsg = Some(msg)
          }
      }

      checkLatestIMEventOccurredMillis(userAgreement.getRelatedIMEventMsg)
    }

    (userAgreementModels, userCreationIMEventMsg)
  }

  private[this] def checkLatestIMEventOccurredMillis(imEvent: IMEventMsg) {
    if(imEvent ne null) {
      if(this._latestIMEventOccurredMillis < imEvent.getOccurredMillis) {
        this._latestIMEventOccurredMillis = imEvent.getOccurredMillis
      }
    }
  }

  def latestIMEventOccurredMillis = this._latestIMEventOccurredMillis

  def hasUserCreationEvent = this._userCreationIMEventMsgOpt.isDefined

  def userCreationIMEvent = this._userCreationIMEventMsgOpt

  def size: Int = msg.getAgreements.size()

  def agreementByTimeslot: immutable.SortedMap[Timeslot, UserAgreementModel] = {
    immutable.TreeMap(_userAgreementModels.map(ag ⇒ (ag.timeslot, ag)).toSeq: _*)
  }

  def insertUserAgreementModel(userAgreement: UserAgreementModel) {
    MessageHelpers.insertUserAgreement(this.msg, userAgreement.msg)
    this._userAgreementModels += userAgreement
    checkLatestIMEventOccurredMillis(userAgreement.msg.getRelatedIMEventMsg)
  }

  def insertUserAgreementMsg(userAgreementMsg: UserAgreementMsg) {
    insertUserAgreementModel(ModelFactory.newUserAgreementModel(userAgreementMsg))
  }

  def insertUserAgreementMsgFromIMEvent(imEvent: IMEventMsg) {
    val userAgreementMsg = MessageFactory.newUserAgreementFromIMEventMsg(imEvent)
    insertUserAgreementMsg(userAgreementMsg)
  }

  def oldestAgreementModel: Option[UserAgreementModel] = {
    _userAgreementModels.headOption
  }

  def newestAgreementModel: Option[UserAgreementModel] = {
    _userAgreementModels.lastOption
  }

  override def toString = msg.toString

//  def agreementInEffectWhen(whenMillis: Long): Option[UserAgreementModel] = {
//    agreements.to(
//      UserAgreementModel("", None, whenMillis, Long.MaxValue, "", PolicyDefinedFullPriceTableRef())
//    ).lastOption
//  }
}

