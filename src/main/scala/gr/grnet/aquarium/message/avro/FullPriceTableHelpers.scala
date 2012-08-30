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

package gr.grnet.aquarium.message.avro

import gr.grnet.aquarium.message.avro.gen.{_EffectivePriceTable, _FullPriceTable}
import gr.grnet.aquarium.AquariumInternalError
import gr.grnet.aquarium.util.shortNameOfType
import gr.grnet.aquarium.util.LogHelpers.Debug
import org.slf4j.Logger
import scala.annotation.tailrec
import java.{util ⇒ ju}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object FullPriceTableHelpers {
  final val DefaultSelectorKey = "default"

  def effectivePriceTableOfSelectorForResource(
      fullPriceTable: _FullPriceTable,
      selectorPath: List[CharSequence],
      resource: String,
      logger: Logger
  ): _EffectivePriceTable = {

      // Most of the code is for exceptional cases, which we identify in detail.
      @tailrec
      def find(
          partialSelectorPath: List[CharSequence],
          partialSelectorData: Any
      ): _EffectivePriceTable = {

        Debug(logger, "find: ")
        Debug(logger, "  partialSelectorPath = %s", partialSelectorPath.mkString("/"))
        Debug(logger, "  partialSelectorData = %s", partialSelectorData)

        partialSelectorPath match {
          case selector :: Nil ⇒
            // One selector, meaning that the selectorData must be a Map[String, EffectivePriceTable]
            partialSelectorData match {
              case selectorMap: ju.Map[_,_] ⇒
                // The selectorData is a map indeed
                selectorMap.asInstanceOf[ju.Map[String, _]].get(selector) match {
                  case null ⇒
                    // The selectorData is a map but it does nto contain the selector
                    throw new AquariumInternalError(
                      "[AQU-SEL-002] Cannot select path %s for resource %s. Nothing found at partial selector path %s".format(
                        selectorPath.mkString("/"),
                        resource,
                        partialSelectorPath.mkString("/")
                      )
                    )

                  case selected: _EffectivePriceTable ⇒
                    // Yes, it is a map of the right type (OK, we assume keys are always Strings)
                    // (we only check the value type)
                    selected

                  case badSelected ⇒
                    // The selectorData is a map but the value is not of the required type
                    throw new AquariumInternalError(
                      "[AQU-SEL-001] Cannot select path %s for resource %s. Found %s instead of an %s at partial selector path %s".format(
                        selectorPath.mkString("/"),
                        resource,
                        badSelected,
                        shortNameOfType[_EffectivePriceTable],
                        partialSelectorPath.mkString("/")
                      )
                    )
                }


              case badData ⇒
                // The selectorData is not a map. So we have just one final selector but no map to select from.
                throw new AquariumInternalError(
                  "[AQU-SEL-003] Cannot select path %s for resource %s. Found %s instead of a Map at partial selector path %s".format(
                    selectorPath.mkString("/"),
                    resource,
                    badData,
                    partialSelectorPath.mkString("/")
                  )
                )
            }

          case selector :: selectorTail ⇒
            // More than one selector in the path, meaning that the selectorData must be a Map[String, Map[String, _]]
            partialSelectorData match {
              case selectorMap: ju.Map[_,_] ⇒
               // The selectorData is a map indeed
                selectorMap.asInstanceOf[ju.Map[String,_]].get(selector) match {
                  case null ⇒
                    // The selectorData is a map but it does not contain the selector
                    throw new AquariumInternalError(
                      "[AQU-SEL-005] Cannot select path %s for resource %s. Nothing found at partial selector path %s".format(
                        selectorPath.mkString("/"),
                        resource,
                        partialSelectorPath.mkString("/")
                      )
                    )

                  case furtherSelectorMap: ju.Map[_,_] ⇒
                    // The selectorData is a map and we found the respective value for the selector to be a map.
                    find(selectorTail, furtherSelectorMap)

                  case furtherBad ⇒
                    // The selectorData is a map but the respective value is not a map, so that
                    // the selectorTail path cannot be used.
                    throw new AquariumInternalError(
                      "[AQU-SEL-004] Cannot select path %s for resource %s. Found %s instead of a Map at partial selector path %s".format(
                        selectorPath.mkString("/"),
                        resource,
                        furtherBad,
                        partialSelectorPath.mkString("/")
                       )
                    )
                }

              case badData ⇒
                // The selectorData is not a Map. So we have more than one selectors but no map to select from.
                throw new AquariumInternalError(
                  "[AQU-SEL-006] Cannot select path %s for resource %s. Found %s instead of a Map at partial selector path %s".format(
                    selectorPath.mkString("/"),
                    resource,
                    badData,
                    partialSelectorPath.mkString("/")
                  )
                )
            }

          case Nil ⇒
            throw new AquariumInternalError(
              "[AQU-SEL-007] No selector path for resource %s".format(resource)
            )

        }
      }

      Debug(logger, "effectivePriceTableOfSelectorForResource:")
      Debug(logger, "  selectorPath = %s", selectorPath.mkString("/"))

      val selectorData = fullPriceTable.getPerResource().get(resource)
      if(selectorData eq null) {
        throw new AquariumInternalError("Unknown resource type '%s'", resource)
      }

      Debug(logger, "  selectorData = %s", selectorData)
      find(selectorPath, selectorData)
  }

}
