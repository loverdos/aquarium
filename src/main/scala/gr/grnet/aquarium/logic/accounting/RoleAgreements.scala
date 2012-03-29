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

package gr.grnet.aquarium.logic.accounting

import dsl.DSLAgreement
import gr.grnet.aquarium.util.Loggable
import gr.grnet.aquarium.Configurator
import gr.grnet.aquarium.Configurator.{MasterConfigurator, Keys}
import io.Source
import java.io.{InputStream, File}
import java.util.regex.Pattern

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

  private var mappings: Map[String, DSLAgreement] = loadMappings

  /**
   * Returns the agreement that matches the provided role. The search for a
   * matching agreement is done with the current version of the Aquarium
   * policy.
   */
  def agreementForRole(role : String) = mappings.get(role.toLowerCase) match {
    case Some(x) => x
    case None => mappings.get("*").getOrElse(
      throw new RuntimeException("Cannot find agreement for default role *"))
  }

  /**
   * Trigger reloading of the mappings file.
   */
  def reloadMappings = mappings = loadMappings

  /**
   * Load and parse the mappings file
   */
  private[logic] def loadMappings = synchronized {
    val config = MasterConfigurator.get(Keys.aquarium_role_agreement_map)
    val configFile = MasterConfigurator.findConfigFile(
      Configurator.RolesAgreementsName, Keys.aquarium_role_agreement_map,
      Configurator.RolesAgreementsName)

    def loadFromClasspath: Source = {
      getClass.getClassLoader.getResourceAsStream(Configurator.RolesAgreementsName) match {
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
        else {
          logger.warn(("Configured file %s is a directory. " +
            "Trying the default one.").format(config))
          loadFromClasspath
        }
    } else {
        logger.warn("Configured file %s for role-agreement mappings cannot " +
          "be found. Trying the default one.".format(config))
        loadFromClasspath
    }

    parseMappings(source)
  }

  def parseMappings(src: Source) = {
    val p = Pattern.compile("^\\s*([\\*a-zA-Z0-9-_]+)\\s*=\\s*([a-zA-Z0-9-_]+).*$")

    val mappings = src.getLines.foldLeft(Map[String, DSLAgreement]()) {
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
    if (!mappings.keysIterator.contains("*"))
      throw new RuntimeException("Cannot find agreement for default role *")
    mappings
  }
}
