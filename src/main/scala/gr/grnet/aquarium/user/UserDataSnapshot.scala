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

package gr.grnet.aquarium
package user

import logic.accounting.dsl.{DSLResource, DSLAgreement}
import collection.mutable
import logic.events.WalletEntry


/**
 * Snapshot of data that are user-related.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

sealed trait UserDataSnapshot[T] extends DataSnapshot[T]

case class CreditSnapshot(data: Double, snapshotTime: Long) extends UserDataSnapshot[Double]

case class AgreementSnapshot(data: DSLAgreement, snapshotTime: Long) extends UserDataSnapshot[DSLAgreement]

case class RolesSnapshot(data: List[String], snapshotTime: Long) extends UserDataSnapshot[List[String]]

// TODO: Check if needed
case class PaymentOrdersSnapshot(data: List[AnyRef], snapshotTime: Long) extends UserDataSnapshot[List[AnyRef]]

// TODO: Check if needed
case class OwnedGroupsSnapshot(data: List[String], snapshotTime: Long) extends UserDataSnapshot[List[String]]

// TODO: Check if needed
case class GroupMembershipsSnapshot(data: List[String], snapshotTime: Long) extends UserDataSnapshot[List[String]]

/**
 * Maintains the current state of resources owned by the user. In order to have a
 * uniform representation of the resource state for all resource types
 * (complex or simple) the following convention applies:
 *
 *  * If the resource is complex, then `data.get(AResource)` returns a Map of
 *  `("instance-id" -> current_resource_value)`, as expected.
 *  * If the resource is simple, then `data.get(AResource)` returns
 *   `("1" -> current_resource_value)`. This means that simple resources are
 *   always stored with key 1 as `instance-id`.
 */
case class OwnedResourcesSnapshot(data: Map[DSLResource, Map[String, Float]], snapshotTime: Long)
  extends UserDataSnapshot[Map[DSLResource, Map[String, Float]]]

/**
 * A generic exception thrown when errors occur in dealing with user data snapshots
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class UserDataSnapshotException(msg: String) extends Exception(msg)

/**
 * Holds the user active/suspended status.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class ActiveSuspendedSnapshot(data: Boolean, snapshotTime: Long) extends UserDataSnapshot[Boolean]