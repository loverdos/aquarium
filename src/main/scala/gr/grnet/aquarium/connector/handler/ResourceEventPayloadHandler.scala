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

package gr.grnet.aquarium.connector.handler

import org.slf4j.Logger
import gr.grnet.aquarium.store.{LocalFSEventStore, ResourceEventStore}
import gr.grnet.aquarium.converter.JsonTextFormat
import gr.grnet.aquarium.Aquarium
import gr.grnet.aquarium.event.model.resource.{StdResourceEvent, ResourceEventModel}
import gr.grnet.aquarium.actor.message.event.ProcessResourceEvent
import gr.grnet.aquarium.actor.RouterRole
import gr.grnet.aquarium.util._

/**
 * A [[gr.grnet.aquarium.connector.handler.PayloadHandler]] for
 * [[gr.grnet.aquarium.event.model.resource.ResourceEventModel]]s.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class ResourceEventPayloadHandler(aquarium: Aquarium, logger: Logger)
  extends GenericPayloadHandler[ResourceEventModel, ResourceEventStore#ResourceEvent](
      payload ⇒ {
        aquarium.converters.convertEx[JsonTextFormat](payload)
      },

      (payload, jsonTextFormat) ⇒ {
      },

      (payload, error) ⇒ {
        logger.error("Error creating JSON from %s payload".format(Tags.ResourceEventTag), error)

        LocalFSEventStore.storeUnparsedResourceEvent(aquarium, payload, error)
      },

      jsonTextFormat ⇒ {
        StdResourceEvent.fromJsonTextFormat(jsonTextFormat)
      },

      (payload, event) ⇒ {
        LocalFSEventStore.storeResourceEvent(aquarium, event, payload)
      },

      (payload, error) ⇒ {
        logger.error("Error creating object model from %s payload".format(Tags.ResourceEventTag), error)
      },

      rcEvent ⇒ {
        val className = shortClassNameOf(rcEvent)
        val id = rcEvent.id

        // Let's decide if it is OK to store the event
        // Remember that OK == None as the returning result
        //
        // NOTE: If anything goes wrong with this function, then the handler will issue a Resend, so
        //       do not bother to catch exceptions here.

        // 1. Check if the same ID exists. Note that we use the ID sent by the event producer.
        //    It is a requirement that this ID is unique.
        val store = aquarium.resourceEventStore
        store.findResourceEventById(id) match {
          case Some(_) ⇒
            // Reject the duplicate
            Some(HandlerResultReject("Duplicate %s with id = %s".format(className, id)))

          case None ⇒
            None
        }
      },

      rcEvent ⇒ {
        aquarium.resourceEventStore.insertResourceEvent(rcEvent)
      },

      rcEvent ⇒ {
        aquarium.actorProvider.actorForRole(RouterRole) ! ProcessResourceEvent(rcEvent)
      }
    )