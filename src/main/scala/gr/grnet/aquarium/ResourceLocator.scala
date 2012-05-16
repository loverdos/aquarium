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

import com.ckkloverdos.maybe.{Failed, Just, Maybe, NoVal}
import com.ckkloverdos.sys.{SysEnv, SysProp}
import java.io.File

import gr.grnet.aquarium.util.justForSure
import gr.grnet.aquarium.util.isRunningTests
import com.ckkloverdos.resource.{FileStreamResource, StreamResource, CompositeStreamResourceContext, ClassLoaderStreamResourceContext, FileStreamResourceContext}

/**
 * Locates resources.
 *
 * This code was initially in [[gr.grnet.aquarium.Configurator]].
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

object ResourceLocator {
  final object ResourceNames {
    final val CONF_FODLER               = "conf"
    final val SLASH_ETC_AQUARIUM_FOLDER = "/etc/aquarium"

    final val LOGBACK_XML         = "logback.xml"
    final val AQUARIUM_PROPERTIES = "aquarium.properties"
    final val POLICY_YAML         = "policy.yaml"
    final val ROLE_AGREEMENTS_MAP = "roles-agreements.map"
  }

  final object HomeNames {
    final val NAME_AKKA_HOME     = "AKKA_HOME"
    final val NAME_AQUARIUM_HOME = "AQUARIUM_HOME"
  }

  final object Homes {
    final val AKKA_HOME = SysEnv(HomeNames.NAME_AKKA_HOME)

    /**
     * This is normally exported from the shell script that starts Aquarium.
     *
     * TODO: Make this searchable for resources (ie put it in the resource context)
     */
    final val AQUARIUM_HOME = {
      val Home = SysEnv(HomeNames.NAME_AQUARIUM_HOME)

      // Set or update the system property of the same name
      for(value ← Home.value) {
        SysProp(HomeNames.NAME_AQUARIUM_HOME).update(value)
      }

      Home
    }

    final lazy val AQUARIUM_HOME_FOLDER: File = {
      Homes.AQUARIUM_HOME.value match {
        case Just(home) ⇒
          val file = new File(home)
          if(!file.isDirectory) {
            throw new AquariumInternalError("%s (%s) is not a folder".format(Homes.AQUARIUM_HOME.name, home))
          }
          file.getCanonicalFile()

        case _ ⇒
          if(isRunningTests()) {
            new File(".")
          } else {
            throw new AquariumInternalError("%s is not set".format(Homes.AQUARIUM_HOME.name))
          }
      }
    }

  }

  final object SysPropsNames {
    final val NameAquariumPropertiesPath = "aquarium.properties.path"
    final val NameAquariumConfFolder     = "aquarium.conf.folder"
  }

  final object SysProps {
    /**
     * Use this property to override the place of aquarium.properties.
     * If this is set, then it override any other way to specify the aquarium.properties location.
     */
    final lazy val AquariumPropertiesPath = SysProp(SysPropsNames.NameAquariumPropertiesPath)

    /**
     * Use this property to override the place where aquarium configuration resides.
     *
     * The value of this property is a folder that defines the highest-priority resource context.
     */
    final lazy val AquariumConfFolder = SysProp(SysPropsNames.NameAquariumConfFolder)
  }

  final object ResourceContexts {
    /**
     * AQUARIUM_HOME/conf resource context.
     */
    private[this] final lazy val HomeConfResourceContext = new FileStreamResourceContext(AQUARIUM_HOME_CONF_FOLDER)

    /**
     * The venerable /etc resource context. Applicable in Unix environments
     */
    private[this] final lazy val SlashEtcResourceContext = new FileStreamResourceContext(ResourceNames.SLASH_ETC_AQUARIUM_FOLDER)

    /**
     * Class loader resource context.
     * This has the lowest priority.
     */
    private[this] final lazy val ClasspathBaseResourceContext = new ClassLoaderStreamResourceContext(
      Thread.currentThread().getContextClassLoader)

    private[this] final lazy val BasicResourceContext = new CompositeStreamResourceContext(
      NoVal,
      SlashEtcResourceContext,
      HomeConfResourceContext,
      ClasspathBaseResourceContext)

    /**
     * The resource context used in the application.
     */
    final lazy val MasterResourceContext = {
      SysProps.AquariumConfFolder.value match {
        case Just(value) ⇒
          // We have a system override for the configuration location
          new CompositeStreamResourceContext(
            Just(BasicResourceContext),
            new FileStreamResourceContext(value))

        case NoVal ⇒
          BasicResourceContext

        case Failed(e) ⇒
          throw new AquariumInternalError(e)
      }
    }
  }

  final lazy val AQUARIUM_HOME_CONF_FOLDER = new File(Homes.AQUARIUM_HOME_FOLDER, ResourceNames.CONF_FODLER)

  final lazy val LOGBACK_XML_FILE = new File(AQUARIUM_HOME_CONF_FOLDER, ResourceNames.LOGBACK_XML)

  /**
   * This exists in order to have a feeling of where we are.
   */
  final lazy val CONF_HERE = justForSure(getResource(".")).get.url.toExternalForm

  final object Resources {
    final lazy val AquariumPropertiesResource = {
      ResourceLocator.SysProps.AquariumPropertiesPath.value match {
        case Just(aquariumPropertiesPath) ⇒
          // If we have a command-line override, prefer that
          new FileStreamResource(new File(aquariumPropertiesPath))

        case Failed(e) ⇒
          // On error, fail
          throw new AquariumInternalError(
            "Could not find %s=%s".format(
              ResourceLocator.SysPropsNames.NameAquariumPropertiesPath,
              ResourceLocator.SysProps.AquariumPropertiesPath),
            e)

        case NoVal ⇒
          // Otherwise try other locations
          val aquariumPropertiesRCM = ResourceLocator getResource ResourceLocator.ResourceNames.AQUARIUM_PROPERTIES
          aquariumPropertiesRCM match {
            case Just(aquariumProperties) ⇒
              aquariumProperties

            case NoVal ⇒
              // No luck
              throw new AquariumInternalError(
                "Could not find %s".format(ResourceLocator.ResourceNames.AQUARIUM_PROPERTIES))

            case Failed(e) ⇒
              // Bad luck
              throw new AquariumInternalError(
                "Could not find %s".format(ResourceLocator.ResourceNames.AQUARIUM_PROPERTIES), e)
          }
      }
    }
  }


  def getResource(what: String): Maybe[StreamResource] = {
    ResourceContexts.MasterResourceContext.getResource(what)
  }
}
