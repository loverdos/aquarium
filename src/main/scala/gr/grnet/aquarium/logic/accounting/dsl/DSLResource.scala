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

package gr.grnet.aquarium.logic.accounting.dsl

/**
 * Represents a chargeable resource.
 *
 * @param name
 * @param unit
 * @param costPolicy
 */
case class DSLResource (
  name: String,
  unit: String,
  costPolicy: DSLCostPolicy,
  isComplex: Boolean = false,
  descriminatorField: String = "instanceId"
) extends DSLItem {

  override def toMap(): Map[String, Any] =
    Map(Vocabulary.name -> name) ++
    Map(Vocabulary.unit -> unit) ++
    Map(Vocabulary.costpolicy -> costPolicy.name) ++
    Map(Vocabulary.complex -> isComplex) ++
    Map(Vocabulary.descriminatorfield -> descriminatorField)
}

object DSLResource {
  final val SimpleResourceInstanceId = "1"
  final val SomeSimpleResourceInstanceId = Some(SimpleResourceInstanceId)
}

object DSLSimpleResource {
  def apply(name: String, unit: String, costPolicy: DSLCostPolicy) = {
    DSLResource(name, unit, costPolicy)
  }
}

object DSLComplexResource {
  def apply(name: String, unit: String, costPolicy: DSLCostPolicy) = {//, descriminatorField: String) = {
    DSLResource(name, unit, costPolicy, true)
  }
}
