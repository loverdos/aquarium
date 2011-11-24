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

package gr.grnet.aquarium.messaging.amqp
package rabbitmq
package v091


import confmodel.RabbitMQConfigurationsModel
import gr.grnet.aquarium.util.xstream.XStreamHelpers
import com.ckkloverdos.maybe.{Failed, NoVal, Just, Maybe}
import gr.grnet.aquarium.util.{Loggable, shortClassNameOf}
import com.ckkloverdos.resource.{StreamResource, StreamResourceContext}
import com.thoughtworks.xstream.XStream

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class RabbitMQConfigurations(val confModel: RabbitMQConfigurationsModel) extends AMQPConfigurations {
  private val _configurations = confModel.configurations.map(new RabbitMQConfiguration(_))
  def configurations = _configurations
}

object RabbitMQConfigurations extends Loggable {
  object RCFolders {
    val rabbitmq = "rabbitmq"

    val producers = "producers"
    val consumers = "consumers"
  }

  object PropFiles {
    val configurations = "configurations.xml"
  }

  def apply(resource: StreamResource, xs: XStream): Maybe[RabbitMQConfigurations] = {
    logger.info("Reading rabbitmq configuration from %s".format(resource.url))
    val maybeConfsModel = XStreamHelpers.parseType[RabbitMQConfigurationsModel](resource, xs)

    maybeConfsModel match {
      case Just(confsModel) =>
        // parsed <configurations>.xml (like: rabbitmq/configurations.xml)
        // now have a RabbitMQConfigurationsModel
        logger.debug("rabbitmq configuration from %s is\n%s".format(resource.url, resource.stringContent.getOr("")))
        val confsModelErrors = confsModel.validateConfModel

        if(confsModelErrors.size > 0) {
          val errorMsg = "%s has %s error(s)".format(shortClassNameOf(confsModel.getClass))
          logger.error(errorMsg)
          for(error <- confsModelErrors) {
            logger.error(error)
          }
          Failed(new Exception(errorMsg))
        } else {
          Just(new RabbitMQConfigurations(confsModel))
        }
      case NoVal =>
        NoVal
      case Failed(e, m) =>
        Failed(e, m)
    }
  }
  
  def apply(baseRC: StreamResourceContext, xs: XStream = XStreamHelpers.DefaultXStream): Maybe[RabbitMQConfigurations] = {
    val rabbitMQRC = baseRC / RCFolders.rabbitmq

    val maybeConfsResource = rabbitMQRC.getResource(PropFiles.configurations)
    maybeConfsResource.flatMap(this(_, xs))
  }
}