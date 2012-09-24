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

import gr.grnet.aquarium.charging.state.UserStateBootstrap
import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.event.{CreditsModel, DetailsModel}
import gr.grnet.aquarium.message.avro.gen._
import java.{util ⇒ ju}
import java.util.{ArrayList ⇒ JArrayList}
import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.Predef.Map
import gr.grnet.aquarium.policy.ResourceType
import gr.grnet.aquarium.charging.state.UserStateBootstrap
import gr.grnet.aquarium.util.date.TimeHelpers


/**
 * Provides helper methods that construct avro messages.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object MessageFactory {
  def anyValueMsgOfBoolean(x: Boolean) = {
    val av = new AnyValueMsg
    av.setAnyValue(java.lang.Boolean.valueOf(x))
    av
  }

  def anyValueMsgOfString(x: String) = {
    val av = new AnyValueMsg
    av.setAnyValue(x)
    av
  }

  def anyValueMsgOfList(l: JArrayList[AnyValueMsg]) = {
    val av = new AnyValueMsg
    av.setAnyValue(l)
    av
  }

  def newEffectiveUnitPriceMsg(unitPrice: Double, whenOpt: Option[CronSpecTupleMsg] = None) = {
    EffectiveUnitPriceMsg.newBuilder().
      setUnitPrice(unitPrice).
      setWhen(whenOpt.getOrElse(null)).
    build()
  }

  def newEffectivePriceTableMsg(priceOverrides: EffectiveUnitPriceMsg*) = {
    EffectivePriceTableMsg.newBuilder().
      setPriceOverrides(priceOverrides.asJava).
    build()
  }

  def newSelectorValueMsg(ept: EffectivePriceTableMsg): SelectorValueMsg = {
    SelectorValueMsg.newBuilder().
      setSelectorValue(ept).
    build()
  }

  def newSelectorValueMsg(map: Map[String, SelectorValueMsg]): SelectorValueMsg = {
    SelectorValueMsg.newBuilder().
      setSelectorValue(map.asJava).
    build()
  }

  def newSelectorValueMsg(pairs: (String, SelectorValueMsg)*): SelectorValueMsg = {
    SelectorValueMsg.newBuilder().
      setSelectorValue(Map(pairs:_*).asJava).
    build()
  }

  def newFullPriceTableMsg(perResource: (String, Map[String, SelectorValueMsg])*) = {
    FullPriceTableMsg.newBuilder().
      setPerResource(
        Map((for((k, v) ← perResource) yield (k, v.asJava)):_*).asJava
      ).
    build()
  }

  def newRoleMappingMsg(map: Map[String, FullPriceTableMsg]): java.util.Map[String, FullPriceTableMsg] = {
    map.asJava
  }

  def newRoleMappingMsg(pairs: (String, FullPriceTableMsg)*): java.util.Map[String, FullPriceTableMsg] = {
    Map(pairs:_*).asJava
  }

  def newResourceTypeMsg(name: String, unit: String, chargingBehavior: String) = {
    ResourceTypeMsg.newBuilder().
      setName(name).
      setUnit(unit).
      setChargingBehaviorClass(chargingBehavior).
    build()
  }

  def newResourceTypeMsg(model: ResourceType): ResourceTypeMsg = {
    newResourceTypeMsg(model.name, model.unit, model.chargingBehavior)
  }

  def newResourceTypeMsgs(rts: ResourceTypeMsg*) = {
    rts.asJava
  }

//  def newResourceTypeMsgsMap(rts: ResourceTypeMsg*): java.util.Map[String, ResourceTypeMsg] = {
//    rts.map(rt ⇒ (rt.getName, rt)).toMap.asJava
//  }

  def newResourceTypeMsgsMap(resourceTypes: Map[String, ResourceType]): java.util.Map[String, ResourceTypeMsg] = {
    resourceTypes.map(rtt ⇒ (rtt._1, newResourceTypeMsg(rtt._2))).asJava
  }

  def newChargingBehaviorMsgs(cbs: String*) = {
    cbs.asJava
  }

  def newBooleanDetail(name: String, value: Boolean) = {
    (name, anyValueMsgOfBoolean(value))
  }

  def newStringDetail(name: String, value: String) = {
    (name, anyValueMsgOfString(value))
  }

  def newDetails(details: (String, AnyValueMsg)*): DetailsModel.Type = {
    DetailsModel.fromScalaTuples(details:_*)
  }

  def newResourceEventMsg(
      originalID: String,
      occurredMillis: Long,
      receivedMillis: Long,
      userID: String,
      clientID: String,
      resource: String,
      instanceID: String,
      value: String,
      eventVersion: String,
      details: DetailsModel.Type = newDetails(),
      inStoreID: String = null
  ) = {
    ResourceEventMsg.newBuilder().
      setOriginalID(originalID).
      setOccurredMillis(occurredMillis).
      setReceivedMillis(receivedMillis).
      setUserID(userID).
      setClientID(clientID).
      setResource(resource).
      setInstanceID(instanceID).
      setValue(value).
      setEventVersion(eventVersion).
      setDetails(details).
      setInStoreID(inStoreID).
    build()
  }

  def newIMEventMsg(
      originalID: String,
      occurredMillis: Long,
      receivedMillis: Long,
      userID: String,
      clientID: String,
      isActive: Boolean,
      role: String,
      eventVersion: String,
      eventType: String,
      details: DetailsModel.Type = newDetails(),
      inStoreID: String = null
  ) = {
    IMEventMsg.newBuilder().
      setOriginalID(originalID).
      setInStoreID(null).
      setOccurredMillis(occurredMillis).
      setReceivedMillis(receivedMillis).
      setUserID(userID).
      setClientID(clientID).
      setIsActive(isActive).
      setRole(role).
      setEventVersion(eventVersion).
      setEventType(eventType).
      setDetails(details).
      setInStoreID(inStoreID).
    build()
  }

  def newWalletEntryMsg(
      userID: String,
      sumOfCreditsToSubtract: CreditsModel.Type,
      oldTotalCredits: CreditsModel.Type,
      newTotalCredits: CreditsModel.Type,
      whenComputedMillis: Long,
      referenceStartMillis: Long,
      referenceStopMillis: Long,
      billingYear: Int,
      billingMonth: Int,
      billingMonthDay: Int,
      chargeslots: ju.List[ChargeslotMsg],
      resourceEvents: ju.List[ResourceEventMsg],
      resourceType: ResourceTypeMsg,
      isSynthetic: Boolean
  ): WalletEntryMsg = {
    WalletEntryMsg.newBuilder().
      setUserID(userID).
      setSumOfCreditsToSubtract(CreditsModel.toTypeInMessage(sumOfCreditsToSubtract)).
      setOldTotalCredits(CreditsModel.toTypeInMessage(oldTotalCredits)).
      setNewTotalCredits(CreditsModel.toTypeInMessage(newTotalCredits)).
      setWhenComputedMillis(whenComputedMillis).
      setReferenceStartMillis(referenceStartMillis).
      setReferenceStopMillis(referenceStopMillis).
      setBillingYear(billingYear).
      setBillingMonth(billingMonth).
      setBillingMonthDay(billingMonthDay).
      setChargeslots(chargeslots).
      setResourceEvents(resourceEvents).
      setResourceType(resourceType).
      setIsSynthetic(isSynthetic).
    build()
  }

  def newResourceInstanceChargingStateMsg(
      details: DetailsModel.Type,
      previousEvents: ju.List[ResourceEventMsg],
      implicitlyIssuedStartEvents: ju.List[ResourceEventMsg],
      oldAccumulatingAmount: Double,
      accumulatingAmount: Double,
      previousValue: Double,
      currentValue: Double,
      clientID: String,
      resource: String,
      instanceID: String
  ): ResourceInstanceChargingStateMsg = {

    val msg = new ResourceInstanceChargingStateMsg
    msg.setDetails(details)
    msg.setPreviousEvents(previousEvents)
    msg.setImplicitlyIssuedStartEvents(implicitlyIssuedStartEvents)
    msg.setOldAccumulatingAmount(java.lang.Double.valueOf(oldAccumulatingAmount))
    msg.setAccumulatingAmount(java.lang.Double.valueOf(accumulatingAmount))
    msg.setPreviousValue(java.lang.Double.valueOf(previousValue))
    msg.setCurrentValue(java.lang.Double.valueOf(currentValue))
    msg.setClientID(clientID)
    msg.setResource(resource)
    msg.setInstanceID(instanceID)
    msg
  }

  def newEmptyUserAgreementHistoryMsg() = {
    val msg = new UserAgreementHistoryMsg
    msg.setAgreements(new ju.ArrayList[UserAgreementMsg]())
    msg
  }

  def newInitialUserAgreementHistoryMsg(
      initialAgreement: UserAgreementMsg,
      originalID: String = MessageHelpers.UserAgreementHistoryMsgIDGenerator.nextUID()
  ) = {
    val historyMsg = new UserAgreementHistoryMsg
    historyMsg.setOriginalID(originalID)
    MessageHelpers.insertUserAgreement(historyMsg, initialAgreement)
    historyMsg
  }

  def newUserAgreementFromIMEventMsg(
      imEvent: IMEventMsg,
      agreementOriginalID: String = MessageHelpers.UserAgreementMsgIDGenerator.nextUID()
  ) = {

    val msg = new UserAgreementMsg

    msg.setId(agreementOriginalID)
    msg.setUserID(imEvent.getUserID)
    msg.setRelatedIMEventOriginalID(imEvent.getOriginalID)
    msg.setRole(imEvent.getRole)
    msg.setValidFromMillis(imEvent.getOccurredMillis)
    msg.setValidToMillis(java.lang.Long.valueOf(java.lang.Long.MAX_VALUE))
    msg.setFullPriceTableRef(null) // get from current (= @imEvent.getOccurredMillis) policy
    msg.setOccurredMillis(TimeHelpers.nowMillis())
    msg.setRelatedIMEventMsg(imEvent)

    msg
  }

  def newUserAgreementHistoryMsg(userID: String): UserAgreementHistoryMsg = {
    val msg = new UserAgreementHistoryMsg
    msg.setOriginalID(MessageHelpers.UserAgreementHistoryMsgIDGenerator.nextUID())
    msg.setUserID(userID)
    msg
  }

  def newWalletEntriesMsg(entries: ju.List[WalletEntryMsg] = new ju.ArrayList[WalletEntryMsg]()) = {
    val msg = new WalletEntriesMsg
    msg.setEntries(entries)
    msg
  }

  def newDummyPolicyMsgAt(millis: Long) : PolicyMsg = {
    PolicyMsg.newBuilder().
      setOriginalID("").
      setInStoreID(null).
      setParentID(null).
      setValidFromMillis(millis).
      setValidToMillis(Long.MaxValue).
      setChargingBehaviors(new ju.ArrayList[String]()).
      setResourceTypes(new ju.ArrayList[ResourceTypeMsg]()).
      setRoleMapping(new ju.HashMap[String, FullPriceTableMsg]()).
      build()
  }

  def newInitialUserStateMsg(
      usb: UserStateBootstrap,
      defaultResourceTypesMap: Map[String, ResourceType],
      occurredMillis: Long
  ): UserStateMsg = {

    val bmi = BillingMonthInfo.fromMillis(occurredMillis)
    val msg = new UserStateMsg

    msg.setUserID(usb.userID)
    msg.setOccurredMillis(java.lang.Long.valueOf(occurredMillis))
    msg.setBillingYear(java.lang.Integer.valueOf(bmi.year))
    msg.setBillingMonth(java.lang.Integer.valueOf(bmi.month))
    msg.setBillingMonthDay(java.lang.Integer.valueOf(bmi.day))
    msg.setTotalCredits(java.lang.Double.valueOf(CreditsModel.toTypeInMessage(usb.initialCredits)))
    msg.setAgreementHistory(newInitialUserAgreementHistoryMsg(usb.initialAgreement.msg))
    msg.setLatestUpdateMillis(java.lang.Long.valueOf(occurredMillis))
    msg.setInStoreID(null)
    msg.setOriginalID(MessageHelpers.UserStateMsgIDGenerator.nextUID())
    msg.setResourceTypesMap(newResourceTypeMsgsMap(defaultResourceTypesMap))
    msg.setStateOfResources(new java.util.HashMap())
    msg.setWalletEntries(new java.util.ArrayList[WalletEntryMsg]())
    msg
  }
}
