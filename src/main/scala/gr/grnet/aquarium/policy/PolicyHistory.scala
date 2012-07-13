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

import scala.collection.immutable.TreeSet
import gr.grnet.aquarium.Timespan
import gr.grnet.aquarium.util.Lock

/**
 * A mutable container of policy models.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class PolicyHistory {
  private[this] val lock = new Lock()
  @volatile private[this] var _policies = TreeSet[PolicyModel]()

  def insertNewPolicy(newPolicy: PolicyModel): Unit = {
    lock.withLock(_policies += newPolicy)
  }

  /**
   * Return the last (ordered) policy that starts before timeMillis
   *
   * @param timeMillis
   * @return
   */
  def findPolicyAt(timeMillis: Long): Option[PolicyModel] = {
    lock.withLock {
      // Take the subset of all ordered policies up to the one with less than or equal start time
      // and then return the last item. This should be the policy right before the given time.
      // TODO: optimize the creation of the fake StdPolicy
      _policies.to(new StdPolicy("", None, Timespan(timeMillis), Set(), Set(), Map())).lastOption
    }
  }
}
