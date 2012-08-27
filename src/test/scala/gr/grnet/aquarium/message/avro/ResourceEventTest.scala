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

import org.junit.{Assert, Test}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class ResourceEventTest {
  @Test
  def testJSON() {
    val rcEvent = new _ResourceEvent()
    val schema = rcEvent.getSchema
    val generatedPrettySchema = schema.toString(true)
    val goodJSONSchema =
    """{
        |  "type" : "record",
        |  "name" : "_ResourceEvent",
        |  "namespace" : "gr.grnet.aquarium.message.avro",
        |  "fields" : [ {
        |    "name" : "originalID",
        |    "type" : "string",
        |    "aliases" : [ "ID" ]
        |  }, {
        |    "name" : "inStoreID",
        |    "type" : "string"
        |  }, {
        |    "name" : "occurredMillis",
        |    "type" : "long"
        |  }, {
        |    "name" : "receivedMillis",
        |    "type" : "long"
        |  }, {
        |    "name" : "userID",
        |    "type" : "string"
        |  }, {
        |    "name" : "clientID",
        |    "type" : "string"
        |  }, {
        |    "name" : "eventVersion",
        |    "type" : {
        |      "type" : "enum",
        |      "name" : "_EventVersion",
        |      "symbols" : [ "VERSION_1_0" ]
        |    }
        |  }, {
        |    "name" : "resourceType",
        |    "type" : "string",
        |    "aliases" : [ "resource" ]
        |  }, {
        |    "name" : "instanceID",
        |    "type" : "string"
        |  }, {
        |    "name" : "value",
        |    "type" : "string"
        |  }, {
        |    "name" : "details",
        |    "type" : {
        |      "type" : "map",
        |      "values" : {
        |        "type" : "record",
        |        "name" : "_AnyValue",
        |        "fields" : [ {
        |          "name" : "anyValue",
        |          "type" : [ "null", "int", "long", "boolean", "double", "bytes", "string" ]
        |        } ]
        |      }
        |    }
        |  } ]
        |}""".stripMargin

    Assert.assertEquals(goodJSONSchema, generatedPrettySchema)
  }
}
