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

import util.Loggable
import akka.actor.Actor
import com.ckkloverdos.sys.{SysEnv, SysProp}
import com.ckkloverdos.maybe.Just
import java.io.File

/**
 * Main method for Aquarium
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object Main extends Loggable {
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

  def main(args: Array[String]) = {
    import ResourceLocator.{AQUARIUM_HOME, AQUARIUM_HOME_FOLDER}
    logger.info("Starting Aquarium from {}", AQUARIUM_HOME_FOLDER)

    val mc = Configurator.MasterConfigurator

    if(mc.hasEventsStoreFolder) {
      logger.info("{} = {}", Configurator.Keys.events_store_folder, mc.eventsStoreFolder)
    }

    val rl = ResourceLocator
    val HERE = gr.grnet.aquarium.util.justForSure(rl.getResource(".")).get.url.toExternalForm

    for {
      prop <- PropsToShow
    } {
      logger.info("{} = {}", prop.name, prop.rawValue)
    }
    logger.info("{} = {}", AQUARIUM_HOME.name, AQUARIUM_HOME_FOLDER)
    logger.info("HERE = {}", HERE)

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