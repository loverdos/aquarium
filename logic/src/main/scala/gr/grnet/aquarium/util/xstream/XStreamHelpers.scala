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

package gr.grnet.aquarium.util
package xstream

import com.thoughtworks.xstream.XStream
import com.ckkloverdos.maybe.{Failed, Just, Maybe}
import com.ckkloverdos.resource.StreamResource
import gr.grnet.aquarium.messaging.amqp.rabbitmq.v091.confmodel._
import gr.grnet.aquarium.store.mongodb.confmodel._


/**
 * Utilities for making our life easier with the XStream library.
 *
 * If you need to grab an `XStream`, use `newXStream` or the `DefaultXStream`.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
object XStreamHelpers {
  val DefaultXStream = newXStream

  def prepareXStreamAlias[T : Manifest](xs: XStream): XStream = {
    val theClass = manifest[T].erasure
    xs.alias(shortClassNameOf(theClass), theClass)
    xs
  }
  
  def prepareXStreamAliases(xs: XStream): XStream = {
    // RabbitMQ
    prepareXStreamAlias[RabbitMQConfigurationsModel](xs)
    prepareXStreamAlias[RabbitMQConfigurationModel](xs)
    prepareXStreamAlias[RabbitMQConnectionModel](xs)
    prepareXStreamAlias[RabbitMQProducerModel](xs)
    prepareXStreamAlias[RabbitMQConsumerModel](xs)

    // MongoDB
    prepareXStreamAlias[MongoDBConfigurationModel](xs)
    prepareXStreamAlias[ServerAddressConfigurationModel](xs)

    xs.alias("List", classOf[::[_]])
    xs.alias("Nil", manifest[Nil.type].erasure)
    xs
  }

  def prepareXStreamConverters(xs: XStream): XStream = {
    xs.registerConverter(new ListConverter(xs.getMapper))
    xs
  }

  def prepareXStream(xs: XStream): XStream = {
    prepareXStreamAliases(xs)
    prepareXStreamConverters(xs)
  }

  def newXStream: XStream = prepareXStream(new XStream)
  
  def parseType[T: Manifest](xml: String, xs: XStream = DefaultXStream): Maybe[T] = {
    try Just(xs.fromXML(xml).asInstanceOf[T])
    catch {
      case e: Exception => Failed(e, "XStream could not parse XML to value of type %s".format(manifest[T]))
    }
  }
  
  def parseType[T : Manifest](resource: StreamResource, xs: XStream): Maybe[T] = {
    resource.mapString(parseType[T](_, xs)).flatten1
  }
}