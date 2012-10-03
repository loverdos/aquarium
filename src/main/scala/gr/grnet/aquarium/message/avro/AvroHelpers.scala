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

import gr.grnet.aquarium.util.json.JsonHelpers
import java.io.{OutputStream, ByteArrayOutputStream}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericRecord, GenericDatumWriter}
import org.apache.avro.io.{JsonDecoder, DecoderFactory, JsonEncoder, EncoderFactory}
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter, SpecificRecord}


/**
 * Provides helper methods for generic Avro-related facilities.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object AvroHelpers {
  private[this] final val DefaultEncoderFactory = EncoderFactory.get()
  private[this] final val DefaultDecoderFactory = DecoderFactory.get()

  def getJsonEncoder(schema: Schema, out: OutputStream): JsonEncoder = {
    val encoder = DefaultEncoderFactory.jsonEncoder(schema, out)
    val jsonGenerator = JsonHelpers.getJsonGenerator(out)
    encoder.configure(jsonGenerator)
  }

  def getJsonDecoder(schema: Schema, in: String): JsonDecoder = {
    DefaultDecoderFactory.jsonDecoder(schema, in)
  }

  def specificRecordOfJsonString[R <: SpecificRecord](json: String, fresh: R): R = {
    val schema = fresh.getSchema
    val decoder = getJsonDecoder(schema, json)
    val reader = new SpecificDatumReader[R](schema)
    reader.read(fresh, decoder)
  }

  def jsonStringOfSpecificRecord[T <: SpecificRecord](t: T): String = {
    val schema = t.getSchema()
    val out = new ByteArrayOutputStream()
    val encoder = getJsonEncoder(schema, out)
    val writer = new SpecificDatumWriter[T](schema)

    writer.write(t, encoder)
    encoder.flush()
    out.toString
  }

  def jsonStringOfGenericRecord[T <: GenericRecord](t: T): String = {
    val schema = t.getSchema()
    val out = new ByteArrayOutputStream()
    val encoder = getJsonEncoder(schema, out)
    val writer = new GenericDatumWriter[T](schema)

    writer.write(t, encoder)
    encoder.flush()
    out.toString
  }

  def bytesOfSpecificRecord[R <: SpecificRecord](r: R): Array[Byte] = {
    val schema = r.getSchema
    val out = new ByteArrayOutputStream()
    val encoder = DefaultEncoderFactory.binaryEncoder(out, null)
    val writer = new SpecificDatumWriter[R](schema)

    writer.write(r, encoder)
    encoder.flush()
    out.toByteArray
  }

  def specificRecordOfBytes[R <: SpecificRecord](bytes: Array[Byte], fresh: R): R = {
    val decoder = DefaultDecoderFactory.binaryDecoder(bytes, null)
    val reader = new SpecificDatumReader[R](fresh.getSchema)
    reader.read(fresh, decoder)
  }
}
