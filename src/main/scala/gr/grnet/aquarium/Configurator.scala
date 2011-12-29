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

package gr.grnet.aquarium

import actor.{ActorProvider}
import com.ckkloverdos.resource._
import com.ckkloverdos.sys.SysProp
import com.ckkloverdos.props.Props
import com.ckkloverdos.maybe.{Maybe, Failed, Just, NoVal}
import com.ckkloverdos.convert.Converters.{DefaultConverters => TheDefaultConverters}
import processor.actor.{ResourceEventProcessorService}
import store._
import util.{Lifecycle, Loggable}

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
              throw new Exception("Could not configure instance of %s".format(className), e)
            case NoVal ⇒
              throw new Exception("Could not configure instance of %s".format(className))
          }
        case _ ⇒
          instance
      }
      case Failed(e, _) ⇒
        throw new Exception("Could not instantiate %s".format(className), e)
      case NoVal ⇒
        throw new Exception("Could not instantiate %s".format(className))
    }

  }

  private[this] lazy val _actorProvider: ActorProvider = {
    val instance = newInstance[ActorProvider](props.getEx(Keys.actor_provider_class))
    logger.info("Loaded ActorProvider: %s".format(instance.getClass))
    instance
  }

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

  private[this] lazy val _WalletEventStoreM: Maybe[WalletEntryStore] = {
    // If there is a specific `IMStore` implementation specified in the
    // properties, then this implementation overrides the event store given by
    // `IMProvider`.
    props.get(Keys.wallet_entry_store_class) map { className ⇒
      val instance = newInstance[WalletEntryStore](className)
      logger.info("Overriding WalletEntryStore provisioning. Implementation given by: %s".format(instance.getClass))
      instance
    }
  }

  private[this] lazy val _resEventProc: ResourceEventProcessorService = {
    new ResourceEventProcessorService()
  }

  def get(key: String, default: String = ""): String = props.getOr(key, default)

  def defaultClassLoader = Thread.currentThread().getContextClassLoader

  def startServices(): Unit = {
    _restService.start()
    _actorProvider.start()
    _resEventProc.start()
  }

  def stopServices(): Unit = {
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

  def storeProvider = _storeProvider
}

object Configurator {
  implicit val DefaultConverters = TheDefaultConverters

  val MasterConfName = "aquarium.properties"

  /**
   * Current directory resource context.
   * Normally this should be the application installation directory.
   *
   * It takes priority over `ClasspathBaseResourceContext`.
   */
  val AppBaseResourceContext = new FileStreamResourceContext(".")

  /**
   * The venerable /etc resource context
   */
  val SlashEtcResourceContext = new FileStreamResourceContext("/etc")

  /**
   * Class loader resource context.
   * This has the lowest priority.
   */
  val ClasspathBaseResourceContext = new ClassLoaderStreamResourceContext(Thread.currentThread().getContextClassLoader)

  /**
   * Use this property to override the place where aquarium configuration resides.
   *
   * The value of this property is a folder that defines the highest-priority resource context.
   */
  val ConfBaseFolderSysProp = SysProp("aquarium.conf.base.folder")

  /**
   * The resource context used in the application.
   */
  lazy val MasterResourceContext = {
    val rc0 = ClasspathBaseResourceContext
    val rc1 = AppBaseResourceContext
    val rc2 = SlashEtcResourceContext
    val basicContext = new CompositeStreamResourceContext(NoVal, rc2, rc1, rc0)
    
    ConfBaseFolderSysProp.value match {
      case Just(value) ⇒
        // We have a system override for the configuration location
        new CompositeStreamResourceContext(Just(basicContext), new FileStreamResourceContext(value))
      case NoVal ⇒
        basicContext
      case Failed(e, m) ⇒
        throw new RuntimeException(m , e)
    }
  }

  lazy val MasterConfResource = {
    val maybeMCResource = MasterResourceContext getResource MasterConfName
    maybeMCResource match {
      case Just(masterConfResource) ⇒
        masterConfResource
      case NoVal ⇒
        throw new RuntimeException("Could not find master configuration file: %s".format(MasterConfName))
      case Failed(e, m) ⇒
        throw new RuntimeException(m, e)
    }
  }

  lazy val MasterConfProps = {
    val maybeProps = Props apply MasterConfResource
    maybeProps match {
      case Just(props) ⇒
        props
      case NoVal ⇒
        throw new RuntimeException("Could not load master configuration file: %s".format(MasterConfName))
      case Failed(e, m) ⇒
        throw new RuntimeException(m, e)
    }
  }

  lazy val MasterConfigurator = {
    Maybe(new Configurator(MasterConfProps)) match {
      case Just(masterConf) ⇒
        masterConf
      case NoVal ⇒
        throw new RuntimeException("Could not initialize master configuration file: %s".format(MasterConfName))
      case Failed(e, m) ⇒
        throw new RuntimeException(m, e)
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
    final val im_eventstore_class = "imevent.store.class"

    /**
     * The class that implements the wallet entries store
     */
    final val wallet_entry_store_class = "wallet.entry.store.class"

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
    final val amqp_exchanges = "amqp.exchanges"

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
     * A time period in milliseconds for which we can tolerate stale data regarding user state.
     *
     * The smaller the value, the more accurate the user credits and other state data are.
     *
     * If a request for user state (e.g. balance) is received and the request timestamp exceeds
     * the timestamp of the last known balance amount by this value, then a re-computation for
     * the balance is triggered.
     */
    final val user_state_timestamp_threshold = "user.state.timestamp.threshold"
  }
}