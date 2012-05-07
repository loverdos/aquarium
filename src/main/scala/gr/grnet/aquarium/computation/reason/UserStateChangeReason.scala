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

package gr.grnet.aquarium.computation.reason

import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.event.im.IMEventModel

sealed trait UserStateChangeReason {
  /**
      * Return `true` if the result of the calculation should be stored back to the
      * [[gr.grnet.aquarium.store.UserStateStore]].
      *
      */
     def shouldStoreUserState: Boolean

  def shouldStoreCalculatedWalletEntries: Boolean

  def forPreviousBillingMonth: UserStateChangeReason

  def calculateCreditsForImplicitlyTerminated: Boolean

  def code: UserStateChangeReasonCodes.ChangeReasonCode
}

case object InitialUserStateSetup extends UserStateChangeReason {
  def shouldStoreUserState = true

  def shouldStoreCalculatedWalletEntries = false

  def forPreviousBillingMonth = this

  def calculateCreditsForImplicitlyTerminated = false

  def code = UserStateChangeReasonCodes.InitialSetupCode
}
/**
 * A calculation made for no specific reason. Can be for testing, for example.
 *
 */
case object NoSpecificChangeReason extends UserStateChangeReason {
  def shouldStoreUserState = false

  def shouldStoreCalculatedWalletEntries = false

  def forBillingMonthInfo(bmi: BillingMonthInfo) = this

  def forPreviousBillingMonth = this

  def calculateCreditsForImplicitlyTerminated = false

  def code = UserStateChangeReasonCodes.NoSpecificChangeCode
}

/**
 * An authoritative calculation for the billing period.
 *
 * This marks a state for caching.
 *
 * @param billingMonthInfo
 */
case class MonthlyBillingCalculation(billingMonthInfo: BillingMonthInfo) extends UserStateChangeReason {
  def shouldStoreUserState = true

  def shouldStoreCalculatedWalletEntries = true

  def forPreviousBillingMonth = MonthlyBillingCalculation(billingMonthInfo.previousMonth)

  def calculateCreditsForImplicitlyTerminated = true

  def code = UserStateChangeReasonCodes.MonthlyBillingCode
}

/**
 * Used for the realtime billing calculation.
 *
 * @param forWhenMillis The time this calculation is for
 */
case class RealtimeBillingCalculation(forWhenMillis: Long) extends UserStateChangeReason {
  def shouldStoreUserState = false

  def shouldStoreCalculatedWalletEntries = false

  def forPreviousBillingMonth = this

  def calculateCreditsForImplicitlyTerminated = false

  def code = UserStateChangeReasonCodes.RealtimeBillingCode
}

case class IMEventArrival(imEvent: IMEventModel) extends UserStateChangeReason {
  def shouldStoreUserState = true

  def shouldStoreCalculatedWalletEntries = false

  def forPreviousBillingMonth = this

  def calculateCreditsForImplicitlyTerminated = false

  def code = UserStateChangeReasonCodes.IMEventArrivalCode
}
