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

import com.ckkloverdos.maybe.{Maybe, NoVal}
import com.ckkloverdos.sys.{SysEnv, SysProp}
import java.io.File

import gr.grnet.aquarium.util.justForSure
import com.ckkloverdos.resource.{FileStreamResource, StreamResource, CompositeStreamResourceContext, ClassLoaderStreamResourceContext, FileStreamResourceContext}
import com.ckkloverdos.props.Props
import com.ckkloverdos.maybe.Just
import com.ckkloverdos.maybe.Failed
import com.ckkloverdos.convert.Converters
import gr.grnet.aquarium.converter.{JsonTextFormat, StdConverters}
import gr.grnet.aquarium.policy.StdPolicy
import gr.grnet.aquarium.message.avro.AvroHelpers
import gr.grnet.aquarium.message.avro.gen.PolicyMsg

/**
 * Locates resources.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

object ResourceLocator {
  final object ResourceNames {
    final val CONF_FODLER               = "conf"
    final val SLASH_ETC_AQUARIUM_FOLDER = "/etc/aquarium"

    final val LOGBACK_XML         = "logback.xml"
    final val AQUARIUM_PROPERTIES = "aquarium.properties"
    final val POLICY_JSON         = "policy.json"
  }

  final object Homes {
    final object Names {
      final val AKKA_HOME     = "AKKA_HOME"
//      final val AQUARIUM_HOME = "AQUARIUM_HOME"
    }

    final object Folders {
      private[this] def checkFolder(name: String, file: File): File = {
        if(!file.isDirectory) {
          throw new AquariumInternalError(
            "%s=%s is not a folder".format(
              name,
              if(file.isAbsolute)
                file.getAbsolutePath
              else
                "%s [=%s]".format(file, file.getCanonicalFile)
            )
          )
        }
        file
      }

      /**
       * This is normally exported from the shell script (AQUARIUM_HOME) that starts Aquarium or given in the command
       * line (aquarium.home).
       *
       * TODO: Make this searchable for resources (ie put it in the resource context)
       */
      final lazy val AquariumHome = {
        SysProps.AquariumHome.value match {
          case Just(aquariumHome) ⇒
            checkFolder(SysProps.Names.AquariumHome, new File(aquariumHome))

          case Failed(e) ⇒
            throw new AquariumInternalError("Error regarding %s".format(SysProps.Names.AquariumHome), e)

          case NoVal ⇒
            SysEnvs.AQUARIUM_HOME.value match {
              case Just(aquarium_home) ⇒
                val folder = new File(aquarium_home)
                checkFolder(SysEnvs.Names.AQUARIUM_HOME, folder)
                SysProps.AquariumHome.update(folder.getPath) // needed for logback configuration
                folder

              case Failed(e) ⇒
                throw new AquariumInternalError("Error regarding %s".format(SysEnvs.Names.AQUARIUM_HOME), e)

              case NoVal ⇒
                val folder = new File(".")
                SysProps.AquariumHome.update(folder.getPath) // needed for logback configuration
                folder
            }
        }
      }
    }
  }

  final object SysEnvs {
    final object Names {
      final val AKKA_HOME     = Homes.Names.AKKA_HOME
      final val AQUARIUM_HOME = "AQUARIUM_HOME"
    }

    final val AKKA_HOME     = SysEnv(Names.AKKA_HOME)
    final val AQUARIUM_HOME = SysEnv(Names.AQUARIUM_HOME)
  }

  final object SysProps {
    final object Names {
      final val AquariumHome           = "aquarium.home"
      final val AquariumPropertiesPath = "aquarium.properties.path"
      final val AquariumConfFolder     = "aquarium.conf.folder"
    }

    final val AquariumHome = SysProp(Names.AquariumHome)

    /**
     * Use this property to override the place of aquarium.properties.
     * If this is set, then it override any other way to specify the aquarium.properties location.
     */
    final val AquariumPropertiesPath = SysProp(Names.AquariumPropertiesPath)

    /**
     * Use this property to override the place where aquarium configuration resides.
     *
     * The value of this property is a folder that defines the highest-priority resource context.
     */
    final val AquariumConfFolder = SysProp(Names.AquariumConfFolder)
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

  final lazy val AQUARIUM_HOME_CONF_FOLDER = new File(Homes.Folders.AquariumHome, ResourceNames.CONF_FODLER)

  /**
   * This exists in order to have a feeling of where we are.
   */
  final lazy val HERE = justForSure(getResource(".")).get.url.toExternalForm

  final object Resources {
    final lazy val AquariumPropertiesResource = {
      ResourceLocator.SysProps.AquariumPropertiesPath.value match {
        case Just(aquariumPropertiesPath) ⇒
          // If we have a command-line override, prefer that
          new FileStreamResource(new File(aquariumPropertiesPath))

        case Failed(e) ⇒
          // On error, fail
          throw new AquariumInternalError(
            "Could not find %s".format(ResourceLocator.SysProps.Names.AquariumPropertiesPath), e)

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

    final lazy val LogbackXMLResource = {
      getResource(ResourceNames.LOGBACK_XML) match {
        case Just(logbackXML) ⇒
          logbackXML

        case NoVal ⇒
          throw new AquariumInternalError(
            "Could not find %s".format(ResourceLocator.ResourceNames.LOGBACK_XML))

        case Failed(e) ⇒
          throw new AquariumInternalError(
            "Could not find %s".format(ResourceLocator.ResourceNames.LOGBACK_XML), e)

      }
    }

    final lazy val PolicyJSONResource = {
      ResourceLocator.getResource(ResourceLocator.ResourceNames.POLICY_JSON) match {
        case Just(policyJSON) ⇒
          policyJSON

        case NoVal ⇒
          throw new AquariumInternalError(
            "Could not find %s".format(ResourceLocator.ResourceNames.POLICY_JSON))

        case Failed(e) ⇒
          throw new AquariumInternalError(
            "Could not find %s".format(ResourceLocator.ResourceNames.POLICY_JSON), e)
      }
    }
  }

  final lazy val AquariumProperties = {
    implicit val DefaultConverters = Converters.DefaultConverters
    val maybeProps = Props(Resources.AquariumPropertiesResource)
    maybeProps match {
      case Just(props) ⇒
        props

      case NoVal ⇒
        throw new AquariumInternalError(
          "Could not load %s from %s".format(
            ResourceLocator.ResourceNames.AQUARIUM_PROPERTIES,
            Resources.AquariumPropertiesResource))


      case Failed(e) ⇒
        throw new AquariumInternalError(
          "Could not load %s from %s".format(
            ResourceLocator.ResourceNames.AQUARIUM_PROPERTIES,
            Resources.AquariumPropertiesResource),
          e)
    }
  }

  final lazy val DefaultPolicyMsg = {
    val maybePolicyJSON = Resources.PolicyJSONResource.stringContent
    maybePolicyJSON match {
      case NoVal ⇒
        throw new AquariumInternalError(
          "Could not load %s from %s".format(
            ResourceLocator.ResourceNames.POLICY_JSON,
            Resources.PolicyJSONResource))

      case Failed(e) ⇒
        throw new AquariumInternalError(e,
          "Could not load %s from %s".format(
            ResourceLocator.ResourceNames.POLICY_JSON,
            Resources.PolicyJSONResource))

      case Just(jsonString) ⇒
        AvroHelpers.specificRecordOfJsonString(jsonString, new PolicyMsg)
    }
  }


  def getResource(what: String): Maybe[StreamResource] = {
    ResourceContexts.MasterResourceContext.getResource(what)
  }
}
