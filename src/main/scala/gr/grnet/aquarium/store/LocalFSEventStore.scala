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

package gr.grnet.aquarium.store

import gr.grnet.aquarium.Configurator
import com.ckkloverdos.maybe.Maybe
import gr.grnet.aquarium.util.makeBytes
import gr.grnet.aquarium.util.date.MutableDateCalc
import java.io.{FileOutputStream, File}
import gr.grnet.aquarium.logic.events.{UserEvent, ResourceEvent}

/**
 * This is used whenever the property `events.store.folder` is setup in aquarium configuration.
 *
 * The public methods guarantee they will not propagate any failure.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

object LocalFSEventStore {
  private[this] def writeToFile(file: File, data: Array[Byte]): Unit = {
    val out = new FileOutputStream(file)
    out.write(data)
    out.flush()
    out.close()
  }

  private[this] def writeToFile(file: File, data: String): Unit = {
    writeToFile(file, makeBytes(data))
  }

  def storeResourceEvent(mc: Configurator, event: ResourceEvent, initialPayload: Array[Byte]): Maybe[Unit] = Maybe {
    if(mc.hasEventsStoreFolder) {
      val occurredString = new MutableDateCalc(event.occurredMillis).toYYYYMMDDHHMMSS
      val root = mc.eventsStoreFolder
      val rcEvents = new File(root, "rcevents")
      val parsed = event ne null

      // We save two files. One containing the initial payload and one containing the transformed object.
      val initialPayloadFile = new File(rcEvents, "rc-%s.raw%s".format(occurredString, if(!parsed) "x" else ""))
      Maybe { writeToFile(initialPayloadFile, initialPayload) }

      if(parsed) {
        val parsedJsonFile = new File(
          rcEvents,
          "rc-%s-[%s]-[%s]-[%s]-[%s].json".format(
            occurredString,
            event.id,
            event.userId,
            event.resource,
            event.instanceId))

        Maybe { writeToFile(parsedJsonFile, event.toJson) }
      }
    }
  }

  def storeUserEvent(mc: Configurator, event: UserEvent, initialPayload: Array[Byte]): Maybe[Unit] = Maybe {
    if(mc.hasEventsStoreFolder) {
      val occurredString = new MutableDateCalc(event.occurredMillis).toYYYYMMDDHHMMSS
      val root = mc.eventsStoreFolder
      val imEvents = new File(root, "imevents")
      val parsed = event ne null

     // We save two files. One containing the initial payload and one containing the transformed object.
      val initialPayloadFile = new File(imEvents, "im-%s.raw%s".format(occurredString, if(!parsed) "x" else ""))
      Maybe { writeToFile(initialPayloadFile, initialPayload) }

      if(parsed) {
        val parsedJsonFile = new File(imEvents, "im-%s-[%s]-[%s].json".format(occurredString, event.id, event.userID))
        Maybe { writeToFile(parsedJsonFile, event.toJson) }
      }
    }
  }
}
