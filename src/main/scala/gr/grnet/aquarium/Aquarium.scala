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
import com.ckkloverdos.key.{IntKey, StringKey, LongKey, TypedKeySkeleton, TypedKey, BooleanKey}
import com.ckkloverdos.props.Props
import gr.grnet.aquarium.store.{PolicyStore, UserStateStore, IMEventStore, ResourceEventStore, StoreProvider}
import java.io.File
import gr.grnet.aquarium.util.{Loggable, Lifecycle}
import gr.grnet.aquarium.service.{RoleableActorProviderService, StoreWatcherService, RabbitMQService, TimerService, EventBusService, AkkaService}
import com.ckkloverdos.convert.Converters
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.{LoggerFactory, Logger}
import gr.grnet.aquarium.logic.accounting.algorithm.CostPolicyAlgorithmCompiler
import gr.grnet.aquarium.computation.UserStateComputations
import com.ckkloverdos.maybe._
import gr.grnet.aquarium.ResourceLocator._
import gr.grnet.aquarium.logic.accounting.dsl.DSLResourcesMap
import gr.grnet.aquarium.logic.accounting.Policy
import com.ckkloverdos.sys.SysProp

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

final class Aquarium(env: Env) extends Lifecycle with Loggable {
  import Aquarium.EnvKeys

  private[this] val _isStopping = new AtomicBoolean(false)

  override def toString = "%s/v%s".format(getClass.getName, version)

  def isStopping() = _isStopping.get()

  @inline
  def getClientLogger(client: AnyRef): Logger = {
    client match {
      case null ⇒
        this.logger

      case _ ⇒
        LoggerFactory.getLogger(client.getClass)
    }
  }

  def debug(client: AnyRef, fmt: String, args: Any*) = {
    getClientLogger(client).debug(fmt.format(args: _*))
  }

  def info(client: AnyRef, fmt: String, args: Any*) = {
    getClientLogger(client).info(fmt.format(args: _*))
  }

  def warn(client: AnyRef, fmt: String, args: Any*) = {
    getClientLogger(client).warn(fmt.format(args: _*))
  }

  @throws(classOf[AquariumInternalError])
  def apply[T: Manifest](key: TypedKey[T]): T = {
    try {
     env.getEx(key)
    } catch {
      case e: Exception ⇒
        throw new AquariumInternalError("Could not locate %s in Aquarium environment".format(key))
    }
  }

  private[this] lazy val _allServices = Aquarium.ServiceKeys.map(this(_))

  private[this] def startServices(): Unit = {
    for(service ← _allServices) {
      logStartingF(service.toString) {
        service.start()
      } {}
    }
  }

  private[this] def stopServices(): Unit = {
    val services = _allServices.reverse

    for(service ← services) {
      logStoppingF(service.toString) {
        safeUnit(service.stop())
      } {}
    }
  }

  private[this] def showBasicConfiguration(): Unit = {
    logger.info("Aquarium Home = %s".format(
      if(Homes.Folders.AquariumHome.isAbsolute)
        Homes.Folders.AquariumHome
      else
        "%s [=%s]".format(Homes.Folders.AquariumHome, Homes.Folders.AquariumHome.getCanonicalPath)
    ))

    for(folder ← this.eventsStoreFolder) {
      logger.info("{} = {}", EnvKeys.eventsStoreFolder.name, folder)
    }
    this.eventsStoreFolder.throwMe // on error

    for(prop ← Aquarium.PropsToShow) {
      logger.info("{} = {}", prop.name, prop.rawValue)
    }

    logger.info("CONF_HERE =  {}", HERE)
    logger.info("{} = {}", ResourceLocator.ResourceNames.AQUARIUM_PROPERTIES, ResourceLocator.Resources.AquariumPropertiesResource)
    logger.info("{} = {}", ResourceLocator.ResourceNames.LOGBACK_XML, ResourceLocator.Resources.LogbackXMLResource)
    logger.info("{} = {}", ResourceLocator.ResourceNames.POLICY_YAML, ResourceLocator.Resources.PolicyYAMLResource)
  }

