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
import org.apache.avro.generic.GenericData
import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.collection.JavaConverters.seqAsJavaListConverter

/**
 * Provides helper methods that construct avro messages.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object MessageFactory {
  def anyValueMsgOfBoolean(x: Boolean) = {
    val av = new AnyValueMsg
    av.setAnyValue(x: java.lang.Boolean)
    av
  }

  def anyValueMsgOfString(x: String) = {
    val av = new AnyValueMsg
    av.setAnyValue(x)
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

  def newResourceTypeMsgs(rts: ResourceTypeMsg*) = {
    rts.asJava
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
      details: DetailsModel.Type = newDetails()
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
      currentValue: Double
  ): ResourceInstanceChargingStateMsg = {

    val msg = new ResourceInstanceChargingStateMsg
    msg.setDetails(details)
    msg.setPreviousEvents(previousEvents)
    msg.setImplicitlyIssuedStartEvents(implicitlyIssuedStartEvents)
    msg.setOldAccumulatingAmount(oldAccumulatingAmount)
    msg.setAccumulatingAmount(accumulatingAmount)
    msg.setPreviousValue(previousValue)
    msg.setCurrentValue(currentValue)
    msg
  }

  def newEmptyUserAgreementHistoryMsg() = {
    val msg = new UserAgreementHistoryMsg
    msg.setAgreements(new ju.ArrayList[UserAgreementMsg]())
    msg
  }

  def newInitialUserAgreementHistoryMsg(initialAgreement: UserAgreementMsg) = {
    val msg = new UserAgreementHistoryMsg
    val list = new GenericData.Array[UserAgreementMsg](1, initialAgreement.getSchema)
    list.add(initialAgreement)
    msg.setAgreements(list)
    msg
  }

  def newUserAgreementFromIMEventMsg(
      imEvent: IMEventMsg,
      id: String = MessageHelpers.UserAgreementMsgIDGenerator.nextUID()
  ) = {

    val msg = new UserAgreementMsg

    msg.setId(id)
    msg.setRelatedIMEventOriginalID(imEvent.getOriginalID)
    msg.setRole(imEvent.getRole)
    msg.setValidFromMillis(imEvent.getOccurredMillis)
    msg.setValidToMillis(Long.MaxValue)
    msg.setFullPriceTableRef(null) // get from current (= @imEvent.getOccurredMillis) policy

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

  def createInitialUserStateMsg(
      usb: UserStateBootstrap,
      occurredMillis: Long
  ): UserStateMsg = {

    val bmi = BillingMonthInfo.fromMillis(occurredMillis)
    val msg = new UserStateMsg

    msg.setUserID(usb.userID)
    msg.setOccurredMillis(occurredMillis)
    msg.setBillingYear(bmi.year)
    msg.setBillingMonth(bmi.month)
    msg.setBillingMonthDay(bmi.day)
    msg.setTotalCredits(CreditsModel.toTypeInMessage(usb.initialCredits))
    msg.setAgreementHistory(newInitialUserAgreementHistoryMsg(usb.initialAgreement.msg))
    msg.setLatestUpdateMillis(occurredMillis)
    msg.setInStoreID(null)
    msg.setOriginalID("") // FIXME get a counter here

    msg
  }

}
