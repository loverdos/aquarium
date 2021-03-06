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

import com.ckkloverdos.env.Env
import com.ckkloverdos.key.{BooleanKey, TypedKey}
import com.ckkloverdos.maybe.{Failed, MaybeEither, Just, NoVal}
import com.ckkloverdos.props.Props
import gr.grnet.aquarium.converter.StdConverters
import gr.grnet.aquarium.message.avro.gen.PolicyMsg
import gr.grnet.aquarium.service.EventBusService
import gr.grnet.aquarium.service.event.AquariumCreatedEvent
import gr.grnet.aquarium.util.Loggable
import java.io.File

/**
 * Create a tailored Aquarium.
 *
 * Thread-unsafe.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

final class AquariumBuilder(
    val originalProps: Props,
    val defaultPolicyMsg: PolicyMsg
) extends Loggable {

  if(originalProps eq null) {
    throw new AquariumInternalError("props is null")
  }

  import Aquarium.EnvKeys

  private[this] var _env = Env()
  // This is special
  private[this] val eventBus = new EventBusService

  @volatile
  private[this] var _aquarium: Aquarium = _

  @throws(classOf[AquariumInternalError])
  private def propsGetEx(key: String): String = {
    try {
     originalProps.getEx(key)
    } catch {
      case e: Exception ⇒
        throw new AquariumInternalError("Could not locate %s in Aquarium properties".format(key))
    }
  }

  @throws(classOf[AquariumInternalError])
  private def envGetEx[T: Manifest](key: TypedKey[T]): T = {
    try {
     _env.getEx(key)
    } catch {
      case e: Exception ⇒
        throw new AquariumInternalError("Could not locate %s in Aquarium environment".format(key))
    }
  }

  def update[T: Manifest](keyvalue: (TypedKey[T], T)): this.type = {
    assert(keyvalue ne null, "keyvalue ne null")

    _env += keyvalue
    this
  }

  def update[T : Manifest](key: TypedKey[T], value: T): this.type = {
    assert(key ne null, "key ne null")

    this update (key -> value)
  }

  private[this] def newInstanceFromKey[T <: AnyRef : Manifest](envKey: TypedKey[T]): T = {
    newInstance(envKey.keyType, envKey.keyType.erasure.getName)
  }

  /**
   * Reflectively provide a new instance of a class and configure it appropriately.
   */
  private[this] def newInstance[C <: AnyRef](manifest: Manifest[C], className: String): C = {
    val defaultClassLoader = Thread.currentThread().getContextClassLoader
    val instanceM = MaybeEither(defaultClassLoader.loadClass(className).newInstance().asInstanceOf[C])
    instanceM match {
      case Just(instance) ⇒
        eventBus.addSubscriber(instance)

        instance match {
          case configurable: Configurable if (originalProps ne null) ⇒
            val localProps = configurable.propertyPrefix match {
              case somePrefix @ Some(prefix) ⇒
                if(prefix.length == 0) {
                  logger.warn(
                    "Property prefix for %s is %s. Consider using None".format(instance, somePrefix))
                }

                originalProps.subsetForKeyPrefix(prefix)

              case None ⇒
                originalProps
            }

            logger.debug("Configuring {} with props (prefix={})", configurable.getClass.getName, configurable.propertyPrefix)
            MaybeEither(configurable configure localProps) match {
              case Just(_) ⇒
                logger.info("Configured {} with props (prefix={})", configurable.getClass.getName, configurable.propertyPrefix)
                instance

              case Failed(e) ⇒
                throw new AquariumInternalError("Could not configure instance of %s".format(className), e)
            }

          case _ ⇒
            instance
        }

      case Failed(e) ⇒
        throw new AquariumInternalError("Could not instantiate %s".format(className), e)
    }
  }

  private[this] def checkStoreProviderOverride: Unit = {
    val envKey = EnvKeys.storeProvider
    if(_env.contains(envKey)) {
      return
    }

    if(originalProps eq null) {
      throw new AquariumInternalError("Cannot locate store provider, since no properties have been defined")
    }

    val propName = envKey.name
    originalProps.get(propName) match {
      case Just(storeProviderClassName) ⇒
        update(envKey, newInstance(envKey.keyType, storeProviderClassName))

      case NoVal ⇒
        throw new AquariumInternalError("No store provider is given in properties")

      case Failed(e) ⇒
        throw new AquariumInternalError(e, "While obtaining value for key %s in properties".format(propName))
    }
  }

  private[this] def checkEventsStoreFolderOverride: Unit = {
    val propName = EnvKeys.eventsStoreFolder.name

    _env.get(EnvKeys.eventsStoreFolder) match {
      case Just(storeFolderOption) ⇒
        // Some value has been set, even a None, so do nothing more
        logger.info("{} = {}", propName, storeFolderOption)

      case Failed(e) ⇒
        throw new AquariumInternalError(e, "While obtaining value for env key %s".format(propName))

      case NoVal ⇒
        if(originalProps eq null) {
          update(EnvKeys.eventsStoreFolder, None)
          return
        }

        // load from props
        for(folderName ← originalProps.get(propName)) {
          logger.info("{} = {}", propName, folderName)

          update(EnvKeys.eventsStoreFolder, Some(new File(folderName)))
        }
    }
  }

  private[this] def checkEventsStoreFolderExistence: Unit = {
    val propName = EnvKeys.eventsStoreFolder.name
    for(folder ← this.envGetEx(EnvKeys.eventsStoreFolder)) {
      val canonicalFolder = {
        if(folder.isAbsolute) {
          folder.getCanonicalFile
        } else {
          logger.info("{} is not absolute, making it relative to Aquarium Home", propName)
          new File(ResourceLocator.Homes.Folders.AquariumHome, folder.getPath).getCanonicalFile
        }
      }

      val canonicalPath = canonicalFolder.getCanonicalPath

      if(canonicalFolder.exists() && !canonicalFolder.isDirectory) {
        throw new AquariumInternalError("%s = %s is not a folder".format(propName, canonicalFolder))
      }

      // Now, events folder must be outside AQUARIUM_HOME, since AQUARIUM_HOME can be wiped out for an upgrade but
      // we still want to keep the events.
      val ahCanonicalPath = ResourceLocator.Homes.Folders.AquariumHome.getCanonicalPath
      if(canonicalPath.startsWith(ahCanonicalPath)) {
        throw new AquariumInternalError(
          "%s = %s is under Aquarium Home = %s".format(
            propName,
            canonicalFolder,
            ahCanonicalPath
          ))
      }

      canonicalFolder.mkdirs()

      update(EnvKeys.eventsStoreFolder, Some(canonicalFolder))
    }
  }

  private[this] def checkEventsStoreFolderVariablesOverrides: Unit = {
    def checkVar(envKey: BooleanKey): Unit = {
      if(!_env.contains(envKey)) {
        val propName = envKey.name
        originalProps.getBoolean(propName) match {
          case Just(propValue) ⇒
            update(envKey, propValue)

          case NoVal ⇒
            update(envKey, false)

          case Failed(e) ⇒
            throw new AquariumInternalError(e, "While obtaining value for key %s in properties".format(propName))
        }
      }
    }

    checkVar(EnvKeys.eventsStoreSaveRCEvents)
    checkVar(EnvKeys.eventsStoreSaveIMEvents)
  }

  private[this] def checkRestServiceOverride: Unit = {
    checkNoPropsOverride(EnvKeys.restService) { envKey ⇒
      val envKey    = EnvKeys.restService
      val propName  = envKey.name
      val propValue = propsGetEx(propName)

      newInstance(envKey.keyType, propValue)
    }
  }

  private[this] def checkNoPropsOverride[T <: AnyRef : Manifest](envKey: TypedKey[T])
                                                                (f: TypedKey[T] ⇒ T): Unit = {

    if(_env.contains(envKey)) {
      return
    }

    update(envKey, f(envKey))
  }

  private[this] def checkPropsOverride[T: Manifest](envKey: TypedKey[T])(f: (TypedKey[T], String) ⇒ T): Unit = {
    if(_env.contains(envKey)) {
      return
    }

    val propName = envKey.name
    originalProps.get(propName) match {
      case Just(propValue) ⇒
        update(envKey, f(envKey, propValue))

      case NoVal ⇒
        throw new AquariumInternalError("No value for key %s in properties".format(propName))

      case Failed(e) ⇒
        throw new AquariumInternalError(e, "While obtaining value for key %s in properties".format(propName))
    }
  }

  private[this] def checkOptionalPropsOverride[T: Manifest]
      (envKey: TypedKey[Option[T]])
      (f: (TypedKey[Option[T]], String) ⇒ Option[T]): Unit = {

    if(_env.contains(envKey)) {
      return
    }

    val propName = envKey.name
    originalProps.get(propName) match {
      case Just(propValue) ⇒
        update(envKey, f(envKey, propValue))

      case NoVal ⇒
        update(envKey, None)

      case Failed(e) ⇒
        throw new AquariumInternalError(e, "While obtaining value for key %s in properties".format(propName))
    }
  }

  /**
   * Builds a fresh instance of [[gr.grnet.aquarium.Aquarium]]. The services needed by [[gr.grnet.aquarium.Aquarium]]
   * are configured but not started yet.
   *
   * This is a very delicate procedure, so always change with care.
   *
   * @return A fresh instance of [[gr.grnet.aquarium.Aquarium]].
   */
  def build(): Aquarium = {
    if(this._aquarium ne null) {
      return this._aquarium
    }

    checkPropsOverride(EnvKeys.version) { (envKey, propValue) ⇒ propValue }

    checkNoPropsOverride(EnvKeys.eventBus) { _ ⇒ eventBus }

    checkNoPropsOverride(EnvKeys.originalProps) { _ ⇒ originalProps }

    checkNoPropsOverride(EnvKeys.defaultClassLoader) { _ ⇒  Thread.currentThread().getContextClassLoader }

    checkNoPropsOverride(EnvKeys.converters) { _ ⇒ StdConverters.AllConverters }

    checkStoreProviderOverride

    checkEventsStoreFolderOverride
    checkEventsStoreFolderExistence
    checkEventsStoreFolderVariablesOverrides

    checkRestServiceOverride

    checkNoPropsOverride(EnvKeys.timerService) { newInstanceFromKey(_) }

    checkNoPropsOverride(EnvKeys.chargingService) { newInstanceFromKey(_) }

    checkNoPropsOverride(EnvKeys.akkaService) { newInstanceFromKey(_) }

    checkNoPropsOverride(EnvKeys.rabbitMQService) { newInstanceFromKey(_) }

    checkNoPropsOverride(EnvKeys.rabbitMQProducer) { newInstanceFromKey(_) }

    checkNoPropsOverride(EnvKeys.storeWatcherService) { newInstanceFromKey(_) }

    checkPropsOverride(EnvKeys.userStateTimestampThreshold) { (envKey, propValue) ⇒
      propValue.toLong
    }

    checkPropsOverride(EnvKeys.restPort) { (envKey, propValue) ⇒
      propValue.toInt
    }

    checkPropsOverride(EnvKeys.restShutdownTimeoutMillis) { (envKey, propValue) ⇒
        propValue.toLong
    }

    checkOptionalPropsOverride(EnvKeys.adminCookie) { (envKey, propValue) ⇒
      Some(propValue)
    }

    update(EnvKeys.defaultPolicyMsg, defaultPolicyMsg)

    this._aquarium = new Aquarium(_env)

    this._aquarium.eventBus.syncPost(AquariumCreatedEvent(this._aquarium))

    this._aquarium
  }
}
