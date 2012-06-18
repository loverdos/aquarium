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
import gr.grnet.aquarium.util.json.JsonSupport

/**
 * Provides information explaining the reason Aquarium calculated a new
 * [[gr.grnet.aquarium.computation.state.UserState]].
 */
case class UserStateChangeReason(
    details: Map[String, String],
    billingMonthInfo: Option[BillingMonthInfo],
    parentReason: Option[UserStateChangeReason]
) extends JsonSupport {

  require(
    details.contains(UserStateChangeReason.Names.name),
    "No name present in the details of %s".format(shortClassNameOf(this))
  )

  private[this] def booleanFromDetails(name: String, default: Boolean) = {
    details.get(name) match {
      case Some(value) ⇒
        value.toBoolean

      case _ ⇒
        false
    }
  }

   /**
    * Return `true` if the result of the calculation should be stored back to the
    * [[gr.grnet.aquarium.store.UserStateStore]].
    *
    */
//  def shouldStoreUserState: Boolean =
//     booleanFromDetails(UserStateChangeReason.Names.shouldStoreUserState, false)

//  def shouldStoreCalculatedWalletEntries: Boolean =
//    booleanFromDetails(UserStateChangeReason.Names.shouldStoreCalculatedWalletEntries, false)

  def calculateCreditsForImplicitlyTerminated: Boolean =
    booleanFromDetails(UserStateChangeReason.Names.calculateCreditsForImplicitlyTerminated, false)

  def forBillingMonthInfo(bmi: BillingMonthInfo) = {
    copy(
      parentReason = Some(this),
      billingMonthInfo = Some(bmi)
    )
  }

  def name: String = {
    // This must be always present
    details.get(UserStateChangeReason.Names.name).getOrElse("<unknown>")
  }
}

object UserStateChangeReason {
  object Names {
    final val name = "name"

    final val imEvent = "imEvent"
    final val forWhenMillis = "forWhenMillis"

//    final val shouldStoreUserState = "shouldStoreUserState"
//    final val shouldStoreCalculatedWalletEntries = "shouldStoreCalculatedWalletEntries"
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

/**
 * Used when the very first user state is saved.
 */
object InitialUserStateSetup {
  def name = "InitialUserStateSetup"

  /**
   * When the user state is initially set up.
   */
  def apply(parentReason: Option[UserStateChangeReason]) = {
    UserStateChangeReason(
      Map(
        UserStateChangeReason.Names.name -> name//,
//        UserStateChangeReason.Names.shouldStoreUserState -> true
      ),
      None,
      parentReason
    )
  }
}

object InitialUserActorSetup {
  def name = "InitialUserActorSetup"

  /**
   * When the user processing unit (actor) is initially set up.
   */
  def apply() = {
    UserStateChangeReason(
      Map(
        UserStateChangeReason.Names.name -> name//,
//        UserStateChangeReason.Names.shouldStoreUserState -> true
      ),
      None,
      None
    )
  }
}

object NoSpecificChangeReason {
  def name = "NoSpecificChangeReason"

  /**
   * A calculation made for no specific reason. Can be for testing, for example.
   */
  def apply() = {
    UserStateChangeReason(
      Map(
        UserStateChangeReason.Names.name -> name
      ),
      None,
      None
    )
  }
}

object MonthlyBillingCalculation {
  def name = "MonthlyBillingCalculation"

  /**
   * An authoritative calculation for the billing period.
   */
  def apply(parentReason: UserStateChangeReason, billingMongthInfo: BillingMonthInfo) = {
    UserStateChangeReason(
      Map(
        UserStateChangeReason.Names.name -> name,
//        UserStateChangeReason.Names.shouldStoreUserState -> true,
//        UserStateChangeReason.Names.shouldStoreCalculatedWalletEntries -> true.toString,
        UserStateChangeReason.Names.calculateCreditsForImplicitlyTerminated -> true.toString
      ),
      Some(billingMongthInfo),
      Some(parentReason)
    )
  }
}

object RealtimeBillingCalculation {
  def name = "RealtimeBillingCalculation"

  /**
   * Used for the real-time billing calculation.
   */
  def apply(parentReason: Option[UserStateChangeReason], forWhenMillis: Long) = {
    UserStateChangeReason(
      Map(
        UserStateChangeReason.Names.name -> name,
        UserStateChangeReason.Names.forWhenMillis -> forWhenMillis.toString
      ),
      None,
      parentReason
    )
  }
}

object IMEventArrival {
  def name = "IMEventArrival"

  def apply(imEvent: IMEventModel) = {
    UserStateChangeReason(
      Map(
        UserStateChangeReason.Names.name -> name,
        UserStateChangeReason.Names.imEvent -> imEvent.toJsonString//,
//        UserStateChangeReason.Names.shouldStoreUserState -> true
      ),
      None,
      None
    )
  }
}
