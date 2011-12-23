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

package gr.grnet.aquarium.util.json

import net.liftweb.json.ext.JodaTimeSerializers
import gr.grnet.aquarium.logic.events.AquariumEvent
import net.liftweb.json.JsonAST.JValue
import net.liftweb.json._

/**
 * Provides conversion methods from and to JSON.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

object JsonHelpers {
  /**
   * The application-wide JSON formats used from the underlying lift-json library.
   */
  implicit val DefaultJsonFormats = (DefaultFormats ++ JodaTimeSerializers.all) +
    FieldSerializer[AquariumEvent]()

  final def anyToJValue(any: Any): JValue = {
    any match {
      case jValue: JValue ⇒
        jValue
      case json: String ⇒
        parse(json)
      case _ ⇒
        Extraction.decompose(any)
    }
  }
  
  final def anyToJson(any: Any): String = {
    Printer.pretty(JsonAST.render(anyToJValue(any)))
  }
  
  final def jsonBytesToObject[A: Manifest](bytes: Array[Byte], encoding: String = "UTF-8"): A = {
    val json = new String(bytes, encoding)
    jsonToObject[A](json)
  } 

  final def jsonToObject[A: Manifest](json: String): A = {
    val jValue = parse(json)
    jValueToObject[A](jValue)
  }
  
  final def jValueToObject[A: Manifest](jValue: JValue): A = {
    Extraction.extract[A](jValue)
  }
}