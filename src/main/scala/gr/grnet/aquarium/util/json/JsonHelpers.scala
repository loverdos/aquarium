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

package gr.grnet.aquarium.util.json

import java.io.{ByteArrayOutputStream, OutputStream}
import org.codehaus.jackson.{JsonParser, JsonEncoding, JsonFactory, JsonGenerator}
import org.codehaus.jackson.map.ObjectMapper

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object JsonHelpers {
  final val DefaultObjectCodec = new ObjectMapper()
  final val DefaultJsonFactory = {
    new JsonFactory(DefaultObjectCodec)//.
//      enable(JsonGenerator.Feature.ESCAPE_NON_ASCII).
//      enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES).
//      enable(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS).
//      disable(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS)//.
//      enable(JsonParser.Feature.CANONICALIZE_FIELD_NAMES).
//      enable(JsonParser.Feature.ALLOW_COMMENTS).
//      enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
  }

  def getJsonGenerator(out: OutputStream): JsonGenerator = {
    DefaultJsonFactory.
      createJsonGenerator(out, JsonEncoding.UTF8).
      useDefaultPrettyPrinter()
  }

  def jsonStringOfJavaMap(map: java.util.Map[_, _]): String = {
    require(map ne null, "map <> null")
    val out = new ByteArrayOutputStream()
    val jg = getJsonGenerator(out)
    jg.writeObject(map)
    jg.flush()
    out.toString
  }
}
