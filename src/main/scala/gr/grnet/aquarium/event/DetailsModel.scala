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

package gr.grnet.aquarium.event

import gr.grnet.aquarium.message.avro.MessageFactory
import gr.grnet.aquarium.message.avro.gen.{DetailsMsg, AnyValueMsg}
import java.{util ⇒ ju}
import org.apache.avro.generic.IndexedRecord
import org.apache.avro.specific.SpecificData
import scala.collection.JavaConverters.mapAsJavaMapConverter

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object DetailsModel {
  type Type = ju.Map[String, AnyValueMsg] // This is from Avro messages

  def make: Type = new ju.HashMap()

  def setBoolean(msgDetails: Type, name: String, value: Boolean = true) {
    msgDetails.put(name, MessageFactory.anyValueMsgOfBoolean(value))
  }

  def setString(msgDetails: Type, name: String, value: String) {
    msgDetails.put(name, MessageFactory.anyValueMsgOfString(value))
  }

  def fromScalaMap(map: scala.collection.Map[String, AnyValueMsg]): Type = {
    map.asJava
  }

  def fromScalaModelMap(map: scala.collection.Map[String, String]): Type = {
    map.map {case (s1,s2) =>
      (s1,MessageFactory.anyValueMsgOfString(s2))
    }.asJava
  }

  def fromScalaTuples(tuples: (String, AnyValueMsg)*): Type = {
    fromScalaMap(Map(tuples:_*))
  }

  def copyOf(details: Type): Type = {
    val wrapper = DetailsMsg.newBuilder().setDetails(details).build()
    val copy = SpecificData.get().deepCopy(wrapper.getSchema, wrapper)
    copy.asInstanceOf[IndexedRecord].get(0).asInstanceOf[Type]
  }
}