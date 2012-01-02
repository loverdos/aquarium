/*
 * Copyright 2011 GRNET S.A. All rights reserved.
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

package gr.grnet.aquarium.logic.events

import gr.grnet.aquarium.util.TimeHelpers.nowMillis
import gr.grnet.aquarium.user._


/**
 * This is an object representation for a resource name, which provides convenient querying methods.
 *
 * Also, a `ResourceType` knows how to compute a state change from a particular `ResourceEvent`.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
sealed abstract class ResourceType(_name: String) {
  def resourceName = _name

  /**
   * Return true if the resource type must lead to wallet entries generation and, thus, credit diffs.
   *
   * Normally, this should always be the case.
   */
  def isBillableType = true

  /**
   * A resource type is independent if it can, by itself only, create a finalized wallet entry (that is,
   * it can create, by itself only, credits).
   *
   * It is dependent if it needs one or more other events of he same type to
   */
  def isIndependentType = true

  def isKnownType = true
  def isDiskSpace = false
  def isVMTime = false
  def isBandwidthUpload = false
  def isBandwidthDownload = false

  /**
   * Calculates the new `UserState` based on the provided resource event, the calculated wallet entries
   * and the current `UserState`.
   *
   * This method is an implementation detail and is not exposed. The actual user-level API is provided in `ResourceEvent`.
   */
  private[events] final
  def calcStateChange(resourceEvent: ResourceEvent, walletEntries: List[WalletEntry], userState: UserState): UserState = {
    val otherState = calcOtherStateChange(resourceEvent, walletEntries, userState)
    val newCredits = calcNewCreditSnapshot(walletEntries, userState)
    otherState.copy(credits = newCredits)
  }

  private[events]
  def calcOtherStateChange(resourceEvent: ResourceEvent, walletEntries: List[WalletEntry], userState: UserState): UserState
  
  private[events] final
  def calcNewCreditSnapshot(walletEntries: List[WalletEntry], userState: UserState): CreditSnapshot = {
    val newCredits = for {
      walletEntry <- walletEntries if(walletEntry.finalized)
    } yield walletEntry.value.toDouble

    val newCreditSum = newCredits.sum
    val now = System.currentTimeMillis()

    CreditSnapshot(userState.credits.data + newCreditSum, now)
  }
}

/**
 * Companion object used to parse a resource name and provide an object representation in the form
 * of a `ResourceType`.
 *
 * Known resource names, which represent Aquarium resources, are like "bndup", "vmtime" etc. and they are all
 * defined in `ResourceNames`.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object ResourceType {
  def fromName(name: String): ResourceType = {
    name match {
      case ResourceNames.bnddown ⇒ BandwidthDown
      case ResourceNames.bndup   ⇒ BandwidthUp
      case ResourceNames.vmtime  ⇒ VMTime
      case _                     ⇒ UnknownResourceType(name)
    }
  }

  def fromResourceEvent(resourceEvent: ResourceEvent): ResourceType = fromName(resourceEvent.resource)
}

case object BandwidthDown extends ResourceType(ResourceNames.bnddown) {
  override def isBandwidthDownload = true

  private[events]
  def calcOtherStateChange(resourceEvent: ResourceEvent, walletEntries: List[WalletEntry], userState: UserState) = {
    val oldBandwidthDownValue = userState.bandwidthDown.data
    val bandwidthDownDiff = resourceEvent.value

    val newBandwidth = BandwidthDownSnapshot(oldBandwidthDownValue + bandwidthDownDiff, nowMillis)

    userState.copy(bandwidthDown = newBandwidth)
  }
}

case object BandwidthUp extends ResourceType(ResourceNames.bndup) {
  override def isBandwidthUpload = true

  private[events]
  def calcOtherStateChange(resourceEvent: ResourceEvent, walletEntries: List[WalletEntry], userState: UserState) = {
    val oldBandwidthUpValue = userState.bandwidthUp.data
    val bandwidthUpDiff = resourceEvent.value

    val newBandwidth = BandwidthUpSnapshot(oldBandwidthUpValue + bandwidthUpDiff, nowMillis)

    userState.copy(bandwidthUp = newBandwidth)
  }
}

case object VMTime extends ResourceType(ResourceNames.vmtime) {
  override def isVMTime = true

  override def isIndependentType = false

  def isVMTimeOn(eventDetails: ResourceEvent.Details) = eventDetails.get(ResourceEvent.JsonNames.action) match {
    case Some("on") ⇒ true
    case Some("up") ⇒ true
    case _          ⇒ false
  }
  
  def isVMTimeOff(eventDetails: ResourceEvent.Details) = eventDetails.get(ResourceEvent.JsonNames.action) match {
    case Some("off")  ⇒ true
    case Some("down") ⇒ true
    case _            ⇒ false
  }

  private[events] def calcOtherStateChange(resourceEvent: ResourceEvent, walletEntries: List[WalletEntry], userState: UserState) = {
    // FIXME: implement
    userState
  }
}

case object DiskSpace extends ResourceType(ResourceNames.dsksp) {
  override def isDiskSpace = true

  private[events] def calcOtherStateChange(resourceEvent: ResourceEvent, walletEntries: List[WalletEntry], userState: UserState) = {
    val oldDiskSpaceValue = userState.diskSpace.data
    val diskSpaceDiff = resourceEvent.value
    val newDiskSpace = DiskSpaceSnapshot(oldDiskSpaceValue + diskSpaceDiff, nowMillis)
    userState.copy(diskSpace = newDiskSpace)
  }
}

case class UnknownResourceType(originalName: String) extends ResourceType(ResourceNames.unknown) {
  override def isKnownType = false

  private[events] def
  calcOtherStateChange(resourceEvent: ResourceEvent, walletEntries: List[WalletEntry], userState: UserState) = userState
}
