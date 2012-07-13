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

package gr.grnet.aquarium.store.mongodb

import gr.grnet.aquarium.Timespan
import gr.grnet.aquarium.policy.{PolicyModel, FullPriceTable, ResourceType}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class MongoDBPolicy(
    _id: String,
    parentID: Option[String],
    validityTimespan: Timespan,
    resourceTypes: Set[ResourceType],
    chargingBehaviors: Set[String],
    roleMapping: Map[String/*Role*/, FullPriceTable]
) extends PolicyModel {

  def id = _id
  def idInStore = Some(_id)
}

object MongoDBPolicy {
  final def fromOther(policy: PolicyModel, _id: String): MongoDBPolicy = {
    MongoDBPolicy(
      _id,
      policy.parentID,
      policy.validityTimespan,
      policy.resourceTypes,
      policy.chargingBehaviors,
      policy.roleMapping
    )
  }
}
