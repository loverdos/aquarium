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

import scala.collection.immutable
import gr.grnet.aquarium.policy.{PolicyDefinedFullPriceTableRef, StdUserAgreement, UserAgreementModel}
import gr.grnet.aquarium.util.json.JsonSupport
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

final case class WorkingAgreementHistory(
    var agreements: immutable.SortedSet[UserAgreementModel] = immutable.SortedSet[UserAgreementModel]()
) extends AgreementHistoryModel with JsonSupport {

  def agreementByTimeslot: immutable.SortedMap[Timeslot, UserAgreementModel] = {
    immutable.TreeMap(agreements.map(ag ⇒ (ag.timeslot, ag)).toSeq: _*)
  }

  def setFrom(that: WorkingAgreementHistory): this.type = {
    this.agreements = that.agreements
    this
  }

  def +(userAgreement: UserAgreementModel): this.type = {
    agreements += userAgreement
    this
  }

  def +=(userAgreement: UserAgreementModel): Unit = {
    agreements += userAgreement
  }

  def ++(userAgreements: Traversable[UserAgreementModel]): this.type = {
    agreements ++= userAgreements
    this
  }

  def ++=(userAgreements: Traversable[UserAgreementModel]): Unit = {
    agreements ++= userAgreements
  }

  def oldestAgreement: Option[UserAgreementModel] = {
    agreements.headOption
  }

  def newestAgreement: Option[UserAgreementModel] = {
    agreements.lastOption
  }

  def agreementInEffectWhen(whenMillis: Long): Option[UserAgreementModel] = {
    agreements.to(
      StdUserAgreement("", None, whenMillis, Long.MaxValue, "", PolicyDefinedFullPriceTableRef())
    ).lastOption
  }

  def toAgreementHistory = {
    AgreementHistory(agreements.toList)
  }
}

