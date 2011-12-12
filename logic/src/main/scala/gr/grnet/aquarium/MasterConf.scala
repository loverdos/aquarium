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

import actor.{DispatcherRole, ActorProvider}
import com.ckkloverdos.resource._
import com.ckkloverdos.sys.SysProp
import com.ckkloverdos.props.Props
import com.ckkloverdos.maybe.{Maybe, Failed, Just, NoVal}
import com.ckkloverdos.convert.Converters.{DefaultConverters => TheDefaultConverters}
import processor.actor.ConfigureDispatcher
import rest.RESTService

/**
 * The master configurator. Responsible to load all of application configuration and provide the relevant services.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
class MasterConf(val props: Props) {
  import MasterConf.Keys

  private[this] def newInstance[C : Manifest](className: String): C = {
    val c = defaultClassLoader.loadClass(className).newInstance().asInstanceOf[C]
    c match {
      case configurable: Configurable ⇒
        configurable configure props
        c
      case _ ⇒
        c
    }
  }

  private[this] val _actorProvider: ActorProvider = {
    newInstance[ActorProvider](props.getEx(Keys.actor_provider_class))
  }
  
  private[this] val _restService: RESTService = {
    newInstance[RESTService](props.getEx(Keys.rest_service_class))
  }

  def get(prop: String): String =
    props.get(prop) match {
      case Just(y) => y
      case _ => ""
    }

  def defaultClassLoader = Thread.currentThread().getContextClassLoader

  def startServices(): Unit = {
    _restService.start()
    _actorProvider.start()

    _actorProvider.actorForRole(DispatcherRole) ! ConfigureDispatcher(this)
  }

  def stopServices(): Unit = {
    _restService.stop()
    _actorProvider.stop()
    
//    akka.actor.Actor.registry.shutdownAll()
  }

  def stopServicesWithDelay(millis: Long) {
    Thread sleep millis
    stopServices()
  }
  
  def actorProvider = _actorProvider
}

object MasterConf {
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

  lazy val MasterConf = {
    Maybe(new MasterConf(MasterConfProps)) match {
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
  }
}