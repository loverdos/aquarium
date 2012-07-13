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

import collection.immutable.{SortedMap, TreeSet}
import gr.grnet.aquarium.Timespan
import gr.grnet.aquarium.util.Lock
import gr.grnet.aquarium.store.PolicyStore
import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import collection.immutable

/**
 * A mutable container of policy models.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 * @author Prodromos Gerakios <pgerakios@grnet.gr>
 */

class PolicyHistory(policyStore: PolicyStore) extends PolicyStore {
  override type Policy = PolicyModel
  private[this] val lock = new Lock()
  @volatile private[this] var _policies = TreeSet[Policy]()
  private def emptyMap = immutable.SortedMap[Timeslot,Policy]()

  private[this] def synchronized[A](f: => A) : A =
    lock.withLock {
      if(_policies.isEmpty)
        _policies ++= policyStore.loadAndSortPoliciesWithin(0,Long.MaxValue).values
      f
    }

  private[this] def policyAt(s:Long) : PolicyModel =
    new StdPolicy("", None, Timespan(s), Set(), Set(), Map())

  def loadAndSortPoliciesWithin(fromMillis: Long, toMillis: Long): SortedMap[Timeslot, Policy] =
    synchronized {
        val range = Timeslot(fromMillis,toMillis)
        /* ``to'' method: return the subset of all policies.from <= range.to */
        _policies.to(policyAt(range.to.getTime)).foldLeft (emptyMap) { (map,p) =>
          if(p.validityTimespan.toTimeslot.to.getTime >= range.from.getTime)
            map + ((p.validityTimespan.toTimeslot,p))
          else
            map
        }
    }


  /**
   * Return the last (ordered) policy that starts before timeMillis
   *
   * @param atMillis
   * @return
   */
  def loadValidPolicyAt(atMillis: Long): Option[Policy] =
    synchronized {
      // Take the subset of all ordered policies up to the one with less than or equal start time
      // and then return the last item. This should be the policy right before the given time.
      // TODO: optimize the creation of the fake StdPolicy
      _policies.to(policyAt(atMillis)).lastOption
    }


  /**
   * Store an accounting policy.
   */
  def insertPolicy(policy: PolicyModel): Policy =
   synchronized {
      var p = policyStore.insertPolicy(policy)
      _policies += p
      p
   }
}
