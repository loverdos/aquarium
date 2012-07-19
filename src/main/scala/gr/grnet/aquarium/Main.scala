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

import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.util.LazyLoggable
import gr.grnet.aquarium.ResourceLocator._
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import com.ckkloverdos.maybe.Just
import gr.grnet.aquarium.service.event.AquariumCreatedEvent
import gr.grnet.aquarium.policy.PolicyModel

/**
 * Main method for Aquarium
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object Main extends LazyLoggable {
  private[this] def configureLogging(): Unit = {
    // Make sure AQUARIUM_HOME is configured, since it is used in logback.xml
    assert(ResourceLocator.Homes.Folders.AquariumHome.isDirectory)

    val f = LoggerFactory.getILoggerFactory
    f match {
      case context: LoggerContext ⇒
        val joran = new JoranConfigurator
        joran.setContext(context)
        context.reset()
        joran.doConfigure(ResourceLocator.Resources.LogbackXMLResource.url)
    }
  }

  private[this] def logBasicConfiguration(): Unit = {
    logger.info("Aquarium Home = %s".format(
      if(Homes.Folders.AquariumHome.isAbsolute)
        Homes.Folders.AquariumHome
      else
        "%s [=%s]".format(Homes.Folders.AquariumHome, Homes.Folders.AquariumHome.getCanonicalPath)
    ))

    for(prop ← Aquarium.PropsToShow) {
      logger.info("{} = {}", prop.name, prop.rawValue)
    }

    logger.info("CONF_HERE =  {}", HERE)
    logger.info("{} = {}", ResourceLocator.ResourceNames.AQUARIUM_PROPERTIES, ResourceLocator.Resources.AquariumPropertiesResource)
    logger.info("{} = {}", ResourceLocator.ResourceNames.LOGBACK_XML, ResourceLocator.Resources.LogbackXMLResource)
    logger.info("{} = {}", ResourceLocator.ResourceNames.POLICY_JSON, ResourceLocator.Resources.PolicyJSONResource)

    logger.info("Runtime.getRuntime.availableProcessors() => {}", Runtime.getRuntime.availableProcessors())
  }

  def main(args: Array[String]) = {
    configureLogging()

    logSeparator()
    logStarting("Aquarium")
    logBasicConfiguration()

    val ms0 = TimeHelpers.nowMillis()
    try {
      val aquarium = new AquariumBuilder(ResourceLocator.AquariumProperties).build()
      aquarium.start()

      val ms1 = TimeHelpers.nowMillis()
      logStarted(ms0, ms1, "%s", aquarium.toString)
      logSeparator()
    } catch {
      case e: Throwable ⇒
      logger.error("Aquarium not started\n%s".format(gr.grnet.aquarium.util.chainOfCausesForLogging(e, 1)), e)
      logSeparator()
      System.exit(1)
    }
  }
}