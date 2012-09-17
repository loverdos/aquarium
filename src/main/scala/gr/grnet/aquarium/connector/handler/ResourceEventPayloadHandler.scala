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

import gr.grnet.aquarium.{AquariumException, Aquarium}
import gr.grnet.aquarium.converter.JsonTextFormat
import gr.grnet.aquarium.message.avro.{MessageFactory, AvroHelpers}
import gr.grnet.aquarium.message.avro.gen.{AnyValueMsg, ResourceEventMsg}
import gr.grnet.aquarium.store.LocalFSEventStore
import gr.grnet.aquarium.util._
import date.TimeHelpers
import json.JsonNames
import org.slf4j.Logger
import gr.grnet.aquarium.message.{MessageConstants, ResourceEventModel}
import gr.grnet.aquarium.event.DetailsModel
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.JsonNode
import java.util.{ArrayList => JArrayList}

/**
 * A [[gr.grnet.aquarium.connector.handler.PayloadHandler]] for
 * [[gr.grnet.aquarium.message.avro.gen.ResourceEventMsg]]s.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class ResourceEventPayloadHandler(aquarium: Aquarium, logger: Logger)
  extends GenericPayloadHandler[ResourceEventMsg](
      // jsonParser: Array[Byte] ⇒ JsonTextFormat
      payload ⇒ {
        aquarium.converters.convertEx[JsonTextFormat](payload)
      },

      // onJsonParserSuccess: (Array[Byte], JsonTextFormat) ⇒ Unit
      (payload, jsonTextFormat) ⇒ {
      },

      // onJsonParserError: (Array[Byte], Throwable) ⇒ Unit
      (payload, error) ⇒ {
        val errMsg = "Error creating JSON from %s payload".format(Tags.ResourceEventTag)
        LogHelpers.logChainOfCauses(logger, error, errMsg)
        logger.error(errMsg, error)

        LocalFSEventStore.storeUnparsedResourceEvent(aquarium, payload, error)
      },

      // eventParser: JsonTextFormat ⇒ E
      jsonTextFormat ⇒ {
        try {
          AvroHelpers.specificRecordOfJsonString(jsonTextFormat.value, new ResourceEventMsg)
        } catch {
          case e:Throwable =>
            val msg = new ResourceEventMsg()

            val mapper = new ObjectMapper()
            val root = mapper.readValue(jsonTextFormat.value, classOf[JsonNode])

            msg.setOriginalID(root.get(JsonNames.id).getTextValue)
            msg.setClientID(root.get(JsonNames.clientID).getTextValue)
            msg.setEventVersion(root.get(JsonNames.eventVersion).getTextValue)
            msg.setOccurredMillis(root.get(JsonNames.occurredMillis).getLongValue)
            msg.setReceivedMillis(TimeHelpers.nowMillis())
            msg.setUserID(root.get(JsonNames.userID).getTextValue)
            msg.setResource(root.get(JsonNames.resource).getTextValue)
            msg.setInstanceID(root.get(JsonNames.instanceID).getTextValue)
            msg.setValue(root.get(JsonNames.value).getValueAsText)

            // Get the details. This is trickier
            val details = DetailsModel.make
            val detailsNode = root.get(JsonNames.details)
            val detailsChildren = detailsNode.getFields
            while(detailsChildren.hasNext) {
              val detailsChildEntry = detailsChildren.next()
              val detailsFieldName = detailsChildEntry.getKey
              val detailsFieldValue: JsonNode = detailsChildEntry.getValue

              if(detailsFieldName == JsonNames.versions) {
                if(detailsFieldValue.isArray) {
                  val jList = new JArrayList[AnyValueMsg]
                  val versionNodes = detailsFieldValue.getElements
                  while(versionNodes.hasNext) {
                    val versionNode = versionNodes.next()
                    val versionTxt = versionNode.getValueAsText
                    val versionAnyValueMsg = MessageFactory.anyValueMsgOfString(versionTxt)
                    jList.add(versionAnyValueMsg)
                  }
                  DetailsModel.setList(details, JsonNames.versions, jList)
                }
                else if(detailsFieldValue.isTextual) {
                  val jList = new JArrayList[AnyValueMsg]
                  val versionsTxt = detailsFieldValue.getTextValue
                  val versions = versionsTxt.split("""\s*,\s*""").map(_.trim)
                  for(versionTxt <- versions) {
                    val versionAnyValueMsg = MessageFactory.anyValueMsgOfString(versionTxt)
                    jList.add(versionAnyValueMsg)
                  }
                  DetailsModel.setList(details, JsonNames.versions, jList)
                }
                else {
                  throw new AquariumException(
                    "Bad value for %s.%s field: %s",
                    JsonNames.details,
                    JsonNames.versions,
                    detailsFieldValue.toString
                  )
                }
              } else {
                DetailsModel.setString(details, detailsFieldName, detailsFieldValue.getValueAsText)
              }

              msg.setDetails(details)
            }

            msg
        }
      },

      // onEventParserSuccess: (Array[Byte], E) ⇒ Unit
      (payload, event) ⇒ {
        LocalFSEventStore.storeResourceEvent(aquarium, event, payload)
      },

      // onEventParserError: (Array[Byte], Throwable) ⇒ Unit
      (payload, error) ⇒ {
        val errMsg = "Error creating object model from %s payload".format(Tags.ResourceEventTag)
        LogHelpers.logChainOfCauses(logger, error, errMsg)
        logger.error(errMsg, error)

        LocalFSEventStore.storeUnparsedResourceEvent(aquarium, payload, error)
      },

      // preSaveAction: E ⇒ Option[HandlerResult]
      rcEvent ⇒ {
        val className = shortClassNameOf(rcEvent)
        val id = rcEvent.getOriginalID

        // Let's decide if it is OK to store the event
        // Remember that OK == None as the returning result
        //
        // NOTE: If anything goes wrong with this function, then the handler
        //       (handlePayload in GenericPayloadHandler) will issue a Resend,
        //       so do not bother to catch exceptions here.

        // 1. Check if the same ID exists. Note that we use the ID sent by the event producer.
        //    It is a requirement that this ID is unique.
        val store = aquarium.resourceEventStore
        store.findResourceEventByID(id) match {
          case Some(_) ⇒
            // Reject the duplicate
            Some(HandlerResultReject("Duplicate %s with id = %s".format(className, id)))

          case None ⇒
            None
        }
      },

      // saveAction: E ⇒ S
      rcEvent ⇒ {
        aquarium.resourceEventStore.insertResourceEvent(rcEvent)
      },

      // forwardAction: S ⇒ Unit
      rcEvent ⇒ {
        aquarium.akkaService.getOrCreateUserActor(rcEvent.getUserID) ! rcEvent
      }
    )
