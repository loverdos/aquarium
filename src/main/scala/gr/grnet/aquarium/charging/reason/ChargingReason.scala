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

package gr.grnet.aquarium.charging.reason

import gr.grnet.aquarium.computation.BillingMonthInfo
import gr.grnet.aquarium.event.model.im.IMEventModel
import gr.grnet.aquarium.util.shortClassNameOf

/**
 * Provides information explaining the reason Aquarium calculated a new
 * [[gr.grnet.aquarium.computation.state.UserState]].
 */
case class ChargingReason(
    details: Map[String, Any],
    billingMonthInfo: Option[BillingMonthInfo],
    parentReason: Option[ChargingReason]
) {

  require(
    details.contains(ChargingReason.Names.name),
    "No name present in the details of %s".format(shortClassNameOf(this))
  )

  private[this] def booleanFromDetails(name: String, default: Boolean) = {
    details.get(name) match {
      case Some(value) ⇒
        value.toString.toBoolean

      case _ ⇒
        false
    }
  }

  def calculateCreditsForImplicitlyTerminated: Boolean =
    booleanFromDetails(ChargingReason.Names.calculateCreditsForImplicitlyTerminated, false)

  def forBillingMonthInfo(bmi: BillingMonthInfo) = {
    copy(
      parentReason = Some(this),
      billingMonthInfo = Some(bmi)
    )
  }

  def name: String = {
    // This must be always present
    details.get(ChargingReason.Names.name).map(_.toString).getOrElse("<unknown>")
  }
}

object ChargingReason {

  object Names {
    final val name = "name"

    final val imEvent = "imEvent"
    final val forWhenMillis = "forWhenMillis"

    final val calculateCreditsForImplicitlyTerminated = "calculateCreditsForImplicitlyTerminated"
  }

}

/**
 * Used when the very first user state is saved.
 */
object InitialUserStateSetup {
  def name = "InitialUserStateSetup"

  /**
   * When the user state is initially set up.
   */
  def apply(parentReason: Option[ChargingReason]) = {
    ChargingReason(
      Map(
        ChargingReason.Names.name -> name
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
    ChargingReason(
      Map(
        ChargingReason.Names.name -> name
      ),
      None,
      None
    )
  }
}

object NoSpecificChargingReason {
  def name = "NoSpecificChargingReason"

  /**
   * A calculation made for no specific reason. Can be for testing, for example.
   */
  def apply() = {
    ChargingReason(
      Map(
        ChargingReason.Names.name -> name
      ),
      None,
      None
    )
  }
}

object MonthlyBillChargingReason {
  def name = "MonthlyBillChargingReason"

  /**
   * An authoritative calculation for the billing period.
   */
  def apply(parentReason: ChargingReason, billingMongthInfo: BillingMonthInfo) = {
    ChargingReason(
      Map(
        ChargingReason.Names.name -> name,
        ChargingReason.Names.calculateCreditsForImplicitlyTerminated -> true.toString
      ),
      Some(billingMongthInfo),
      Some(parentReason)
    )
  }
}

object RealtimeChargingReason {
  def name = "RealtimeChargingReason"

  /**
   * Used for the real-time billing calculation.
   */
  def apply(parentReason: Option[ChargingReason], forWhenMillis: Long) = {
    ChargingReason(
      Map(
        ChargingReason.Names.name -> name,
        ChargingReason.Names.forWhenMillis -> forWhenMillis.toString
      ),
      None,
      parentReason
    )
  }
}

object IMEventArrival {
  def name = "IMEventArrival"

  def apply(imEvent: IMEventModel) = {
    ChargingReason(
      Map(
        ChargingReason.Names.name -> name,
        ChargingReason.Names.imEvent -> imEvent.toJsonString
      ),
      None,
      None
    )
  }
}
