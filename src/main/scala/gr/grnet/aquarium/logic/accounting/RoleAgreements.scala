/*
 * Copyright 2011 GRNET S.A. All rights reserved.
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

package gr.grnet.aquarium.logic.accounting

import dsl.DSLAgreement
import scala.collection.mutable.ConcurrentMap
import gr.grnet.aquarium.util.Loggable
import gr.grnet.aquarium.Configurator.{MasterConfigurator, Keys}
import io.Source
import java.io.{InputStream, File}
import java.util.regex.Pattern
import collection.JavaConversions._
import java.util.concurrent.ConcurrentHashMap

/**
 * Encapsulates mappings from user roles to Aquarium policies. The mappings
 * are used during new user registration to automatically set a policy to
 * a user according to its role.
 *
 * The configuration is read from a configuration file pointed to by the
 * main Aquarium configuration file. The
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
object RoleAgreements extends Loggable {

  lazy val mappings: ConcurrentMap[String, DSLAgreement] = {
    new ConcurrentHashMap[String, DSLAgreement]()

  }

  /**
   * Returns the agreement that matches the provided role. The search for a
   * matching agreement is done with the current version of the Aquarium
   * policy.
   */
  def agreementForRole(role : String) = mappings.get(role.toLowerCase)


  /**
   * Load and parse the mappings file
   */
  def loadMappings = synchronized {
    val config = MasterConfigurator.get(Keys.aquarium_role_agreement_map)
    val configFile = new File(config)

    def loadFromClasspath: Source = {
      getClass.getClassLoader.getResourceAsStream("roles-agreements.map") match {
        case x: InputStream =>
          logger.warn("Using default role to agreement mappings, this is " +
            "problably not what you want")
          Source.fromInputStream(x)
        case null =>
          logger.error("No valid role to agreement mappings configuration found, " +
            "Aquarium will fail")
          null
      }
    }

    val source = if (configFile.exists && configFile.isFile) {
        if (configFile.isFile)
          Source.fromFile(configFile)
        else
          logger.warn(("Configured file %s is a directory. " +
            "Trying the default one.").format(config))
        loadFromClasspath
    } else {
        logger.warn("Configured file %s for role-agreement mappings cannot " +
          "be found. Trying the default one.".format(config))
        loadFromClasspath
    }
    
    val p = Pattern.compile("^\\s*?([a-zA-Z0-9]+)\\s*=\\s*([a-zA-Z0-9]+)\\s+.*$")

    val parsed = source.getLines.foldLeft(Map[String, DSLAgreement]()) {
      (acc, l) =>
        l match {
          case x if (x.matches("^\\s*$")) => acc
          case x if (x.matches("^\\s*\\#")) => acc
          case x if (p.matcher(x).find()) =>
            // Ugly code warning
            val m = p.matcher(x)
            m.find()
            val role = m.group(1)
            val agrName = m.group(2)
            Policy.policy.findAgreement(agrName) match {
              case Some(x) => acc ++ Map(role -> x)
              case None =>
                logger.warn("No agreement with name %s".format(agrName))
                acc
            }
          case _ => acc
        }
    }

    mappings.clear()
    parsed.foreach{r => mappings.add(r._1, r._2)}
  }
}