  private[this] def addShutdownHooks(): Unit = {
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      def run = {
        if(!_isStopping.get()) {
          logStoppingF("Aquarium") {
            stop()
          } {}
        }
      }
    }))
  }

  def start(): Unit = {
    this._isStopping.set(false)
    showBasicConfiguration()
    addShutdownHooks()
    startServices()
  }

  def stop(): Unit = {
    this._isStopping.set(true)
    stopServices()
  }

  /**
   * Stops Aquarium after the given millis. Used during testing.
   */
  def stopAfterMillis(millis: Long) {
    Thread sleep millis
    stop()
  }

  /**
   * Reflectively provide a new instance of a class and configure it appropriately.
   */
  def newInstance[C <: AnyRef](_class: Class[C], className: String): C = {
    val originalProps = apply(EnvKeys.originalProps)

    val instanceM = MaybeEither(defaultClassLoader.loadClass(className).newInstance().asInstanceOf[C])
    instanceM match {
      case Just(instance) ⇒
        eventBus.addSubscriber[C](instance)

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

            logger.debug("Configuring {} with props", configurable.getClass.getName)
            MaybeEither(configurable configure localProps) match {
              case Just(_) ⇒
                logger.info("Configured {} with props", configurable.getClass.getName)
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

  def currentResourcesMap: DSLResourcesMap = {
    // FIXME: Get rid of this singleton stuff
    Policy.policy.resourcesMap
  }

  def initialAgreementForRole(role: String, referenceTimeMillis: Long): String = {
    // FIXME: Where is the mapping?
    "default"
  }

  def initialBalanceForRole(role: String, referenceTimeMillis: Long): Double = {
    // FIXME: Where is the mapping?
    10000.0
  }

  def defaultInitialUserRole: String = {
    // FIXME: Read from properties?
    "default"
  }

  def defaultClassLoader = apply(EnvKeys.defaultClassLoader)

  def resourceEventStore = apply(EnvKeys.resourceEventStore)

  def imEventStore = apply(EnvKeys.imEventStore)

  def userStateStore = apply(EnvKeys.userStateStore)

  def policyStore = apply(EnvKeys.policyStore)

  def eventsStoreFolder = apply(EnvKeys.eventsStoreFolder)

  def algorithmCompiler = apply(EnvKeys.algorithmCompiler)

  def eventBus = apply(EnvKeys.eventBus)

  def userStateComputations = apply(EnvKeys.userStateComputations)

  def userStateTimestampThreshold = apply(EnvKeys.userStateTimestampThreshold)

  def adminCookie = apply(EnvKeys.adminCookie)

  def converters = apply(EnvKeys.converters)

//  def actorProvider = apply(EnvKeys.actorProvider)

  def saveResourceEventsToEventsStoreFolder = apply(EnvKeys.eventsStoreSaveRCEvents)

  def saveIMEventsToEventsStoreFolder = apply(EnvKeys.eventsStoreSaveIMEvents)

  def timerService = apply(EnvKeys.timerService)

  def restPort = apply(EnvKeys.restPort)

  def akkaService = apply(EnvKeys.akkaService)

  def version = apply(EnvKeys.version)
}

object Aquarium {
  final val PropsToShow = List(
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
    SysProp.FileEncoding
  )

  object HTTP {
   final val RESTAdminHeaderName = "X-Aquarium-Admin-Cookie"
   final val RESTAdminHeaderNameLowerCase = RESTAdminHeaderName.toLowerCase
 }

  final class AquariumEnvKey[T: Manifest](override val name: String) extends TypedKeySkeleton[T](name) {
    override def toString = name
  }

  final val ServiceKeys: List[TypedKey[_ <: Lifecycle]] = List(
    EnvKeys.timerService,
    EnvKeys.akkaService,
    EnvKeys.eventBus,
    EnvKeys.restService,
    EnvKeys.rabbitMQService,
    EnvKeys.storeWatcherService
  )

  object EnvKeys {
    /**
     * The Aquarium version. Will be reported in any due occasion.
     */
    final val version = StringKey("version")

    final val originalProps: TypedKey[Props] =
      new AquariumEnvKey[Props]("originalProps")

    /**
     * The fully qualified name of the class that implements the `StoreProvider`.
     * Will be instantiated reflectively and should have a public default constructor.
     */
    final val storeProvider: TypedKey[StoreProvider] =
      new AquariumEnvKey[StoreProvider]("store.provider.class")

    /**
     * If a value is given to this property, then it represents a folder where all events coming to aquarium are
     * saved.
     *
     * This is for debugging purposes.
     */
    final val eventsStoreFolder: TypedKey[Option[File]] =
      new AquariumEnvKey[Option[File]]("events.store.folder")

    /**
     * If this is `true` and `events.store.folder` is defined, then all resource events are
     * also stored in `events.store.folder`.
     *
     * This is for debugging purposes.
     */

    final val eventsStoreSaveRCEvents = BooleanKey("events.store.save.rc.events")

    /**
     * If this is `true` and `events.store.folder` is defined, then all IM events are
     * also stored in `events.store.folder`.
     *
     * This is for debugging purposes.
     */
    final val eventsStoreSaveIMEvents = BooleanKey("events.store.save.im.events")

    /**
     * A time period in milliseconds for which we can tolerate stale parts regarding user state.
     *
     * The smaller the value, the more accurate the user credits and other state parts are.
     *
     * If a request for user state (e.g. balance) is received and the request timestamp exceeds
     * the timestamp of the last known balance amount by this value, then a re-computation for
     * the balance is triggered.
     */
    final val userStateTimestampThreshold = LongKey("user.state.timestamp.threshold")

    /**
     * REST service listening port.
     *
     * Default is 8080.
     */
    final val restPort = IntKey("rest.port")

    final val restShutdownTimeoutMillis = LongKey("rest.shutdown.timeout.millis")

    /**
     * A cookie used in every administrative REST API call, so that Aquarium knows it comes from
     * an authorised client.
     */
    final val adminCookie: TypedKey[Option[String]] =
      new AquariumEnvKey[Option[String]]("admin.cookie")

    final val resourceEventStore: TypedKey[ResourceEventStore] =
      new AquariumEnvKey[ResourceEventStore]("resource.event.store.class")

    final val imEventStore: TypedKey[IMEventStore] =
      new AquariumEnvKey[IMEventStore]("im.event.store.class")

    final val userStateStore: TypedKey[UserStateStore] =
      new AquariumEnvKey[UserStateStore]("user.state.store.class")

    final val policyStore: TypedKey[PolicyStore] =
      new AquariumEnvKey[PolicyStore]("policy.store.class")

    /**
     * The class that initializes the REST service
     */
    final val restService: TypedKey[Lifecycle] =
      new AquariumEnvKey[Lifecycle]("rest.service.class")

    /**
     * The fully qualified name of the class that implements the `RoleableActorProviderService`.
     * Will be instantiated reflectively and should have a public default constructor.
     */
//    final val actorProvider: TypedKey[RoleableActorProviderService] =
//      new AquariumEnvKey[RoleableActorProviderService]("actor.provider.class")

    final val akkaService: TypedKey[AkkaService] =
      new AquariumEnvKey[AkkaService]("akka.service")

    final val eventBus: TypedKey[EventBusService] =
      new AquariumEnvKey[EventBusService]("event.bus.service")

    final val timerService: TypedKey[TimerService] =
      new AquariumEnvKey[TimerService]("timer.service")

    final val rabbitMQService: TypedKey[RabbitMQService] =
      new AquariumEnvKey[RabbitMQService]("rabbitmq.service")

    final val storeWatcherService: TypedKey[StoreWatcherService] =
      new AquariumEnvKey[StoreWatcherService]("store.watcher.service")

    final val converters: TypedKey[Converters] =
      new AquariumEnvKey[Converters]("converters")

    final val algorithmCompiler: TypedKey[CostPolicyAlgorithmCompiler] =
      new AquariumEnvKey[CostPolicyAlgorithmCompiler]("algorithm.compiler")

    final val userStateComputations: TypedKey[UserStateComputations] =
      new AquariumEnvKey[UserStateComputations]("user.state.computations")

    final val defaultClassLoader: TypedKey[ClassLoader] =
      new AquariumEnvKey[ClassLoader]("default.class.loader")

  }
}
