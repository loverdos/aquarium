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

import gr.grnet.aquarium.logic.accounting.dsl.Timeslot
import gr.grnet.aquarium.policy.PolicyModel
import scala.collection.immutable.{SortedMap, SortedSet}
import gr.grnet.aquarium.message.avro.ModelFactory

/**
 * Provides helper methods for the policy store.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object PolicyStoreHelpers {
  def loadAllPoliciesToSortedModels(store: PolicyStore): SortedSet[PolicyModel] = {
    var set = SortedSet[PolicyModel]()
    store.foreachPolicy { msg ⇒
      val policyModel = ModelFactory.newPolicyModel(msg)
      set += policyModel
    }

    set
  }

  def loadSortedPolicyModelsWithin(
      store: PolicyStore,
      fromMillis: Long,
      toMillis: Long
  ): SortedMap[Timeslot, PolicyModel] = {
    store.loadSortedPoliciesWithin(fromMillis, toMillis).map { case (timeslot, policy) ⇒
      (timeslot, ModelFactory.newPolicyModel(policy))
    }
  }
}
