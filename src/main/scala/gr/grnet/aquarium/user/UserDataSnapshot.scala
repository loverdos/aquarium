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


/**
 * Snapshot of data that are user-related.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

sealed trait UserDataSnapshot[T] extends DataSnapshot[T]

case class CreditSnapshot(data: Double, snapshotTime: Long) extends UserDataSnapshot[Double]

case class AgreementSnapshot(data: DSLAgreement, snapshotTime: Long) extends UserDataSnapshot[DSLAgreement]

case class RolesSnapshot(data: List[String], snapshotTime: Long) extends UserDataSnapshot[List[String]]

case class PaymentOrdersSnapshot(data: List[AnyRef], snapshotTime: Long) extends UserDataSnapshot[List[AnyRef]]

case class OwnedGroupsSnapshot(data: List[String], snapshotTime: Long) extends UserDataSnapshot[List[String]]

case class GroupMembershipsSnapshot(data: List[String], snapshotTime: Long) extends UserDataSnapshot[List[String]]

case class OwnedResourcesSnapshot(val data: mutable.Map[DSLResource, Any /*ResourceState*/], snapshotTime: Long) extends UserDataSnapshot[mutable.Map[DSLResource, Any /*ResourceState*/]]

/**
 * Bandwidth is counted in MB (?)
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class BandwidthUpSnapshot(data: Double, snapshotTime: Long) extends UserDataSnapshot[Double]

/**
 * Bandwidth is counted in MB (?)
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class BandwidthDownSnapshot(data: Double, snapshotTime: Long) extends UserDataSnapshot[Double]

/**
 * Time is counted in seconds (?)
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class VMTimeSnapshot(data: Double, snapshotTime: Long) extends UserDataSnapshot[Double]


/**
 * Disk space is counted in MB (?)
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class DiskSpaceSnapshot(data: Double, snapshotTime: Long) extends UserDataSnapshot[Double]