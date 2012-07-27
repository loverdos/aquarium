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

case class FullPriceTable(
    perResource: Map[String/*Resource*/,
                     Map[String/*Per-ChargingBehavior Key, "default" is the default*/, Any]]
) {

  def effectivePriceTableOfSelectorForResource(
      selectorPath: List[String],
      resource: String
  ): EffectivePriceTable = {

    @tailrec
    def find(
        selectorPath: List[String],
        selectorData: Any
    ): EffectivePriceTable = {

      selectorPath match {
        case Nil ⇒
          // End of selector path. This means that the data must be an EffectivePriceTable
          selectorData match {
            case ept: EffectivePriceTable ⇒
              ept

            case _ ⇒
              // TODO more informative error message (include selector path, resource?)
              throw new AquariumInternalError("Got %s instead of an %s", selectorData, shortNameOfType[EffectivePriceTable])
          }

        case key :: tailSelectorPath ⇒
          // Intermediate path. This means we have another round of Map[String, Any]
          selectorData match {
            case selectorMap: Map[_, _] ⇒
              selectorMap.asInstanceOf[Map[String, _]].get(key) match {
                case None ⇒
                  throw new AquariumInternalError("Did not find value for selector %s", key)

                case Some(nextSelectorData) ⇒
                  find(tailSelectorPath, nextSelectorData)
              }
          }
      }
    }

    val selectorDataOpt = perResource.get(resource)
    if(selectorDataOpt.isEmpty) {
      throw new AquariumInternalError("Unknown resource type '%s'", resource)
    }
    val selectorData = selectorDataOpt.get

    find(selectorPath, selectorData)
  }
}

object FullPriceTable {
  final val DefaultSelectorKey = "default"
}