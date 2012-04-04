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

package gr.grnet.aquarium

import akka.actor.Actor
import com.ckkloverdos.sys.SysProp
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.util.StatusPrinter
import com.ckkloverdos.maybe.{NoVal, Maybe, Failed, Just}
import util.{LazyLoggable, Loggable}

/**
 * Main method for Aquarium
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object Main extends LazyLoggable {
  private[this] final val PropsToShow = List(
    SysProp.JavaVMName,
    SysProp.JavaVersion,
    SysProp.JavaHome,
    SysProp.JavaClassVersion,
    SysProp.JavaLibraryPath,
    SysProp.JavaClassPath,
    SysProp.JavaIOTmpDir,
    SysProp.UserName,
    SysProp.UserHome,
    SysProp.UserDir,
    SysProp("file.encoding")
  )

  private[this] def configureLogging(): Unit = {
    // http://logback.qos.ch/manual/joran.html
    LoggerFactory.getILoggerFactory match {
      case context: LoggerContext ⇒
        Maybe {
          val joran = new JoranConfigurator
          joran.setContext(context)
          context.reset()
          joran.doConfigure(ResourceLocator.LOGBACK_XML_FILE)
          logger.info("Logging subsystem configured from {}", ResourceLocator.LOGBACK_XML_FILE)
        } forJust {
          case _ ⇒
            StatusPrinter.printInCaseOfErrorsOrWarnings(context)
        } forFailed {
          case failed @ Failed(e) ⇒
            StatusPrinter.print(context)
            throw new AquariumException(e, "Could not configure logging from %s".format(ResourceLocator.LOGBACK_XML_FILE))
        }
      case _ ⇒
    }
  }

  def main(args: Array[String]) = {
    import ResourceLocator.{AQUARIUM_HOME, AQUARIUM_HOME_FOLDER, CONF_HERE, AKKA_HOME}

    configureLogging()

    logger.info("Starting Aquarium from {}", AQUARIUM_HOME_FOLDER)

    // We have AKKA builtin, so no need to mess with pre-existing installation.
    if(AKKA_HOME.value.isJust) {
      val error = new AquariumException("%s is set. Please unset and restart Aquarium".format(AKKA_HOME.name))
      logger.error("%s is set".format(AKKA_HOME.name), error)
      throw error
    }

    val mc = Configurator.MasterConfigurator

    mc.eventsStoreFolder match {
      case Just(folder) ⇒
        logger.info("{} = {}", Configurator.Keys.events_store_folder, folder)

      case failed @ Failed(e) ⇒
        throw e

      case _ ⇒
    }

    for {
      prop <- PropsToShow
    } {
      logger.info("{} = {}", prop.name, prop.rawValue)
    }
    logger.info("{} = {}", AQUARIUM_HOME.name, AQUARIUM_HOME_FOLDER)
    logger.info("CONF_HERE = {}", CONF_HERE)

    mc.startServices()

    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      def run = {
        logger.info("Shutting down Aquarium")
        mc.stopServices()
        Actor.registry.shutdownAll()
      }
    }))

    logger.info("Started Aquarium")
  }
}