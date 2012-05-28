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

import gr.grnet.aquarium.converter.JsonTextFormat
import gr.grnet.aquarium.connector.handler._
import gr.grnet.aquarium.event.model.ExternalEventModel
import gr.grnet.aquarium.util.safeUnit
import gr.grnet.aquarium.service.EventBusService
import gr.grnet.aquarium.Aquarium
import com.ckkloverdos.maybe.{NoVal, Just, Failed, MaybeEither}

/**
 * Generic handler of events arriving to Aquarium.
 *
 * We first parse them to JSON ([[gr.grnet.aquarium.converter.JsonTextFormat]]) and an appropriate event model
 * (`E <:` [[gr.grnet.aquarium.event.model.ExternalEventModel]]),
 * then store them to DB
 * (`S <:` [[gr.grnet.aquarium.event.model.ExternalEventModel]])
 * and then forward them to business logic.
 *
 * All the above actions are given polymorphically via appropriate functions.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

class GenericPayloadHandler[E <: ExternalEventModel, S <: ExternalEventModel](
    /**
     * Parses payload bytes to a JSON string.
     * The incoming payload must be in UTF-8.
     */
    jsonParser: Array[Byte] ⇒ JsonTextFormat,

    /**
     * This is called if no error happens while parsing to JSON.
     */
    onJsonParserSuccess: (Array[Byte], JsonTextFormat) ⇒ Unit,

    /**
     * This is called if an error happens while parsing to JSON.
     */
    onJsonParserError: (Array[Byte], Throwable) ⇒ Unit,

    /**
     * Parses JSON into a domain object.
     */
    eventParser: JsonTextFormat ⇒ E,

    /**
     * This is called if no error happens while parsing to a domain object.
     */
    onEventParserSuccess: (Array[Byte], E) ⇒ Unit,

    /**
     * This is called if an error happens while parsing to a domain object.
     */
    onEventParserError: (Array[Byte], Throwable) ⇒ Unit,

    /**
     * This is called with the parsed domain object as a final check before saving to DB.
     * If the result is `None`, then we proceed with the `saveAction` else the returned
     * [[gr.grnet.aquarium.connector.handler.HandlerResult]] is communicated back from the
     * `handlePayload` method.
     */
    preSaveAction: E ⇒ Option[HandlerResult],

    /**
     * Saves the parsed domain object to DB. Returns the saved domain object.
     */
    saveAction: E ⇒ S,

    /**
     * Forwards the saved domain object for further processing.
     */
    forwardAction: S ⇒ Unit) extends PayloadHandler {

  def handlePayload(payload: Array[Byte]): HandlerResult = {
    // 1. try to parse as json
    MaybeEither {
      jsonParser(payload)
    } match {
      case Failed(e) ⇒
        safeUnit(onJsonParserError(payload, e))

        HandlerResultReject(e.getMessage)

      case Just(jsonTextFormat) ⇒
        safeUnit(onJsonParserSuccess(payload, jsonTextFormat))

        // 2. try to parse as model
        MaybeEither {
          eventParser(jsonTextFormat)
        } match {
          case Failed(e) ⇒
            safeUnit(onEventParserError(payload, e))

            HandlerResultReject(e.getMessage)

          case Just(event) ⇒
            safeUnit(onEventParserSuccess(payload, event))

            // 3. See if we are ready to save to DB
            MaybeEither {
              preSaveAction(event)
            } match {
              case Failed(e) ⇒
                // oops. must resend this message due to unexpected result
                HandlerResultResend

              case Just(Some(handlerResult)) ⇒
                // Nope. Not ready to save.
                handlerResult

              case Just(None) ⇒
                // Yep. Ready to save
                // 4. try to save to DB
                MaybeEither {
                  saveAction(event)
                } match {
                  case Failed(e) ⇒
                    HandlerResultPanic

                  case Just(s) ⇒
                    // 4. try forward but it's OK if something bad happens here.
                    safeUnit {
                      forwardAction(s)
                    }

                    HandlerResultSuccess
                }
            }


        }
    }
  }
}