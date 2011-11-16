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

package gr.grnet.aquarium.logic.accounting

import collection.immutable.HashMap
import collection.immutable.List
import java.util.Date

abstract class Agreement {

  val id : Long = 0

  val pricelist : Map[AccountingEventType.Value, Float] = Map()

  var policies : Map[AccountingEventType.Value, Map[Date,Policy]] = Map()

  def addPolicy(et: AccountingEventType.Value, p: Policy, d: Date) = {
    policies = policies ++ Map(et -> Map(d -> p))
  }

  /** Get applicable policy at the resource that the event occured */
  def policy(et: AccountingEventType.Value, d: Date) : Option[Policy] = {
    val ruleset = policies.getOrElse(et, new HashMap[Date, Policy])
    val keyset = List(new Date(0)) ++
                 ruleset.keys.toList.sorted ++
                 List(new Date(Long.MaxValue))

    val key = intervals[Date](keyset).find(
      k => d.compareTo(k.head) >= 0 && d.compareTo(k.reverse.head) < 0
    ).get.head

    ruleset.get(key)
  }

  /**Generate a list of pairs by combining the elements of the provided
   * list sequentially.
   *
   * for example:
   * intervals(List(1,2,3,4)) -> List(List(1,2), List(2,3), List(3,4))
   */
  private def intervals[A](a : List[A]) : List[List[A]] = {
    if (a.size <= 1)
      return List()
    List(List(a.head, a.tail.head)) ++ intervals(a.tail)
  }
}