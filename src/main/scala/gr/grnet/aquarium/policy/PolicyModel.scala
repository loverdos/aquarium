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

import gr.grnet.aquarium.{AquariumInternalError, Timespan}
import gr.grnet.aquarium.util.json.JsonSupport
import gr.grnet.aquarium.charging.ChargingBehavior

/**
 * A policy is the fundamental business-related configuration of Aquarium.
 * Policies change over the course of time. The underlying representation must be immutable.
 *
 * TODO: The following sentence will self-destruct at most after 200 commits.
 * This model supersedes the original DSLPolicy, after changes in requirements and some thoughts on simplicity.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

trait PolicyModel extends JsonSupport {
  def id: String

  def parentID: Option[String]

  def idInStore: Option[Any]


  /**
   * The time period within which this policy is valid.
   */
  def validityTimespan: Timespan

  final def validFrom: Long = validityTimespan.fromMillis

  final def validTo: Long = validityTimespan.toMillis

  /**
   * All known resource types for the policy's validity period.
   */
  def resourceTypes: Set[ResourceType]

  /**
   * All known charging behaviors for the policy's validity period.<p/>
   *
   * Note than since a charging behavior is semantically attached to an implementation, a change in the set
   * of known charging behaviors normally means a change in the implementation of Aquarium.
   */
  def chargingBehaviorClasses: Set[String/*ImplementationClassName*/]

  /**
   * Each role is mapped to a full price table.
   */
  def roleMapping: Map[String/*Role*/, FullPriceTable]


  /**
   * All the known roles for the policy's validity period.
   * These names must be common between all communicating parties, i.e. the IM component that sends
   * [[gr.grnet.aquarium.event.model.im.IMEventModel]] events.
   *
   * This is a derived set, from the keys of `roleMapping`
   */
  def roles: Set[String] = roleMapping.keySet

  def resourceTypesMap: Map[String, ResourceType] = Map(resourceTypes.map(rt ⇒ (rt.name, rt)).toSeq: _*)
}

object PolicyModel {
  trait NamesT {
    final val a = 1
  }

  final object Names extends NamesT
}
