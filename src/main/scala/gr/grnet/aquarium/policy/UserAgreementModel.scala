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

package gr.grnet.aquarium.policy

import gr.grnet.aquarium.{AquariumInternalError, Timespan}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

trait UserAgreementModel extends Ordered[UserAgreementModel] {
  /**
   * By convention, the id is the id of the IMEvent that made the agreement change.
   * @return
   */
  def id: String

  def relatedIMEventID: Option[String]

  def validityTimespan = Timespan(validFromMillis, validToMillis)

  def role: String

  def fullPriceTableRef: FullPriceTableRef

  def timeslot = validityTimespan.toTimeslot

  def validFromMillis: Long

  def validToMillis: Long

  def compare(that: UserAgreementModel): Int = {
    if(this.validFromMillis < that.validFromMillis) {
      -1
    }
    else if(this.validFromMillis == that.validFromMillis) {
      0
    }
    else {
      1
    }
  }

  def effectivePriceTableOfResourceTypeForSelector(
      resource: String,
      selectorPath: List[String],
      policy: PolicyModel
  ): EffectivePriceTable = {

    val fullPriceTable = this.fullPriceTableRef match {
      case PolicyDefinedFullPriceTableRef ⇒
        policy.roleMapping.get(role) match {
          case Some(fullPriceTable) ⇒
            fullPriceTable

          case None ⇒
            throw new AquariumInternalError("Unknown role '%s' in policy".format(role))
        }

      case AdHocFullPriceTableRef(fullPriceTable) ⇒
        fullPriceTable
    }

    fullPriceTable.effectivePriceTableOfSelector(selectorPath, resource)
  }
}
