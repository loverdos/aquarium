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

import net.liftweb.json._
import org.junit.{Assert, Test}
import gr.grnet.aquarium.AquariumException
import gr.grnet.aquarium.converter.StdConverters.{StdConverters ⇒ Converters}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class Foo(map: Map[Int, Int])

object FooSerializer extends Serializer[Foo] {
  val FooClass = classOf[Foo]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Foo] = {
    case (TypeInfo(FooClass, _), jValue) ⇒
      var _map: Map[Int, Int] = Map()
      jValue match {
        case JObject(List(JField("map", JArray(pairs)))) ⇒
          for(pair <- pairs) {
            pair match {
              case JObject(List(JField("k", JInt(k)), JField("v", JInt(v)))) ⇒
                pair
                _map = _map.updated(k.intValue(), v.intValue())
              case _ ⇒
                throw new AquariumException(
                  "While deserializing a %s from %s".format(
                    gr.grnet.aquarium.util.shortNameOfClass(classOf[Foo]),
                    jValue))
            }
          }

        case _ ⇒
          throw new AquariumException(
            "While deserializing a %s from %s".format(
              gr.grnet.aquarium.util.shortNameOfClass(classOf[Foo]),
              jValue))
      }
      Foo(_map)

    case other ⇒
      throw new AquariumException(
        "While desiariling a %s from %s".format(
          gr.grnet.aquarium.util.shortNameOfClass(classOf[Foo]),
          other))
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case Foo(map) ⇒
      val kvs = for {
        (k, v) <- map
      } yield JObject(
        List(
          JField("k", JInt(k)),
          JField("v", JInt(v))
        )
      )

      JObject(List(JField("map", JArray(kvs.toList))))
  }
}

class ConverterTest {
  implicit val Formats = JsonConversions.Formats + FooSerializer

  @Test
  def testJSONMapConversion: Unit = {
    val foo = Foo(Map(1 -> 1, 2 -> 2, 3 -> 3))
    val foo2 = JsonConversions.jsonToObject[Foo](JsonConversions.anyToJson(foo))

    Assert.assertEquals(foo, foo2)
  }

  @Test
  def testJsonText2Pretty: Unit = {
    val json = """{
    "x" : 1,
    "y" : { "a": true, "b": []},
    "z" : "once upon a time in the west"
    }"""

    val pretty  = Converters.convertEx[PrettyJsonTextFormat](json)
  }

  @Test
  def testJsonText2Compact: Unit = {
   val json = """{
   "x" : 1,
   "y" : { "a": true, "b": []},
   "z" : "once upon a time in the west"
   }"""

   val compact = Converters.convertEx[CompactJsonTextFormat](json)
 }

  @Test
  def testJsonText: Unit = {
    val json = """{"x":1,"y":2}"""

    val pretty  = Converters.convertEx[PrettyJsonTextFormat](json)
    val compact = Converters.convertEx[CompactJsonTextFormat](json)
    val jValueOfPretty  = Converters.convertEx[JValue](pretty)
    val jValueOfCompact = Converters.convertEx[JValue](compact)

    Assert.assertEquals(jValueOfPretty, jValueOfCompact)
  }
}
