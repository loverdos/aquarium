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

package gr.grnet.aquarium.store

import scala.collection.immutable
import collection.immutable.SortedMap
import gr.grnet.aquarium.logic.accounting.dsl.{DSL, DSLPolicy, Timeslot}
import com.ckkloverdos.maybe.{NoVal, Just, Maybe}
import gr.grnet.aquarium.events.PolicyEntry

/**
 * A store for serialized policy entries.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait PolicyStore {

  /**
   * Load all accounting policies valid after the specified time instance.
   * The results are returned sorted by PolicyEntry.validFrom
   */
  def loadPolicyEntriesAfter(after: Long): List[PolicyEntry]

  def loadAndSortPolicyEntriesWithin(fromMillis: Long, toMillis: Long): SortedMap[Timeslot, PolicyEntry] = {
    val all = loadPolicyEntriesAfter(0L)
    val filtered = all.filter { policyEntry ⇒
      policyEntry.validFrom <= fromMillis &&
      policyEntry.validTo   >= toMillis
    }

    (immutable.SortedMap[Timeslot, PolicyEntry]() /: filtered) { (map, policyEntry) ⇒
      map.updated(policyEntry.fromToTimeslot, policyEntry)
    }
  }
  
  def loadAndSortPoliciesWithin(fromMillis: Long, toMillis: Long, dsl: DSL): SortedMap[Timeslot, DSLPolicy] = {
    for((timeslot, policyEntry) <- loadAndSortPolicyEntriesWithin(fromMillis, toMillis))
      yield (timeslot, dsl.parse(policyEntry.policyYAML))
  }
  
  def loadValidPolicyEntryAt(atMillis: Long): Maybe[PolicyEntry] = Maybe {
    loadPolicyEntriesAfter(0L).find { policyEntry ⇒
      policyEntry.fromToTimeslot.containsTimeInMillis(atMillis)
    } match {
      case Some(policyEntry) ⇒
        policyEntry
      case None ⇒
        null // Do not worry, this will be transformed to a NoVal by the Maybe polymorphic constructor
    }
  }
  
  def loadValidPolicyAt(atMillis: Long, dsl: DSL): Maybe[DSLPolicy] = {
    loadValidPolicyEntryAt(atMillis).map(policyEntry ⇒ dsl.parse(policyEntry.policyYAML))
  }

  /**
   * Store an accounting policy.
   */
  def storePolicyEntry(policy: PolicyEntry): Maybe[RecordID]

  /**
   * Updates the policy record whose id is equal to the id
   * of the provided policy entry.
   */
  def updatePolicyEntry(policy: PolicyEntry): Unit

  /**
   * Find a policy by its unique id
   */
  def findPolicyEntry(id: String): Maybe[PolicyEntry]
}