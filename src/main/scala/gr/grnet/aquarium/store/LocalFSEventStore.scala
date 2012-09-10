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

import gr.grnet.aquarium.Aquarium
import gr.grnet.aquarium.message.avro.gen.{IMEventMsg, ResourceEventMsg}
import gr.grnet.aquarium.util.date.{TimeHelpers, MutableDateCalc}
import gr.grnet.aquarium.util.{Loggable, stringOfStackTrace, makeBytes, UTF_8_Charset}
import java.io.{FileOutputStream, File}

/**
 * This is used whenever the property `events.store.folder` is setup in aquarium configuration.
 *
 * This is mainly a debugging aid. You normally want to disable it in a production environment.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

object LocalFSEventStore extends Loggable {
  private[this] final val NewLine  = makeBytes("\n", UTF_8_Charset) // super-fluous!

  private[this] def writeToFile(
      file: File,
      dataHeader: String,
      data: Array[Byte],
      dataFooter: String,
      appendString: Option[String] = None
  ): Unit = {

    val out = new FileOutputStream(file)

    out.write(makeBytes(dataHeader, UTF_8_Charset))
    out.write(data)
    out.write(makeBytes(dataFooter, UTF_8_Charset))

    appendString match {
      case Some(s) ⇒
        out.write(NewLine)
        out.write(makeBytes(s, UTF_8_Charset))
      case None ⇒
    }

    out.flush()
    out.close()

    logger.debug("Wrote to file {}", file.getCanonicalPath)
  }

  private[this] def dateTagForFolder(): String = {
    new MutableDateCalc(TimeHelpers.nowMillis()).toYYYYMMDD
  }

  private[this] def createResourceEventsFolder(root: File): File = {
    val folder0 = new File(root, "rc")
    val folder = new File(folder0, "rc-%s".format(dateTagForFolder()))
    folder.mkdirs()
    folder
  }

  private[this] def createIMEventsFolder(root: File): File = {
    val folder0 = new File(root, "im")
    val folder = new File(folder0, "im-%s".format(dateTagForFolder()))
    folder.mkdirs()
    folder
  }

  private[this] def writeJson(
      tag: String,
      folder: File,
      jsonPayload: Array[Byte],
      occurredString: String,
      extraName: Option[String],
      isParsed: Boolean,
      appendString: Option[String]
  ): Unit = {

    val file = new File(
      folder,
      "%s-%s%s.%s.json".format(
        tag,
        occurredString,
        extraName match {
          case Some(s) ⇒ "-" + s
          case None    ⇒ ""
        },
        if(isParsed) "p" else "u"
      ))

    val dataHeader = "// %s bytes of payload\n".format(jsonPayload.length)
    val dataFooter = "\n" + dataHeader

    writeToFile(
      file,
      dataHeader,
      jsonPayload,
      dataFooter,
      appendString)
  }

  def storeUnparsedResourceEvent(aquarium: Aquarium, initialPayload: Array[Byte], exception: Throwable): Unit = {
    for(root <- aquarium.eventsStoreFolder) {
      val occurredMDC = new MutableDateCalc(TimeHelpers.nowMillis())
      val occurredString = occurredMDC.toFilename_YYYYMMDDHHMMSSSSS
      val rcEventsFolder = createResourceEventsFolder(root)
      val trace = stringOfStackTrace(exception)

      writeJson("rc", rcEventsFolder, initialPayload, occurredString, None, false, Some(trace))
    }
  }

  def storeResourceEvent(aquarium: Aquarium, event: ResourceEventMsg, initialPayload: Array[Byte]): Unit = {
    if(!aquarium.saveResourceEventsToEventsStoreFolder) {
      return
    }

    require(event ne null, "Resource event must be not null")

    for(root <- aquarium.eventsStoreFolder) {
      val occurredMDC = new MutableDateCalc(event.getOccurredMillis)
      val occurredString = occurredMDC.toFilename_YYYYMMDDHHMMSSSSS
      val rcEventsFolder = createResourceEventsFolder(root)

      // Store parsed file
      writeJson(
        "rc",
        rcEventsFolder,
        initialPayload,
        occurredString,
        Some("[%s]-[%s]-[%s]-[%s]".format(
          event.getOriginalID,
          event.getUserID,
          event.getResource,
          event.getInstanceID)),
        true,
        None
      )
    }
  }

  def storeUnparsedIMEvent(aquarium: Aquarium, initialPayload: Array[Byte], exception: Throwable): Unit = {
    for(root <- aquarium.eventsStoreFolder) {
      val occurredMDC = new MutableDateCalc(TimeHelpers.nowMillis())
      val occurredString = occurredMDC.toFilename_YYYYMMDDHHMMSSSSS
      val imEventsFolder = createIMEventsFolder(root)
      val trace = stringOfStackTrace(exception)

      writeJson("im", imEventsFolder, initialPayload, occurredString, None, false, Some(trace))
    }
  }

  def storeIMEvent(aquarium: Aquarium, event: IMEventMsg, initialPayload: Array[Byte]): Unit = {
    if(!aquarium.saveIMEventsToEventsStoreFolder) {
      return
    }

    require(event ne null, "IM event must be not null")

    for(root <- aquarium.eventsStoreFolder) {
      val occurredMDC = new MutableDateCalc(event.getOccurredMillis)
      val occurredString = occurredMDC.toFilename_YYYYMMDDHHMMSSSSS
      val imEventsFolder = createIMEventsFolder(root)

      writeJson(
        "im",
        imEventsFolder,
        initialPayload,
        occurredString,
        Some("[%s]-[%s]".format(event.getOriginalID, event.getUserID)),
        true,
        None
      )
    }
  }
}
