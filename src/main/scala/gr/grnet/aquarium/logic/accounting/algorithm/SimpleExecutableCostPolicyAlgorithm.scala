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

package gr.grnet.aquarium.logic.accounting.algorithm


import com.ckkloverdos.maybe.Maybe
import gr.grnet.aquarium.logic.accounting.dsl._

/**
 * An executable charging algorithm with some simple implementation.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object SimpleExecutableCostPolicyAlgorithm extends ExecutableCostPolicyAlgorithm {

  @inline private[this]
  def hrs(millis: Double) = millis / 1000 / 60 / 60

  def apply(vars: Map[DSLCostPolicyVar, Any]): Maybe[Double] = Maybe {
    vars.apply(DSLCostPolicyNameVar) match {
      case DSLCostPolicyNames.continuous ⇒
        val unitPrice = vars.get(DSLUnitPriceVar).get.asInstanceOf[Double]
        val oldTotalAmount = vars.get(DSLOldTotalAmountVar).get.asInstanceOf[Double]
        val timeDelta = vars.get(DSLTimeDeltaVar).get.asInstanceOf[Double]

        hrs(timeDelta) * oldTotalAmount * unitPrice

      case DSLCostPolicyNames.discrete ⇒
        val unitPrice = vars.get(DSLUnitPriceVar).get.asInstanceOf[Double]
        val currentValue = vars.get(DSLCurrentValueVar).get.asInstanceOf[Double]

        currentValue * unitPrice

      case DSLCostPolicyNames.onoff ⇒
        val unitPrice = vars.get(DSLUnitPriceVar).get.asInstanceOf[Double]
        val timeDelta = vars.get(DSLTimeDeltaVar).get.asInstanceOf[Double]

        hrs(timeDelta) * unitPrice
      case name ⇒
        throw new Exception("Unknown cost policy %s".format(name))
    }
  }
}