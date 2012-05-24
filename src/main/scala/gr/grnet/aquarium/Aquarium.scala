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

import java.io.File

import com.ckkloverdos.maybe._
import com.ckkloverdos.props.Props
import com.ckkloverdos.convert.Converters.{DefaultConverters => TheDefaultConverters}

import gr.grnet.aquarium.service._
import gr.grnet.aquarium.util.{Lifecycle, Loggable, shortNameOfClass}
import gr.grnet.aquarium.store._
import gr.grnet.aquarium.connector.rabbitmq.service.RabbitMQService
import gr.grnet.aquarium.converter.StdConverters
import java.util.concurrent.atomic.AtomicBoolean
import gr.grnet.aquarium.ResourceLocator._
import com.ckkloverdos.sys.SysProp

/**
 * This is the Aquarium entry point.
 *
 * Responsible to load all of application configuration and provide the relevant services.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
final class Aquarium(val props: Props) extends Lifecycle with Loggable {
  import Aquarium.Keys

  private[this] val _isStopping = new AtomicBoolean(false)

  def isStopping() = _isStopping.get()

  /**
   * Reflectively provide a new instance of a class and configure it appropriately.
   */
  private[this] def newInstance[C : Manifest](_className: String = ""): C = {
    val className = _className match {
      case "" ⇒
        manifest[C].erasure.getName

      case name ⇒
        name
    }

    val instanceM = MaybeEither(defaultClassLoader.loadClass(className).newInstance().asInstanceOf[C])
    instanceM match {
      case Just(instance) ⇒ instance match {
        case configurable: Configurable ⇒
          val localProps = configurable.propertyPrefix match {
            case Some(prefix) ⇒
              props.subsetForKeyPrefix(prefix)

            case None ⇒
              props
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

  private[this] lazy val _actorProvider = newInstance[RoleableActorProviderService](props(Keys.actor_provider_class))

  /**
   * Initializes a store provider, according to the value configured
   * in the configuration file. The
   */
  private[this] lazy val _storeProvider = newInstance[StoreProvider](props(Keys.store_provider_class))
  
  private[this] lazy val _restService = newInstance[Lifecycle](props(Keys.rest_service_class))

  private[this] lazy val _userStateStoreM: Maybe[UserStateStore] = {
    // If there is a specific `UserStateStore` implementation specified in the
    // properties, then this implementation overrides the user store given by
    // `StoreProvider`.
    props.get(Keys.user_state_store_class) map { className ⇒
      val instance = newInstance[UserStateStore](className)
      logger.info("Overriding %s provisioning. Implementation given by: %s".format(
        shortNameOfClass(classOf[UserStateStore]),
        instance.getClass))
      instance
    }
  }

  private[this] lazy val _resourceEventStoreM: Maybe[ResourceEventStore] = {
    // If there is a specific `EventStore` implementation specified in the
    // properties, then this implementation overrides the event store given by
    // `StoreProvider`.
    props.get(Keys.resource_event_store_class) map { className ⇒
      val instance = newInstance[ResourceEventStore](className)
      logger.info("Overriding EventStore provisioning. Implementation given by: %s".format(instance.getClass))
      instance
    }
  }

  private[this] lazy val _imEventStoreM: Maybe[IMEventStore] = {
    props.get(Keys.user_event_store_class) map { className ⇒
      val instance = newInstance[IMEventStore](className)
      logger.info("Overriding IMEventStore provisioning. Implementation given by: %s".format(instance.getClass))
      instance
    }
  }

  private[this] lazy val _WalletEventStoreM: Maybe[WalletEntryStore] = {
    // If there is a specific `IMStore` implementation specified in the
    // properties, then this implementation overrides the event store given by
    // `IMProvider`.
    props.get(Keys.wallet_entry_store_class) map {
      className ⇒
        val instance = newInstance[WalletEntryStore](className)
        logger.info("Overriding WalletEntryStore provisioning. Implementation given by: %s".format(instance.getClass))
        instance
    }
  }

  private[this] lazy val _policyStoreM: Maybe[PolicyStore] = {
    props.get(Keys.policy_store_class) map {
      className ⇒
        val instance = newInstance[PolicyStore](className)
        logger.info("Overriding PolicyStore provisioning. Implementation given by: %s".format(instance.getClass))
        instance
    }
  }

  private[this] lazy val _eventsStoreFolder: Maybe[File] = {
    props.get(Keys.events_store_folder) map {
      folderName ⇒
        logger.info("{} = {}", Keys.events_store_folder, folderName)
        
        val canonicalFolder = {
          val folder = new File(folderName)
          if(folder.isAbsolute) {
            folder.getCanonicalFile
          } else {
            logger.info("{} is not absolute, making it relative to Aquarium Home", Keys.events_store_folder)
            new File(ResourceLocator.Homes.Folders.AquariumHome, folderName).getCanonicalFile
          }
        }

        val canonicalPath = canonicalFolder.getCanonicalPath

        if(canonicalFolder.exists() && !canonicalFolder.isDirectory) {
          throw new AquariumInternalError("%s = %s is not a folder".format(Keys.events_store_folder, canonicalFolder))
        }

        // Now, events folder must be outside AQUARIUM_HOME, since AQUARIUM_HOME can be wiped out for an upgrade but
        // we still want to keep the events.
        val ahCanonicalPath = ResourceLocator.Homes.Folders.AquariumHome.getCanonicalPath
        if(canonicalPath.startsWith(ahCanonicalPath)) {
          throw new AquariumException(
            "%s = %s is under Aquarium Home = %s".format(
              Keys.events_store_folder,
              canonicalFolder,
              ahCanonicalPath
            ))
        }

        canonicalFolder.mkdirs()

        canonicalFolder
    }
  }

  private[this] lazy val _events_store_save_rc_events = props.getBoolean(Keys.events_store_save_rc_events).getOr(false)

  private[this] lazy val _events_store_save_im_events = props.getBoolean(Keys.events_store_save_im_events).getOr(false)

  private[this] lazy val _converters = StdConverters.AllConverters

  private[this] lazy val _timerService: TimerService = newInstance[SimpleTimerService]()

  private[this] lazy val _akka = newInstance[AkkaService]()

  private[this] lazy val _eventBus = newInstance[EventBusService]()

  private[this] lazy val _rabbitmqService = newInstance[RabbitMQService]()

  private[this] lazy val _storeWatcherService = newInstance[StoreWatcherService]()

  private[this] lazy val _allServices = List(
    _timerService,
    _akka,
    _actorProvider,
    _eventBus,
    _restService,
    _rabbitmqService,
    _storeWatcherService
  )

  def get(key: String, default: String = ""): String = props.getOr(key, default)

  def defaultClassLoader = Thread.currentThread().getContextClassLoader

  /**
   * FIXME: This must be ditched.
   * 
   * Find a file whose location can be overiden in
   * the configuration file (e.g. policy.yaml)
   *
   * @param name Name of the file to search for
   * @param prop Name of the property that defines the file path
   * @param default Name to return if no file is found
   */
  def findConfigFile(name: String, prop: String, default: String): File = {
    // Check for the configured value first
    val configured = new File(get(prop))
    if (configured.exists)
      return configured

    // Look into the configuration context
    ResourceLocator.getResource(name) match {
      case Just(policyResource) ⇒
        val path = policyResource.url.getPath
        new File(path)
      case _ ⇒
        new File(default)
    }
  }

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

  def stopWithDelay(millis: Long) {
    Thread sleep millis
    stop()
  }

  private[this] def configure(): Unit = {
    logger.info("Aquarium Home = %s".format(
      if(Homes.Folders.AquariumHome.isAbsolute)
        Homes.Folders.AquariumHome
      else
        "%s [=%s]".format(Homes.Folders.AquariumHome, Homes.Folders.AquariumHome.getCanonicalPath)
    ))

    for(folder ← this.eventsStoreFolder) {
      logger.info("{} = {}", Aquarium.Keys.events_store_folder, folder)
    }
    this.eventsStoreFolder.throwMe // on error

    for(prop ← Aquarium.PropsToShow) {
      logger.info("{} = {}", prop.name, prop.rawValue)
    }


    logger.info("CONF_HERE = {}", CONF_HERE)
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

  def start() = {
    this._isStopping.set(false)
    configure()
    addShutdownHooks()
    startServices()
  }

  def stop() = {
    this._isStopping.set(true)
    stopServices()
  }

  def converters = _converters
  
  def actorProvider = _actorProvider

  def eventBus = _eventBus

  def timerService = _timerService

  def userStateStore = {
    _userStateStoreM match {
      case Just(us) ⇒ us
      case _        ⇒ storeProvider.userStateStore
    }
  }

  def resourceEventStore = {
    _resourceEventStoreM match {
      case Just(es) ⇒ es
      case _        ⇒ storeProvider.resourceEventStore
    }
  }

  def walletStore = {
    _WalletEventStoreM match {
      case Just(es) ⇒ es
      case _        ⇒ storeProvider.walletEntryStore
    }
  }

  def imEventStore = {
    _imEventStoreM match {
      case Just(es) ⇒ es
      case _        ⇒ storeProvider.imEventStore
    }
  }

  def policyStore = {
    _policyStoreM match {
      case Just(es) ⇒ es
      case _        ⇒ storeProvider.policyStore
    }
  }

  def storeProvider = _storeProvider
  
  def withStoreProviderClass[C <: StoreProvider](spc: Class[C]): Aquarium = {
    val map = this.props.map
    val newMap = map.updated(Keys.store_provider_class, spc.getName)
    val newProps = new Props(newMap)
    new Aquarium(newProps)
  }

  def eventsStoreFolder = _eventsStoreFolder

  def saveResourceEventsToEventsStoreFolder = _events_store_save_rc_events

  def saveIMEventsToEventsStoreFolder = _events_store_save_im_events

  def adminCookie: MaybeOption[String] = props.get(Aquarium.Keys.admin_cookie) match {
    case just @ Just(_) ⇒ just
    case _ ⇒ NoVal
  }
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

  implicit val DefaultConverters = TheDefaultConverters

  final val PolicyConfName = ResourceLocator.ResourceNames.POLICY_YAML

  final val RolesAgreementsName = ResourceLocator.ResourceNames.ROLE_AGREEMENTS_MAP

  final lazy val AquariumPropertiesResource = ResourceLocator.Resources.AquariumPropertiesResource

  final lazy val AquariumProperties = {
    val maybeProps = Props(AquariumPropertiesResource)
    maybeProps match {
      case Just(props) ⇒
        props

      case NoVal ⇒
        throw new AquariumInternalError(
          "Could not load %s from %s".format(
            ResourceLocator.ResourceNames.AQUARIUM_PROPERTIES,
            AquariumPropertiesResource))


      case Failed(e) ⇒
        throw new AquariumInternalError(
          "Could not load %s from %s".format(
            ResourceLocator.ResourceNames.AQUARIUM_PROPERTIES,
            AquariumPropertiesResource),
          e)
    }
  }

  /**
   * The main [[gr.grnet.aquarium.Aquarium]] instance.
   */
  final lazy val Instance = {
    Maybe(new Aquarium(AquariumProperties)) match {
      case Just(masterConf) ⇒
        masterConf

      case NoVal ⇒
        throw new AquariumInternalError(
          "Could not create Aquarium configuration from %s".format(
            AquariumPropertiesResource))

      case Failed(e) ⇒
        throw new AquariumInternalError(
          "Could not create Aquarium configuration from %s".format(
            AquariumPropertiesResource),
          e)
    }
  }

  /**
   * Defines the names of all the known keys inside the master properties file.
   */
  final object Keys {

    /**
     * The Aquarium version. Will be reported in any due occasion.
     */
    final val version = "version"

    /**
     * The fully qualified name of the class that implements the `RoleableActorProviderService`.
     * Will be instantiated reflectively and should have a public default constructor.
     */
    final val actor_provider_class = "actor.provider.class"

    /**
     * The class that initializes the REST service
     */
    final val rest_service_class = "rest.service.class"

    /**
     * The fully qualified name of the class that implements the `StoreProvider`.
     * Will be instantiated reflectively and should have a public default constructor.
     */
    final val store_provider_class = "store.provider.class"

    /**
     * The class that implements the User store
     */
    final val user_state_store_class = "user.state.store.class"

    /**
     * The class that implements the resource event store
     */
    final val resource_event_store_class = "resource.event.store.class"

    /**
     * The class that implements the IM event store
     */
    final val user_event_store_class = "user.event.store.class"

    /**
     * The class that implements the wallet entries store
     */
    final val wallet_entry_store_class = "wallet.entry.store.class"

    /**
     * The class that implements the wallet entries store
     */
    final val policy_store_class = "policy.store.class"


    /** The lower mark for the UserActors' LRU.
     *
     * The terminology is borrowed from the (also borrowed) Apache-lucene-solr-based implementation.
     *
     */
    final val user_actors_lru_lower_mark = "user.actors.LRU.lower.mark"

    /**
     * The upper mark for the UserActors' LRU.
     *
     * The terminology is borrowed from the (also borrowed) Apache-lucene-solr-based implementation.
     */
    final val user_actors_lru_upper_mark = "user.actors.LRU.upper.mark"

    /**
     * REST service listening port.
     *
     * Default is 8080.
     */
    final val rest_port = "rest.port"

    /**
     * Location of the Aquarium accounting policy config file
     */
    final val aquarium_policy = "aquarium.policy"

    /**
     * Location of the role-agreement mapping file
     */
    final val aquarium_role_agreement_map = "aquarium.role-agreement.map"
    
    /**
     * A time period in milliseconds for which we can tolerate stale data regarding user state.
     *
     * The smaller the value, the more accurate the user credits and other state data are.
     *
     * If a request for user state (e.g. balance) is received and the request timestamp exceeds
     * the timestamp of the last known balance amount by this value, then a re-computation for
     * the balance is triggered.
     */
    final val user_state_timestamp_threshold = "user.state.timestamp.threshold"

    /**
     * The time unit is the lowest billable time period.
     * For example, with a time unit of ten seconds, if a VM is started up and shut down in nine
     * seconds, then the user will be billed for ten seconds.
     *
     * This is an overall constant. We use it as a property in order to prepare ourselves for
     * multi-cloud setup, where the same Aquarium instance is used to bill several distinct cloud
     * infrastructures.
     */
    final val time_unit_in_millis = "time.unit.in.seconds"

    /**
     * If a value is given to this property, then it represents a folder where all events coming to aquarium are
     * saved.
     */
    final val events_store_folder = "events.store.folder"

    /**
     * If this is `true` and `events.store.folder` is defined, then all resource events are
     * also stored in `events.store.folder`.
     *
     * This is for debugging purposes.
     */
    final val events_store_save_rc_events = "events.store.save.rc.events"

    /**
     * If this is `true` and `events.store.folder` is defined, then all IM events are
     * also stored in `events.store.folder`.
     *
     * This is for debugging purposes.
     */
    final val events_store_save_im_events = "events.store.save.im.events"

    /**
     * If set to `true`, then an IM event that cannot be parsed to [[gr.grnet.aquarium.event.model.im.IMEventModel]] is
     * saved to the [[gr.grnet.aquarium.store.IMEventStore]].
     */
    final val save_unparsed_event_im = "save.unparsed.event.im"

    /**
     * A cookie used in every administrative REST API call, so that Aquarium knows it comes from
     * an authorised client.
     */
    final val admin_cookie = "admin.cookie"
  }

  object HTTP {
    final val RESTAdminHeaderName = "X-Aquarium-Admin-Cookie"
    final val RESTAdminHeaderNameLowerCase = RESTAdminHeaderName.toLowerCase
  }
}
