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
import xml.NodeSeq
import net.liftweb.json.Xml


/**
 * Every data type conversion happening inside Aquarium is defined here.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

object StdConverters {
  private[this] final lazy val builder: ConvertersBuilder = {
    val builder: ConvertersBuilder = new StdConvertersBuilder().registerDefaultConversions()

    // Any ⇒ JValue
    builder.registerConverter(AnyToJValueConverter)

    // Any ⇒ PrettyJsonTextFormat
    builder.registerConverter(AnyToPrettyJsonTextConverter)

    // Any ⇒ CompactJsonTextFormat
    builder.registerConverter(AnyToCompactJsonTextConverter)

    // JsonTextFormat ⇒ AnyRef
    builder.registerConverter(JsonTextToObjectConverter)

    // JsonSupport ⇒ DBObject
    builder.registerConverter(JsonSupportToDBObjectConverter)

    // CharSequence ⇒ DBObject
    builder.registerConverter(CharSequenceToDBObjectConverter)

    // JValue ⇒ NodeSeq
    builder.registerConverter(JValueToNodeSeqConverter)

    // Array[Byte] ⇒ JsonTextFormat
    builder.registerConverter(ByteArrayToJsonTextConverter)

    builder
  }

  object ByteArrayToJsonTextConverter extends StrictSourceConverterSkeleton[Array[Byte], JsonTextFormat] {
    @throws(classOf[ConverterException])
    final protected def convertEx_(sourceValue: Array[Byte]) = {
      JsonTextFormat(JsonConversions.jsonBytesToJson(sourceValue))
    }
  }

  object AnyToJValueConverter extends NonStrictSourceConverterSkeleton[Any, JValue] {
    @throws(classOf[ConverterException])
    final protected def convertEx_(sourceValue: Any) = {
      JsonConversions.anyToJValue(sourceValue)(JsonConversions.Formats)
    }
  }

  object AnyToPrettyJsonTextConverter extends NonStrictSourceConverterSkeleton[Any, PrettyJsonTextFormat] {
    @throws(classOf[ConverterException])
    final protected def convertEx_(sourceValue: Any) = {
      PrettyJsonTextFormat(JsonConversions.anyToJson(sourceValue, true)(JsonConversions.Formats))
    }
  }

  object AnyToCompactJsonTextConverter extends NonStrictSourceConverterSkeleton[Any, CompactJsonTextFormat] {
    @throws(classOf[ConverterException])
    final protected def convertEx_(sourceValue: Any) = {
      CompactJsonTextFormat(JsonConversions.anyToJson(sourceValue, false)(JsonConversions.Formats))
    }
  }

  object JsonTextToObjectConverter extends Converter {
    def isStrictSource = false

    def canConvertType[S: Type, T: Type] = {
      erasureOf[JsonTextFormat].isAssignableFrom(erasureOf[S])
    }

    @throws(classOf[ConverterException])
    def convertEx[T: Type](sourceValue: Any) = {
      // Generic deserializer from json string to a business logic model
      JsonConversions.jsonToObject[T](sourceValue.asInstanceOf[JsonTextFormat].value)(manifest[T], JsonConversions.Formats)
    }
  }

  object JsonSupportToDBObjectConverter extends NonStrictSourceConverterSkeleton[JsonSupport, DBObject] {
    @throws(classOf[ConverterException])
    final protected def convertEx_(sourceValue: JsonSupport) = {
      JSON.parse(sourceValue.asInstanceOf[JsonSupport].toJsonString).asInstanceOf[DBObject]
    }
  }

  object CharSequenceToDBObjectConverter extends NonStrictSourceConverterSkeleton[CharSequence, DBObject] {
    @throws(classOf[ConverterException])
    final protected def convertEx_(sourceValue: CharSequence) = {
      JSON.parse(sourceValue.asInstanceOf[CharSequence].toString).asInstanceOf[DBObject]
    }
  }

  object JValueToNodeSeqConverter extends NonStrictSourceConverterSkeleton[JValue, NodeSeq] {
    @throws(classOf[ConverterException])
    final protected def convertEx_(sourceValue: JValue) = {
      Xml.toXml(sourceValue.asInstanceOf[JValue])
    }
  }

  final val AllConverters: Converters = builder.build
}

