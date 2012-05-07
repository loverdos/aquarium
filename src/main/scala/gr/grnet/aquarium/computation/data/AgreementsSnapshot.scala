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

package gr.grnet.aquarium.computation.data

import java.util.Date

import gr.grnet.aquarium.logic.accounting.dsl.{DSLAgreement, Timeslot}
import gr.grnet.aquarium.logic.accounting.Policy
import scala.collection.immutable.{SortedMap, TreeMap}

/**
 * User agreement data that will be part of UserState.
 * The provided list of agreements cannot have time gaps. This is checked at object creation type.
 *
 * Note: This is copied from UserDataSnapshot.scala/AgreementSnapshot.
 * TODO: Review
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class AgreementsSnapshot(agreements: List[AgreementSnapshot]) {
  ensureNoGaps(agreements.sortWith((a,b) => if (b.validFrom > a.validFrom) true else false))

  def ensureNoGaps(agreements: List[AgreementSnapshot]): Unit = agreements match {
    case ha :: (t @ (hb :: tail)) =>
      assert(ha.validTo - hb.validFrom == 1);
      ensureNoGaps(t)
    case h :: Nil =>
      assert(h.validTo == Long.MaxValue)
    case Nil => ()
  }

  def agreementsByTimeslot: SortedMap[Timeslot, String] = {
    TreeMap(agreements.map(ag => (ag.timeslot, ag.name)): _*)
  }

  /**
   * Get the user agreement at the specified timestamp
   */
  def findForTime(at: Long): Option[DSLAgreement] = {
    // FIXME: Refactor and do not make this static call to Policy
    agreements.find{ x => x.validFrom < at && x.validTo > at} match {
      case Some(x) => Policy.policy(new Date(at)).findAgreement(x.name)
      case None => None
    }
  }
}
