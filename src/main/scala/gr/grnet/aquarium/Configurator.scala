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

import actor.ActorProvider
import com.ckkloverdos.props.Props
import com.ckkloverdos.maybe.{Maybe, Failed, Just, NoVal}
import com.ckkloverdos.convert.Converters.{DefaultConverters => TheDefaultConverters}
import processor.actor.{UserEventProcessorService, ResourceEventProcessorService}
import store._
import util.{Lifecycle, Loggable}
import java.io.File

/**
 * The master configurator. Responsible to load all of application configuration and provide the relevant services.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class Configurator(val props: Props) extends Loggable {
  import Configurator.Keys

  /**
   * Reflectively provide a new instance of a class and configure it appropriately.
   */
  private[this] def newInstance[C : Manifest](className: String): C = {
    val instanceM = Maybe(defaultClassLoader.loadClass(className).newInstance().asInstanceOf[C])
    instanceM match {
      case Just(instance) ⇒ instance match {
        case configurable: Configurable ⇒
          Maybe(configurable configure props) match {
            case Just(_) ⇒
              instance
            case Failed(e, _) ⇒
              throw new AquariumException("Could not configure instance of %s".format(className), e)
            case NoVal ⇒
              throw new AquariumException("Could not configure instance of %s".format(className))
          }
        case _ ⇒
          instance
      }
      case Failed(e, _) ⇒
        throw new AquariumException("Could not instantiate %s".format(className), e)
      case NoVal ⇒
        throw new AquariumException("Could not instantiate %s".format(className))
    }

  }

  private[this] lazy val _actorProvider: ActorProvider = {
    val instance = newInstance[ActorProvider](props.getEx(Keys.actor_provider_class))
    logger.info("Loaded ActorProvider: %s".format(instance.getClass))
    instance
  }

  /**
   * Initializes a store provider, according to the value configured
   * in the configuration file. The
   */
  private[this] lazy val _storeProvider: StoreProvider = {
    val instance = newInstance[StoreProvider](props.getEx(Keys.store_provider_class))
    logger.info("Loaded StoreProvider: %s".format(instance.getClass))
    instance
  }
  
  private[this] lazy val _restService: Lifecycle = {
    val instance = newInstance[Lifecycle](props.getEx(Keys.rest_service_class))
    logger.info("Loaded RESTService: %s".format(instance.getClass))
    instance
  }

  private[this] lazy val _userStateStoreM: Maybe[UserStateStore] = {
    // If there is a specific `UserStateStore` implementation specified in the
    // properties, then this implementation overrides the user store given by
    // `StoreProvider`.
    props.get(Keys.user_state_store_class) map { className ⇒
      val instance = newInstance[UserStateStore](className)
      logger.info("Overriding UserStateStore provisioning. Implementation given by: %s".format(instance.getClass))
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

  private[this] lazy val _userEventStoreM: Maybe[UserEventStore] = {
    props.get(Keys.user_event_store_class) map { className ⇒
      val instance = newInstance[UserEventStore](className)
      logger.info("Overriding UserEventStore provisioning. Implementation given by: %s".format(instance.getClass))
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
        val folder = {
          val folder = new File(folderName)
          if(folder.isAbsolute) {
            folder
          } else {
            logger.info("{} is not absolute, making it relative to AQUARIUM_HOME", Keys.events_store_folder)
            new File(ResourceLocator.AQUARIUM_HOME_FOLDER, folderName)
          }
        }
        folder.mkdirs()
        if(folder.isDirectory) {
          folder.getCanonicalFile
        } else {
          throw new AquariumException("%s = %s is not a folder".format(Keys.events_store_folder, folder))
        }
    }
  }

  private[this] lazy val _resEventProc: ResourceEventProcessorService = new ResourceEventProcessorService

  private[this] lazy val _imEventProc: UserEventProcessorService = new UserEventProcessorService

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

  def startServices(): Unit = {
    _restService.start()
    _actorProvider.start()
    _resEventProc.start()
    _imEventProc.start()
  }

  def stopServices(): Unit = {
    _imEventProc.stop()
    _resEventProc.stop()
    _restService.stop()
    _actorProvider.stop()

//    akka.actor.Actor.registry.shutdownAll()
  }

  def stopServicesWithDelay(millis: Long) {
    Thread sleep millis
    stopServices()
  }
  
  def actorProvider = _actorProvider

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

  def userEventStore = {
    _userEventStoreM match {
      case Just(es) ⇒ es
      case _        ⇒ storeProvider.userEventStore
    }
  }

  def policyStore = {
    _policyStoreM match {
      case Just(es) ⇒ es
      case _        ⇒ storeProvider.policyStore
    }
  }

  def storeProvider = _storeProvider
  
  def withStoreProviderClass[C <: StoreProvider](spc: Class[C]): Configurator = {
    val map = this.props.map
    val newMap = map.updated(Keys.store_provider_class, spc.getName)
    val newProps = new Props(newMap)
    new Configurator(newProps)
  }

  // FIXME: This is instead of props.getInt which currently contains a bug.
  // FIXME: Fix the original bug and delete this method
  def getInt(name: String): Maybe[Int] = {
    props.get(name).map(_.toInt)
  }

  def hasEventsStoreFolder = _eventsStoreFolder.isJust

  def eventsStoreFolder = gr.grnet.aquarium.util.justForSure(_eventsStoreFolder).get
}

object Configurator {
  implicit val DefaultConverters = TheDefaultConverters

  val MasterConfName = "aquarium.properties"

  val PolicyConfName = "policy.yaml"

  val RolesAgreementsName = "roles-agreements.map"

  lazy val MasterConfResource = {
    val maybeMCResource = ResourceLocator getResource MasterConfName
    maybeMCResource match {
      case Just(masterConfResource) ⇒
        masterConfResource
      case NoVal ⇒
        throw new AquariumException("Could not find master configuration file: %s".format(MasterConfName))
      case Failed(e, m) ⇒
        throw new AquariumException(m, e)
    }
  }

  lazy val MasterConfProps = {
    val maybeProps = Props apply MasterConfResource
    maybeProps match {
      case Just(props) ⇒
        props
      case NoVal ⇒
        throw new AquariumException("Could not load master configuration file: %s".format(MasterConfName))
      case Failed(e, m) ⇒
        throw new AquariumException(m, e)
    }
  }

  lazy val MasterConfigurator = {
    Maybe(new Configurator(MasterConfProps)) match {
      case Just(masterConf) ⇒
        masterConf
      case NoVal ⇒
        throw new AquariumException("Could not initialize master configuration file: %s".format(MasterConfName))
      case Failed(e, m) ⇒
        throw new AquariumException(m, e)
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
     * The fully qualified name of the class that implements the `ActorProvider`.
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


    /** The lower mark for the UserActors' LRU, managed by UserActorManager.
     *
     * The terminology is borrowed from the (also borrowed) Apache-lucene-solr-based implementation.
     *
     */
    final val user_actors_lru_lower_mark = "user.actors.LRU.lower.mark"

    /**
     * The upper mark for the UserActors' LRU, managed by UserActorManager.
     *
     * The terminology is borrowed from the (also borrowed) Apache-lucene-solr-based implementation.
     */
    final val user_actors_lru_upper_mark = "user.actors.LRU.upper.mark"

    /**
     * Comma separated list of amqp servers running in active-active
     * configuration.
     */
    final val amqp_servers = "amqp.servers"

    /**
     * Comma separated list of amqp servers running in active-active
     * configuration.
     */
    final val amqp_port = "amqp.port"

    /**
     * User name for connecting with the AMQP server
     */
    final val amqp_username = "amqp.username"

    /**
     * Passwd for connecting with the AMQP server
     */
    final val amqp_password = "amqp.passwd"

    /**
     * Virtual host on the AMQP server
     */
    final val amqp_vhost = "amqp.vhost"

    /**
     * Comma separated list of exchanges known to aquarium
     */
    final val amqp_exchange = "amqp.exchange"

    /**
     * Queues for retrieving resource events from. Multiple queues can be
     * declared, seperated by semicolon
     *
     * Format is `exchange:routing.key:queue-name;<exchnage2:routing.key2:queue-name>;...`
     */
    final val amqp_resevents_queues = "amqp.resevents.queues"

    /**
     * Queues for retrieving user events from. Multiple queues can be
     * declared, seperated by semicolon
     *
     * Format is `exchange:routing.key:queue-name;<exchnage2:routing.key2:queue-name>;...`
     */
    final val amqp_userevents_queues="amqp.userevents.queues"

    /**
     * REST service listening port.
     *
     * Default is 8080.
     */
    final val rest_port = "rest.port"

    /*
     * Provider for persistence services
     */
    final val persistence_provider = "persistence.provider"

    /**
     * Hostname for the persistence service
     */
    final val persistence_host = "persistence.host"

    /**
     * Username for connecting to the persistence service
     */
    final val persistence_username = "persistence.username"

    /**
     *  Password for connecting to the persistence service
     */
    final val persistence_password = "persistence.password"

    /**
     *  Password for connecting to the persistence service
     */
    final val persistence_port = "persistence.port"

    /**
     *  The DB schema to use
     */
    final val persistence_db = "persistence.db"

    /**
     * Maximum number of open connections to MongoDB
     */
    final val mongo_connection_pool_size = "mongo.connection.pool.size"

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
     * stored.
     */
    final val events_store_folder = "events.store.folder"

    /**
     * If set to `true`, then an IM event that cannot be parsed to [[gr.grnet.aquarium.logic.events.UserEvent]] is
     * saved to the [[gr.grnet.aquarium.store.UserEventStore]].
     */
    final val save_unparsed_event_im = "save.unparsed.event.im"
  }
}
