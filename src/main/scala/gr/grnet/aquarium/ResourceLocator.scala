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

import com.ckkloverdos.resource.{StreamResource, CompositeStreamResourceContext, ClassLoaderStreamResourceContext, FileStreamResourceContext}
import com.ckkloverdos.maybe.{Failed, Just, Maybe, NoVal}
import com.ckkloverdos.sys.{SysEnv, SysProp}
import java.io.File

/**
 * Used to locate configuration files.
 *
 * This code was initially in [[gr.grnet.aquarium.Configurator]].
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

object ResourceLocator {
  final val AQUARIUM_HOME = SysEnv("AQUARIUM_HOME")

  lazy val AQUARIUM_HOME_FOLDER: File = {
    AQUARIUM_HOME.value match {
      case Just(home) ⇒
        val file = new File(home)
        if(!file.isDirectory) {
          throw new Exception("%s (%s) is not a folder".format(AQUARIUM_HOME.name, home))
        }
        file.getCanonicalFile()
      case _ ⇒
        throw new Exception("%s is not set".format(AQUARIUM_HOME.name))
    }
  }

  /**
   * Current directory resource context.
   */
  private[this] final val AppBaseResourceContext = new FileStreamResourceContext(".")

  /**
   * Default config context for Aquarium distributions
   */
  private[this] final val LocalConfigResourceContext = new FileStreamResourceContext("conf")

  /**
   * The venerable /etc resource context. Applicable in Unix environments
   */
  private[this] final val SlashEtcResourceContext = new FileStreamResourceContext("/etc/aquarium")

  /**
   * Class loader resource context.
   * This has the lowest priority.
   */
  private[this] final val ClasspathBaseResourceContext = new ClassLoaderStreamResourceContext(Thread.currentThread().getContextClassLoader)

  /**
   * Use this property to override the place where aquarium configuration resides.
   *
   * The value of this property is a folder that defines the highest-priority resource context.
   */
  private[this] final val ConfBaseFolderSysProp = SysProp("aquarium.conf.base.folder")

  private[this] final val BasicResourceContext = new CompositeStreamResourceContext(
    NoVal,
    SlashEtcResourceContext,
    LocalConfigResourceContext,
    AppBaseResourceContext,
    ClasspathBaseResourceContext)

  /**
   * The resource context used in the application.
   */
  private[this] final val MasterResourceContext = {
    ConfBaseFolderSysProp.value match {
      case Just(value) ⇒
        // We have a system override for the configuration location
        new CompositeStreamResourceContext(Just(BasicResourceContext), new FileStreamResourceContext(value))
      case NoVal ⇒
        BasicResourceContext
      case Failed(e, m) ⇒
        throw new RuntimeException(m , e)
    }
  }

  def getResource(what: String): Maybe[StreamResource] =
    MasterResourceContext.getResource(what)
}
