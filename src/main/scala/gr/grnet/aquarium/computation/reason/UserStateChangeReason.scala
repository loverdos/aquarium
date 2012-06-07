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
import gr.grnet.aquarium.event.model.im.IMEventModel
import gr.grnet.aquarium.util.shortClassNameOf

/**
 * Provides information explaining the reason Aquarium calculated a new [[gr.grnet.aquarium.computation.UserState]].
 */
case class UserStateChangeReason(
    parentReason: Option[UserStateChangeReason],
    billingMonthInfo: Option[BillingMonthInfo],
    details: Map[String, Any]
) {

  require(
    details.contains(UserStateChangeReason.Names.`type`),
    "No type present in the details of %s".format(shortClassNameOf(this))
  )

  private[this] def booleanFromDetails(name: String, default: Boolean) = {
    details.get(name) match {
      case Some(value: Boolean) ⇒
        value

      case _ ⇒
        false
    }
  }

   /**
    * Return `true` if the result of the calculation should be stored back to the
    * [[gr.grnet.aquarium.store.UserStateStore]].
    *
    */
  def shouldStoreUserState: Boolean =
     booleanFromDetails(UserStateChangeReason.Names.shouldStoreUserState, false)

  def shouldStoreCalculatedWalletEntries: Boolean =
    booleanFromDetails(UserStateChangeReason.Names.shouldStoreCalculatedWalletEntries, false)

  def calculateCreditsForImplicitlyTerminated: Boolean =
    booleanFromDetails(UserStateChangeReason.Names.calculateCreditsForImplicitlyTerminated, false)

  def forBillingMonthInfo(bmi: BillingMonthInfo) = {
    copy(
      parentReason = Some(this),
      billingMonthInfo = Some(bmi)
    )
  }

  def `type`: String = {
    // This must be always present
    details(UserStateChangeReason.Names.`type`).asInstanceOf[String]
  }
}

object UserStateChangeReason {
  object Names {
    final val `type` = "type"

    final val imEvent = "imEvent"
    final val forWhenMillis = "forWhenMillis"

    final val shouldStoreUserState = "shouldStoreUserState"
    final val shouldStoreCalculatedWalletEntries = "shouldStoreCalculatedWalletEntries"
    final val calculateCreditsForImplicitlyTerminated = "calculateCreditsForImplicitlyTerminated"
  }
}

sealed trait UserStateChangeReason_ {
  def originalReason: UserStateChangeReason_
  /**
    * Return `true` if the result of the calculation should be stored back to the
    * [[gr.grnet.aquarium.store.UserStateStore]].
    *
    */
  def shouldStoreUserState: Boolean

  def shouldStoreCalculatedWalletEntries: Boolean

  def forPreviousBillingMonth: UserStateChangeReason_

  def calculateCreditsForImplicitlyTerminated: Boolean

  def code: UserStateChangeReasonCodes.ChangeReasonCode
}

object InitialUserStateSetup {
  def `type` = "InitialUserStateSetup"

  /**
   * When the user state is initially set up.
   */
  def apply(parentReason: Option[UserStateChangeReason]) = {
    UserStateChangeReason(
      parentReason,
      None,
      Map(
        UserStateChangeReason.Names.`type` -> `type`,
        UserStateChangeReason.Names.shouldStoreUserState -> true
      )
    )
  }
}

object InitialUserActorSetup {
  def `type` = "InitialUserActorSetup"

  /**
   * When the user processing unit (actor) is initially set up.
   */
  def apply() = {
    UserStateChangeReason(
      None,
      None,
      Map(
        UserStateChangeReason.Names.`type` -> `type`,
        UserStateChangeReason.Names.shouldStoreUserState -> true
      )
    )
  }
}

object NoSpecificChangeReason {
  def `type` = "NoSpecificChangeReason"

  /**
   * A calculation made for no specific reason. Can be for testing, for example.
   */
  def apply() = {
    UserStateChangeReason(
      None,
      None,
      Map(
        UserStateChangeReason.Names.`type` -> `type`
      )
    )
  }
}

object MonthlyBillingCalculation {
  def `type` = "MonthlyBillingCalculation"

  /**
   * An authoritative calculation for the billing period.
   */
  def apply(parentReason: UserStateChangeReason, billingMongthInfo: BillingMonthInfo) = {
    UserStateChangeReason(
      Some(parentReason),
      Some(billingMongthInfo),
      Map(
        UserStateChangeReason.Names.`type` -> `type`,
        UserStateChangeReason.Names.shouldStoreUserState -> true,
        UserStateChangeReason.Names.shouldStoreCalculatedWalletEntries -> true,
        UserStateChangeReason.Names.calculateCreditsForImplicitlyTerminated -> true
      )
    )
  }
}

object RealtimeBillingCalculation {
  def `type` = "RealtimeBillingCalculation"

  /**
   * Used for the real-time billing calculation.
   */
  def apply(parentReason: Option[UserStateChangeReason], forWhenMillis: Long) = {
    UserStateChangeReason(
      parentReason,
      None,
      Map(
        UserStateChangeReason.Names.`type` -> `type`,
        UserStateChangeReason.Names.forWhenMillis -> forWhenMillis
      )
    )
  }
}

object IMEventArrival {
  def `type` = "IMEventArrival"

  def apply(imEvent: IMEventModel) = {
    UserStateChangeReason(
      None,
      None,
      Map(
        UserStateChangeReason.Names.`type` -> `type`,
        UserStateChangeReason.Names.imEvent -> imEvent,
        UserStateChangeReason.Names.shouldStoreUserState -> true
      )
    )
  }
}
