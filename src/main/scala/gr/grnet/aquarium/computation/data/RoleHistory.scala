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

import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import scala.collection.immutable.{TreeMap, SortedMap}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class RoleHistory(
                        /**
                         * The head role is the most recent. The same rule applies for the tail.
                         */
                       roles: List[RoleHistoryItem]) {
  def rolesByTimeslot: SortedMap[Timeslot, String] = {
    TreeMap(roles.map(role => (role.timeslot, role.name)): _*)
  }

  /**
   * Insert the new role in front of the other ones and adjust the `validTo` limit of the previous role.
   */
  def addMostRecentRole(role: String, validFrom: Long) = {
    val newItem = RoleHistoryItem(role, validFrom)
    val newRoles = roles match {
      case head :: tail ⇒
        newItem :: head.withNewValidTo(validFrom) :: tail

      case Nil ⇒
        newItem :: Nil
    }

    RoleHistory(newRoles)
  }
}

object RoleHistory {
  final val Empty = RoleHistory(Nil)

  def initial(role: String, validFrom: Long): RoleHistory = {
    RoleHistory(RoleHistoryItem(role, validFrom) :: Nil)
  }
}
