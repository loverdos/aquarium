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

/**
 * Main method for Aquarium
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
object Main extends LazyLoggable {
  private[this] def configureLogging(): Unit = {
    // Make sure AQUARIUM_HOME is configured, since it is used in logback.xml
    assert(ResourceLocator.Homes.Folders.AquariumHome.isDirectory)
  }

  def doStart(): Unit = {
    import ResourceLocator.SysEnvs

    // We have AKKA builtin, so no need to mess with pre-existing installation.
    if(SysEnvs.AKKA_HOME.value.isJust) {
      val error = new AquariumInternalError("%s is set. Please unset and restart Aquarium".format(SysEnvs.Names.AKKA_HOME))
      logger.error("%s is set".format(SysEnvs.Names.AKKA_HOME), error)
      throw error
    }

    Aquarium.Instance.start()
  }

  def main(args: Array[String]) = {
    configureLogging()

    logSeparator()
    logStarting("Aquarium")
    val ms0 = TimeHelpers.nowMillis()
    try {
      doStart()
      val ms1 = TimeHelpers.nowMillis()
      logStarted(ms0, ms1, "Aquarium")
      logSeparator()
    } catch {
      case e: Throwable ⇒
      logger.error("Aquarium not started\n%s".format(gr.grnet.aquarium.util.chainOfCausesForLogging(e, 1)), e)
      logSeparator()
      System.exit(1)
    }
  }
}