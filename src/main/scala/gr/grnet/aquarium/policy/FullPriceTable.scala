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

import gr.grnet.aquarium.AquariumInternalError
import scala.annotation.tailrec
import gr.grnet.aquarium.util.shortNameOfType

/**
 * A full price table provides detailed pricing information for all resources.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class FullPriceTable(perResource: Map[String/*Resource*/, SelectorMapOrEffectivePriceTable]) {
  def effectivePriceTableOfSelector(selectorPath: List[String], resource: String): EffectivePriceTable = {
    @tailrec
    def find(
        selectorPath: List[String],
        soeOfResource: SelectorMapOrEffectivePriceTable
    ): EffectivePriceTable = {

      selectorPath match {
        case Nil ⇒
          if(soeOfResource.isSelectorMap) {
            throw new AquariumInternalError("Expecting an %s but got a selector", shortNameOfType[EffectivePriceTable])
          }
          soeOfResource.effectivePriceTable

        case key :: tailSelectorPath ⇒
          if(!soeOfResource.isSelectorMap) {
            throw new AquariumInternalError("Expecting a selector but got an %s", shortNameOfType[EffectivePriceTable])
          }

          val selctorMap = soeOfResource.selectorMap
          val selectedOpt = selctorMap.get(key)
          if(selectedOpt.isEmpty) {
            throw new AquariumInternalError("Could not find selector '%s'", key)
          }

         find(tailSelectorPath, selectedOpt.get)
      }
    }

    val soeOfResourceOpt = perResource.get(resource)
    if(soeOfResourceOpt.isEmpty) {
      throw new AquariumInternalError("Unknown resource type '%s'", resource)
    }

    find(selectorPath, soeOfResourceOpt.get)
  }
}

sealed trait SelectorMapOrEffectivePriceTable {
  def isSelectorMap: Boolean

  def selectorMap: Map[String, SelectorMapOrEffectivePriceTable]

  def effectivePriceTable: EffectivePriceTable
}

case class IsSelectorMap(selectorMap: Map[String, SelectorMapOrEffectivePriceTable]) extends SelectorMapOrEffectivePriceTable {
  def isSelectorMap = true

  def effectivePriceTable =
    throw new AquariumInternalError("Accessed effectivePriceTable of %s", shortNameOfType[IsSelectorMap])
}

case class IsEffectivePriceTable(effectivePriceTable: EffectivePriceTable) extends SelectorMapOrEffectivePriceTable {
  def isSelectorMap = false

  def selectorMap =
    throw new AquariumInternalError("Accessed selector of %s", shortNameOfType[IsEffectivePriceTable])
}