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

import gr.grnet.aquarium.message.avro.ModelFactory
import gr.grnet.aquarium.message.avro.gen.PolicyMsg
import gr.grnet.aquarium.util.json.JsonSupport
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.immutable

/**
 * A policy is the fundamental business-related configuration of Aquarium.
 * Policies change over the course of time. The underlying representation must be immutable.
 *
 * TODO: The following sentence will self-destruct at most after 200 commits.
 * This model supersedes the original DSLPolicy, after changes in requirements and some thoughts on simplicity.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class PolicyModel(
    msg: PolicyMsg
) extends Ordered[PolicyModel] with JsonSupport {

  /**
   * Each role is mapped to a full price table.
   */
  val roleMapping = ModelFactory.newRoleMapping(msg.getRoleMapping)

  /**
   * All known charging behaviors for the policy's validity period. These are the fully
   * qualified class names that implement [[gr.grnet.aquarium.charging.ChargingBehavior]]<p/>
   * Note than since a charging behavior is semantically attached to an implementation,
   * a change in the set of known charging behaviors normally means a change in the
   * implementation of Aquarium.
   */
  val chargingBehaviors = immutable.Set(msg.getChargingBehaviors().asScala.toSeq: _*)

  /**
   * All known resource types for the policy's validity period.
   */
  val resourceTypes = immutable.Set(msg.getResourceMapping().asScala.valuesIterator.toSeq.map(ModelFactory.newResourceType).toSeq: _*)

  def validFromMillis = msg.getValidFromMillis: Long

  def validToMillis = msg.getValidToMillis: Long

  final def compare(that: PolicyModel): Int = {
    if(this.validFromMillis < that.validFromMillis) {
      -1
    } else if(this.validFromMillis == that.validFromMillis) {
      0
    } else {
      1
    }
  }

  /**
   * All the known roles for the policy's validity period.
   * These names must be common between all communicating parties, i.e. the IM component that sends
   * [[gr.grnet.aquarium.message.avro.gen.IMEventMsg]] events.
   *
   * This is a derived set, from the keys of `roleMapping`
   */
  val roles = roleMapping.keySet

  val resourceTypesMap: Map[String, ResourceType] = {
    msg.getResourceMapping.asScala.map(kv ⇒ (kv._1, ModelFactory.newResourceType(kv._2))).toMap
  }

  def resourceTypeByName(resource: String) = resourceTypes.find(_.name == resource)
}
