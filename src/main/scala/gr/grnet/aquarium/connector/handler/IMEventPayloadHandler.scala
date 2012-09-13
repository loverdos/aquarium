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

import gr.grnet.aquarium.Aquarium
import gr.grnet.aquarium.converter.JsonTextFormat
import gr.grnet.aquarium.message.avro.gen.IMEventMsg
import gr.grnet.aquarium.store.LocalFSEventStore
import gr.grnet.aquarium.util.{LogHelpers, Tags}
import org.slf4j.Logger
import gr.grnet.aquarium.message.avro.{MessageFactory, MessageHelpers, AvroHelpers}
import gr.grnet.aquarium.message.{IMEventModel, ResourceEventModel}
import gr.grnet.aquarium.event.DetailsModel

/**
 * A [[gr.grnet.aquarium.connector.handler.PayloadHandler]] for
 * [[gr.grnet.aquarium.message.avro.gen.IMEventMsg]]s.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class IMEventPayloadHandler(aquarium: Aquarium, logger: Logger)
  extends GenericPayloadHandler[IMEventMsg](
      // jsonParser: Array[Byte] ⇒ JsonTextFormat
      payload ⇒ {
        aquarium.converters.convertEx[JsonTextFormat](payload)
      },

      // onJsonParserSuccess: (Array[Byte], JsonTextFormat) ⇒ Unit
      (payload, jsonTextFormat) ⇒ {
      },

      // onJsonParserError: (Array[Byte], Throwable) ⇒ Unit
      (payload, error) ⇒ {
        val errMsg = "Error creating JSON from %s payload".format(Tags.IMEventTag)
        LogHelpers.logChainOfCausesAndException(logger, error, errMsg)

        LocalFSEventStore.storeUnparsedIMEvent(aquarium, payload, error)
      },

      // eventParser: JsonTextFormat ⇒ E
      jsonTextFormat ⇒ {

        try {
          AvroHelpers.specificRecordOfJsonString(jsonTextFormat.value, new IMEventMsg)
        }
        catch {
          case e:Throwable =>
              val model = aquarium.converters.convertEx[IMEventModel](jsonTextFormat)
              val msg = new IMEventMsg()
              msg.setOriginalID(model.id)
              msg.setClientID(model.clientID)
              msg.setDetails(DetailsModel.fromScalaModelMap(model.details))
              msg.setEventType(model.eventType)
              msg.setEventVersion(model.eventVersion)
              msg.setIsActive(java.lang.Boolean.valueOf(model.isActive))
              msg.setOccurredMillis(model.occurredMillis)
              msg.setReceivedMillis(model.receivedMillis)
              msg.setRole(model.role)
              msg.setUserID(model.userID)
              msg
        }
      },

      // onEventParserSuccess: (Array[Byte], E) ⇒ Unit
      (payload, event) ⇒ {
        LocalFSEventStore.storeIMEvent(aquarium, event, payload)
      },

      // onEventParserError: (Array[Byte], Throwable) ⇒ Unit
      (payload, error) ⇒ {
        val errMsg = "Error creating object model from %s payload".format(Tags.IMEventTag)
        LogHelpers.logChainOfCausesAndException(logger, error, errMsg)

        LocalFSEventStore.storeUnparsedIMEvent(aquarium, payload, error)
      },

      // preSaveAction: E ⇒ Option[HandlerResult]
      imEvent ⇒ {
        val id = imEvent.getOriginalID

        // Let's decide if it is OK to store the event
        // Remember that OK == None as the returning result
        //
        // NOTE: If anything goes wrong with this function, then the handler
        //       (handlePayload in GenericPayloadHandler) will issue a Resend,
        //       so do not bother to catch exceptions here.

        // 1. Check if the same ID exists. Note that we use the ID sent by the event producer.
        //    It is a requirement that this ID is unique.
        val store = aquarium.imEventStore

        val imEventDebugString = AvroHelpers.jsonStringOfSpecificRecord(imEvent)

        store.findIMEventByID(id) match {
          case Some(_) ⇒
           // Reject the duplicate
           logger.debug("Rejecting duplicate ID for %s".format(imEventDebugString))
           Some(HandlerResultReject("Duplicate ID for %s".format(imEventDebugString)))

          case None ⇒
            // No duplicate. Find the CREATE event if any
            val userID = imEvent.getUserID
            val createIMEventOpt = store.findCreateIMEventByUserID(userID)
            val userHasBeenCreated = createIMEventOpt.isDefined
            val isCreateUser = MessageHelpers.isIMEventCreate(imEvent)

            (userHasBeenCreated, isCreateUser) match {
              case (true, true) ⇒
                // (User CREATEd, CREATE event)
                val reason = "User %s is already created. Rejecting %s".format(userID, imEventDebugString)
                logger.warn(reason)
                Some(HandlerResultReject(reason))

              case (true, false) ⇒
                // (User CREATEd, MODIFY event)
                // Everything BEFORE the CREATE event is rejected
                val createIMEvent = createIMEventOpt.get
                if(imEvent.getOccurredMillis < createIMEvent.getOccurredMillis) {
                  val reason = "IMEvent(id=%s) is before the creation event (id=%s). Rejecting".format(
                    imEvent.getOriginalID,
                    createIMEvent.getOriginalID
                  )
                  logger.warn(reason)
                  Some(HandlerResultReject(reason))
                }
                else {
                  None
                }

              case (false, true) ⇒
                // (User not CREATEd, CREATE event)
                logger.info("Got user CREATE %s".format(imEventDebugString))
                None

              case (false, false) ⇒
                // (User not CREATEd, MODIFY event)
                // We allow any older modification events until the user is created
                logger.debug("User not created yet. Processing %s".format(imEventDebugString))
                None
            }
        }
      },

      // saveAction: E ⇒ S
      imEvent ⇒ {
        aquarium.imEventStore.insertIMEvent(imEvent)
      },

      // forwardAction: S ⇒ Unit
      imEvent ⇒ {
        aquarium.akkaService.getOrCreateUserActor(imEvent.getUserID) ! imEvent
      }
    )
