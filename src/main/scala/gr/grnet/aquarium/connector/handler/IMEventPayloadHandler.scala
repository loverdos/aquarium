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
import org.slf4j.Logger
import gr.grnet.aquarium.converter.JsonTextFormat
import gr.grnet.aquarium.actor.RouterRole
import gr.grnet.aquarium.store.{IMEventStore, LocalFSEventStore}
import gr.grnet.aquarium.event.model.im.{StdIMEvent, IMEventModel}
import gr.grnet.aquarium.actor.message.event.ProcessIMEvent
import gr.grnet.aquarium.util.date.MutableDateCalc
import gr.grnet.aquarium.util.{LogHelpers, Tags, shortClassNameOf}

/**
 * A [[gr.grnet.aquarium.connector.handler.PayloadHandler]] for
 * [[gr.grnet.aquarium.event.model.im.IMEventModel]]s.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class IMEventPayloadHandler(aquarium: Aquarium, logger: Logger)
  extends GenericPayloadHandler[IMEventModel, IMEventStore#IMEvent](
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
        StdIMEvent.fromJsonTextFormat(jsonTextFormat)
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
        val id = imEvent.id
        val acceptMessage = None: Option[HandlerResult]

        // Let's decide if it is OK to store the event
        // Remember that OK == None as the returning result
        //
        // NOTE: If anything goes wrong with this function, then the handler
        //       (handlePayload in GenericPayloadHandler) will issue a Resend,
        //       so do not bother to catch exceptions here.

        // 1. Check if the same ID exists. Note that we use the ID sent by the event producer.
        //    It is a requirement that this ID is unique.
        val store = aquarium.imEventStore

        val imEventDebugString = imEvent.toDebugString

        store.findIMEventByID(id) match {
          case Some(_) ⇒
           // Reject the duplicate
           logger.debug("Rejecting duplicate ID for %s".format(imEventDebugString))
           Some(HandlerResultReject("Duplicate ID for %s".format(imEventDebugString)))

          case None ⇒
            // 2. Check that the new event is not older than our most recent event in DB.
            //    Sorry. We cannot tolerate out-of-order events here, since they really mess with the
            //    agreements selection and thus with the charging procedure.
            //
            // 2.1 The only exception is the user creation event. We allow late arrival, since
            //     the rest of Aquarium does nothing (but accumulate events) if the user has never
            //     been properly created (this behavior may be helpful to devops).
            //
            // TODO: We really need to store these bad events anyway but somewhere else (BadEventsStore?)
            def checkOlder(): Option[HandlerResult] = {
              store.findLatestIMEventByUserID(imEvent.userID) match {
                case Some(latestStoredEvent) ⇒
                  val occurredMillis       = imEvent.occurredMillis
                  val latestOccurredMillis = latestStoredEvent.occurredMillis

                  if(occurredMillis < latestOccurredMillis) {
                    val occurredDebugString       = new MutableDateCalc(occurredMillis).toYYYYMMDDHHMMSSSSS
                    val latestOccurredDebugString = new MutableDateCalc(latestOccurredMillis).toYYYYMMDDHHMMSSSSS

                    val formatter = (x: String) ⇒ x.format(
                      imEventDebugString,
                      occurredDebugString,
                      latestOccurredDebugString
                    )

                    logger.debug(formatter("Rejecting older %s. [%s] < [%s]"))

                    Some(HandlerResultReject(formatter("Older %s. [%s] < [%s]")))
                  } else {
                    None
                  }

                case None ⇒
                  None
              }
            }
            val userID = imEvent.userID

            val userHasBeenCreated = store.findCreateIMEventByUserID(userID).isDefined
            val isCreateUser       = imEvent.isCreateUser

            (userHasBeenCreated, isCreateUser) match {
              case (true, true) ⇒
                // (User CREATEd, CREATE event)
                val reason = "User is already created. Rejecting %s".format(imEventDebugString)
                logger.info(reason)
                Some(HandlerResultReject(reason))

              case (true, false) ⇒
                // (User CREATEd, MODIFY event)
                checkOlder()

              case (false, true) ⇒
                // (User not CREATEd, CREATE event)
                logger.info("User created by %s".format(imEventDebugString))
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
        aquarium.actorProvider.actorForRole(RouterRole) ! ProcessIMEvent(imEvent)
      }
    )
