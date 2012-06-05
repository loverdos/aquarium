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
import scala.collection.mutable.ListBuffer
import scala.annotation.tailrec

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class RoleHistory(
                       /**
                        * The head role is the most recent. The same rule applies for the tail.
                        */
                       roles: List[RoleHistoryItem]) {

  def roleNamesByTimeslot: SortedMap[Timeslot, String] = {
    TreeMap(roles.map(role ⇒ (role.timeslot, role.name)): _*)
  }

  def rolesByTimeslot: SortedMap[Timeslot, RoleHistoryItem] = {
    TreeMap(roles.map(role ⇒ (role.timeslot, role)): _*)
  }

  def updatedWithRole(role: String, validFrom: Long) = {
    // TODO: Review this when Timeslot is also reviewed.
    //       Currently, we need `fixValidTo` because Timeslot does not validate when `validFrom` and `validTo`
    //       are equal.
    def fixValidTo(validFrom: Long, validTo: Long): Long = {
      if(validTo == validFrom) {
        // Since validTo is exclusive, make at least 1ms gap
        validFrom + 1
      } else {
        validTo
      }
    }

    val newItems = roles match {
      case Nil ⇒
        RoleHistoryItem(role, validFrom) :: Nil

      case head :: tail ⇒
        if(head.startsStrictlyAfter(validFrom)) {
          // must search history items to find where this fits in
          @tailrec
          def check(allChecked: ListBuffer[RoleHistoryItem],
                    lastCheck: RoleHistoryItem,
                    toCheck: List[RoleHistoryItem]): List[RoleHistoryItem] = {

            toCheck match {
              case Nil ⇒
                allChecked.append(RoleHistoryItem(role, validFrom, fixValidTo(validFrom, lastCheck.validFrom)))
                allChecked.toList

              case toCheckHead :: toCheckTail ⇒
                if(toCheckHead.startsStrictlyAfter(validFrom)) {
                  allChecked.append(toCheckHead)

                  check(allChecked, toCheckHead, toCheckTail)
                } else {
                  allChecked.append(RoleHistoryItem(role, validFrom, fixValidTo(validFrom, lastCheck.validFrom)))
                  allChecked.toList
                }
            }
          }

          val buffer = new ListBuffer[RoleHistoryItem]
          buffer.append(head)
          check(buffer, head, tail)
        } else {
          // assume head.validTo goes to infinity,
          RoleHistoryItem(role, validFrom) :: head.copyWithValidTo(fixValidTo(head.validFrom, validFrom)) :: tail
        }
    }

    RoleHistory(newItems)
  }

  /**
   * Returns the first, chronologically, role.
   */
  def firstRole: Option[RoleHistoryItem] = {
    rolesByTimeslot.valuesIterator.toList.headOption
  }

  /**
   * Returns the name of the first, chronologically, role.
   */
  def firstRoleName: Option[String] = {
    roleNamesByTimeslot.valuesIterator.toList.headOption
  }

  /**
   * Returns the last, chronologically, role.
   */
  def lastRole: Option[RoleHistoryItem] = {
    rolesByTimeslot.valuesIterator.toList.lastOption
  }

  /**
   * Returns the name of the last, chronologically, role.
   */
  def lastRoleName: Option[String] = {
    roleNamesByTimeslot.valuesIterator.toList.lastOption
  }
}

object RoleHistory {
  final val Empty = RoleHistory(Nil)

  def initial(role: String, validFrom: Long): RoleHistory = {
    RoleHistory(RoleHistoryItem(role, validFrom) :: Nil)
  }
}
