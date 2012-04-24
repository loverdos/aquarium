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
import gr.grnet.aquarium.util.json.JsonSupport
import com.mongodb.util.JSON
import com.mongodb.DBObject


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
    builder.registerConverter(AnyToPrettyJsonTextConverter)

    // Any => CompactJsonTextFormat
    builder.registerConverter(AnyToCompactJsonTextConverter)

    // JsonTextFormat => AnyRef
    builder.registerConverter(JsonTextToObjectConverter)

    // JsonSupport => DBObject
    builder.registerConverter(JsonSupportToDBObjectConverter)

    // CharSequence => DBObject
    builder.registerConverter(CharSequenceToDBObjectConverter)

    builder
  }

  object AnyToJValueConverter extends NonStrictSourceConverterSkeleton[Any, JValue] {
    @scala.throws(classOf[ConverterException])
    def convertEx[T: Type](sourceValue: Any) = {
      JsonConversions.anyToJValue(sourceValue)(JsonConversions.Formats).asInstanceOf[T]
    }
  }

  object AnyToPrettyJsonTextConverter extends NonStrictSourceConverterSkeleton[Any, PrettyJsonTextFormat] {
    @scala.throws(classOf[ConverterException])
    def convertEx[T: Type](sourceValue: Any) = {
      PrettyJsonTextFormat(JsonConversions.anyToJson(sourceValue, true)(JsonConversions.Formats)).asInstanceOf[T]
    }
  }

  object AnyToCompactJsonTextConverter extends NonStrictSourceConverterSkeleton[Any, CompactJsonTextFormat] {
    @scala.throws(classOf[ConverterException])
    def convertEx[T: Type](sourceValue: Any) = {
      CompactJsonTextFormat(JsonConversions.anyToJson(sourceValue, false)(JsonConversions.Formats)).asInstanceOf[T]
    }
  }

  object JsonTextToObjectConverter extends Converter {
    def isStrictSource = false

    def canConvertType[S: Type, T: Type] = {
      erasureOf[JsonTextFormat].isAssignableFrom(erasureOf[S])
    }

    @scala.throws(classOf[ConverterException])
    def convertEx[T: Type](sourceValue: Any) = {
      JsonConversions.jsonToObject[T](sourceValue.asInstanceOf[JsonTextFormat].value)(manifest[T], JsonConversions.Formats)
    }
  }

  object JsonSupportToDBObjectConverter extends NonStrictSourceConverterSkeleton[JsonSupport, DBObject] {
    @scala.throws(classOf[ConverterException])
    def convertEx[T: Type](sourceValue: Any) = {
      JSON.parse(sourceValue.asInstanceOf[JsonSupport].toJson).asInstanceOf[T]
    }
  }

  object CharSequenceToDBObjectConverter extends NonStrictSourceConverterSkeleton[CharSequence, DBObject] {
    @scala.throws(classOf[ConverterException])
    def convertEx[T: Type](sourceValue: Any) = {
      JSON.parse(sourceValue.asInstanceOf[CharSequence].toString).asInstanceOf[T]
    }
 }

  final val StdConverters: Converters = builder.build
}

