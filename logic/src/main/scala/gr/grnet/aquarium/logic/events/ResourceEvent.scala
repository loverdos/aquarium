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

package gr.grnet.aquarium.messaging

import net.liftweb.json.{Extraction, parse => parseJson, DefaultFormats, JsonAST, Printer}
import net.liftweb.json.Xml
import net.liftweb.json.ext.JodaTimeSerializers

/**
 * Event sent to Aquarium by clients for resource accounting.
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 * @author Georgios Gousios <gousiosg@gmail.com>.
 */
case class ResourceEvent(
  userId: Long,
  cliendId: Long,
  resource: String,
  timestamp: Long,
  eventVersion: Short,
  details: Map[String, String]
) {

  def toJValue: JsonAST.JValue = {
    implicit val formats = ResourceEvent.DefaultJsonFormats
    Extraction.decompose(this)
  }

  def toJson: String = {
    Printer.pretty(JsonAST.render(this.toJValue))
  }

  def toBytes: Array[Byte] = {
    toJson.getBytes("UTF-8")
  }
  
  def toXml = Xml.toXml(toJValue).toString()
}

object ResourceEvent {
  val DefaultJsonFormats = DefaultFormats ++ JodaTimeSerializers.all

  def fromJson(json: String): ResourceEvent = {
    implicit val formats = DefaultJsonFormats
    val jsonAST = parseJson(json)
    Extraction.extract(jsonAST)
  }

  def fromJValue(jsonAST: JsonAST.JValue): ResourceEvent = {
    implicit val formats = DefaultJsonFormats
    Extraction.extract(jsonAST)
  }

  def fromBytes(bytes: Array[Byte]): ResourceEvent = {
    fromJson(new String(bytes, "UTF-8"))
  }

  def fromXml(xml: String): ResourceEvent = {
    fromJValue(Xml.toJson(scala.xml.XML.loadString(xml)))
  }
}