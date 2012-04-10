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

package gr.grnet.aquarium.converter

import net.liftweb.json.JsonAST.JValue
import com.ckkloverdos.convert._


/**
 * Every data type conversion happening inside Aquarium is defined here.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

object StdConverters {
  private[this] final lazy val builder: ConvertersBuilder = {
    val builder: ConvertersBuilder = new StdConvertersBuilder().registerDefaultConversions()

    // Any => JValue
    builder.registerConverter(AnyToJValueConverter)

    // Any => PrettyJsonTextFormat
    builder.registerConverter(AnyToPrettyJsonTexConverter)

    // Any => CompactJsonTextFormat
    builder.registerConverter(AnyToCompactJsonTextConverter)

    // JsonTextFormat => AnyRef
    builder.registerConverter(JsonTextToObjectConverter)

    builder
  }

  object AnyToJValueConverter extends Converter {
    def canConvertType[S: Manifest, T: Manifest] = {
      manifest[T].erasure.isAssignableFrom(classOf[JValue])
    }

    @scala.throws(classOf[ConverterException])
    def convertEx[T: Manifest](sourceValue: Any) = {
      JsonConversions.anyToJValue(sourceValue)(JsonConversions.Formats).asInstanceOf[T]
    }

    def isStrictSource = false
  }

  object AnyToPrettyJsonTexConverter extends Converter {
    def canConvertType[S: Manifest, T: Manifest] = {
      manifest[T].erasure == classOf[PrettyJsonTextFormat]
    }

    @scala.throws(classOf[ConverterException])
    def convertEx[T: Manifest](sourceValue: Any) = {
      PrettyJsonTextFormat(JsonConversions.anyToJson(sourceValue, true)(JsonConversions.Formats)).asInstanceOf[T]
    }

    def isStrictSource = false
  }

  object AnyToCompactJsonTextConverter extends Converter {
    def canConvertType[S: Manifest, T: Manifest] = {
      manifest[T].erasure == classOf[CompactJsonTextFormat]
    }

    @scala.throws(classOf[ConverterException])
    def convertEx[T: Manifest](sourceValue: Any) = {
      CompactJsonTextFormat(JsonConversions.anyToJson(sourceValue, false)(JsonConversions.Formats)).asInstanceOf[T]
    }

    def isStrictSource = false
  }

  object JsonTextToObjectConverter extends Converter {
    def canConvertType[S: Manifest, T: Manifest] = {
      manifest[S].erasure.isAssignableFrom(classOf[JsonTextFormat])
    }

    @scala.throws(classOf[ConverterException])
    def convertEx[T: Manifest](sourceValue: Any) = {
      JsonConversions.jsonToObject[T](sourceValue.asInstanceOf[JsonTextFormat].value)(manifest[T], JsonConversions.Formats)
    }

    def isStrictSource = false
  }

  final val StdConverters: Converters = builder.build
}

