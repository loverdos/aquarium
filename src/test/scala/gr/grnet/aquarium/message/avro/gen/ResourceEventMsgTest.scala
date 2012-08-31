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

package gr.grnet.aquarium.message.avro.gen

import java.io.ByteArrayOutputStream
import org.apache.avro.io.{EncoderFactory, Encoder, JsonEncoder}
import org.apache.avro.specific.SpecificDatumWriter
import org.junit.{Assert, Test}
import java.util
import org.codehaus.jackson.{JsonEncoding, JsonFactory}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
class ResourceEventMsgTest {
  @Test
  def testJSONSchema() {
    val rcEvent = new ResourceEventMsg()
    val schema = rcEvent.getSchema
    val generatedPrettySchema = schema.toString(true)
    val goodJSONSchema =
      """{
        |  "type" : "record",
        |  "name" : "ResourceEventMsg",
        |  "namespace" : "gr.grnet.aquarium.message.avro.gen",
        |  "fields" : [ {
        |    "name" : "id",
        |    "type" : "string",
        |    "aliases" : [ "originalID", "ID" ]
        |  }, {
        |    "name" : "idInStore",
        |    "type" : "string"
        |  }, {
        |    "name" : "occurredMillis",
        |    "type" : "long"
        |  }, {
        |    "name" : "receivedMillis",
        |    "type" : "long",
        |    "default" : 0
        |  }, {
        |    "name" : "userID",
        |    "type" : "string"
        |  }, {
        |    "name" : "clientID",
        |    "type" : "string"
        |  }, {
        |    "name" : "eventVersion",
        |    "type" : "string",
        |    "default" : "1.0"
        |  }, {
        |    "name" : "resource",
        |    "type" : "string",
        |    "aliases" : [ "resourceType" ]
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
        |        "name" : "AnyValueMsg",
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

  @Test
  def testJSONObjectOutput() {
    val rcEvent = ResourceEventMsg.newBuilder().
      setOriginalID("id-1").
      setClientID("client-1").
      setResource("diskspace").
      setInstanceID("1").
      setOccurredMillis(1000L).
      setUserID("foouser").
      setValue("123.32").
      setDetails(new util.HashMap[CharSequence, AnyValueMsg]()).
      build()

    val schema = rcEvent.getSchema
    val out = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().jsonEncoder(schema, out)
    val jsonGenerator = new JsonFactory().
      createJsonGenerator(out, JsonEncoding.UTF8).
      useDefaultPrettyPrinter()
    encoder.configure(jsonGenerator)
    val writer = new SpecificDatumWriter[ResourceEventMsg](schema)

    writer.write(rcEvent, encoder)

    encoder.flush()

    val generatedJSON = out.toString

    val goodJSONString =
      """{
        |  "id" : "id-1",
        |  "idInStore" : "",
        |  "occurredMillis" : 1000,
        |  "receivedMillis" : 0,
        |  "userID" : "foouser",
        |  "clientID" : "client-1",
        |  "eventVersion" : "1.0",
        |  "resource" : "diskspace",
        |  "instanceID" : "1",
        |  "value" : "123.32",
        |  "details" : {
        |  }
        |}""".stripMargin

    Assert.assertEquals(goodJSONString, generatedJSON)
  }
}
