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

import com.ckkloverdos.convert.Converters
import com.ckkloverdos.env.Env
import com.ckkloverdos.key.{IntKey, StringKey, LongKey, TypedKeySkeleton, TypedKey, BooleanKey}
import com.ckkloverdos.maybe._
import com.ckkloverdos.props.Props
import com.ckkloverdos.sys.SysProp
import connector.rabbitmq.RabbitMQProducer
import gr.grnet.aquarium.charging.{ChargingService, ChargingBehavior}
import gr.grnet.aquarium.message.avro.gen.{UserAgreementMsg, FullPriceTableMsg, IMEventMsg, ResourceTypeMsg, PolicyMsg}
import gr.grnet.aquarium.message.avro.{MessageHelpers, MessageFactory, ModelFactory, AvroHelpers}
import gr.grnet.aquarium.policy.{AdHocFullPriceTableRef, FullPriceTableModel, PolicyModel, CachingPolicyStore, PolicyDefinedFullPriceTableRef, UserAgreementModel, ResourceType}
import gr.grnet.aquarium.service.event.AquariumCreatedEvent
import gr.grnet.aquarium.service.{StoreWatcherService, RabbitMQService, TimerService, EventBusService, AkkaService}
import gr.grnet.aquarium.store.StoreProvider
import gr.grnet.aquarium.util.date.TimeHelpers
import gr.grnet.aquarium.util.{Loggable, Lifecycle}
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.{LoggerFactory, Logger}
import java.util.{Map ⇒ JMap}
import java.util.{HashMap ⇒ JHashMap}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

final class Aquarium(env: Env) extends Lifecycle with Loggable {

  import Aquarium.EnvKeys

  @volatile private[this] var _chargingBehaviorMap = Map[String, ChargingBehavior]()

  // Caching value for the latest resource mapping
  @volatile private[this] var _resourceMapping = apply(EnvKeys.defaultPolicyMsg).getResourceMapping

  private[this] lazy val cachingPolicyStore = new CachingPolicyStore(
    apply(EnvKeys.defaultPolicyMsg),
    apply(EnvKeys.storeProvider).policyStore
  )

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

  private[this] lazy val _allServices: Seq[_ <: Lifecycle] = Aquarium.ServiceKeys.map(this.apply(_))

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
    for(folder ← this.eventsStoreFolder) {
      logger.info("{} = {}", EnvKeys.eventsStoreFolder.name, folder)
    }
    this.eventsStoreFolder.throwMe // on error

    logger.info("default policy = {}", AvroHelpers.jsonStringOfSpecificRecord(defaultPolicyMsg))
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
  def newInstance[C <: AnyRef](_class: Class[C]): C = {
    newInstance(_class.getName)
  }

  /**
   * Reflectively provide a new instance of a class and configure it appropriately.
   */
  def newInstance[C <: AnyRef](className: String): C = {
    val originalProps = apply(EnvKeys.originalProps)

    val instanceM = MaybeEither(defaultClassLoader.loadClass(className).newInstance().asInstanceOf[C])
    instanceM match {
      case Just(instance) ⇒
//        eventBus.addSubscriber[C](instance)
        instance match {
          case aquariumAware: AquariumAware ⇒
            aquariumAware.awareOfAquarium(AquariumCreatedEvent(this))

          case _ ⇒
        }

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

              case Failed(e) ⇒
                throw new AquariumInternalError("Could not configure instance of %s".format(className), e)
            }

          case _ ⇒
        }

        instance

      case Failed(e) ⇒
        throw new AquariumInternalError("Could not instantiate %s".format(className), e)
    }

  }

  /**
   * @deprecated Use `currentResourceMapping` instead
   */
  def resourceMappingAtMillis(millis: Long): JMap[String, ResourceTypeMsg] = {
    val policyMspOpt = policyStore.loadPolicyAt(millis)
    if(policyMspOpt.isEmpty) {
      throw new AquariumInternalError(
        "Cannot get resource mapping. Not even the default policy found for time %s",
        TimeHelpers.toYYYYMMDDHHMMSSSSS(millis)
      )
    }

    val policyMsg = policyMspOpt.get
    policyMsg.getResourceMapping
  }

  /**
   * Provides the current resource mapping. This value is cached.
   *
   * NOTE: The assumption is that the resource mapping is always updated with new keys,
   *       that is we allow only the addition of new resource types.
   */
  def currentResourceMapping = {
    this._resourceMapping synchronized this._resourceMapping
  }

  //  def resourceTypesMapAtMillis(millis: Long): Map[String, ResourceType] = {
//    val policyMspOpt = policyStore.loadPolicyAt(millis)
//    if(policyMspOpt.isEmpty) {
//      throw new AquariumInternalError(
//        "Cannot get resource types map. Not even the default policy found for time %s",
//        TimeHelpers.toYYYYMMDDHHMMSSSSS(millis)
//      )
//    }
//
//    val policyMsg = policyMspOpt.get
//    // TODO optimize
//    ModelFactory.newPolicyModel(policyMsg).resourceTypesMap
//  }
//
//  def currentResourceTypesMap: Map[String, ResourceType] = {
//    resourceTypesMapAtMillis(TimeHelpers.nowMillis())
//  }

  def unsafeValidPolicyModelAt(referenceTimeMillis: Long): PolicyModel = {
    policyStore.loadPolicyAt(referenceTimeMillis) match {
      case None ⇒
        throw new AquariumInternalError(
          "No policy found at %s".format(TimeHelpers.toYYYYMMDDHHMMSSSSS(referenceTimeMillis))
        )

      case Some(policyMsg) ⇒
        ModelFactory.newPolicyModel(policyMsg)
    }
  }

  def unsafeValidPolicyAt(referenceTimeMillis: Long): PolicyMsg = {
    unsafeValidPolicyModelAt(referenceTimeMillis).msg
  }

  def unsafeFullPriceTableModelForRoleAt(role: String, referenceTimeMillis: Long): FullPriceTableModel = {
    val policyModelAtReferenceTime = unsafeValidPolicyModelAt(referenceTimeMillis)

    policyModelAtReferenceTime.roleMapping.get(role) match {
      case None ⇒
        throw new AquariumInternalError("Unknown price table for role %s at %s".format(
          role,
          TimeHelpers.toYYYYMMDDHHMMSSSSS(referenceTimeMillis)
        ))

      case Some(fullPriceTable) ⇒
        fullPriceTable
    }
  }

  def unsafeFullPriceTableForRoleAt(role: String, referenceTimeMillis: Long): FullPriceTableMsg = {
    val policyAtReferenceTime = unsafeValidPolicyAt(referenceTimeMillis)
    policyAtReferenceTime.getRoleMapping.get(role) match {
      case null ⇒
        throw new AquariumInternalError("Unknown price table for role %s at %s".format(
          role,
          TimeHelpers.toYYYYMMDDHHMMSSSSS(referenceTimeMillis)
        ))

      case fullPriceTable ⇒
        fullPriceTable
    }
  }

  def unsafeFullPriceTableModelForAgreement(
      userAgreementModel: UserAgreementModel,
      knownPolicyModel: PolicyModel
  ): FullPriceTableModel = {
    val policyModel = knownPolicyModel match {
      case null ⇒
        unsafeValidPolicyModelAt(userAgreementModel.validFromMillis)

      case policyModel ⇒
        policyModel
    }

    userAgreementModel.fullPriceTableRef match {
      case PolicyDefinedFullPriceTableRef ⇒
        val role = userAgreementModel.role
        policyModel.roleMapping.get(role) match {
          case None ⇒
            throw new AquariumInternalError("Unknown role %s while computing full price table for user %s at %s",
              role,
              userAgreementModel.userID,
              TimeHelpers.toYYYYMMDDHHMMSSSSS(userAgreementModel.validFromMillis)
            )

          case Some(fullPriceTable) ⇒
            fullPriceTable
        }

      case AdHocFullPriceTableRef(fullPriceTable) ⇒
        fullPriceTable
    }
  }

  def unsafeFullPriceTableForAgreement(
      userAgreement: UserAgreementMsg,
      knownPolicyModel: PolicyModel
  ): FullPriceTableMsg = {

    val policyModel = knownPolicyModel match {
      case null ⇒
        unsafeValidPolicyModelAt(userAgreement.getValidFromMillis)

      case policyModel ⇒
        policyModel
    }

    unsafeFullPriceTableForAgreement(userAgreement, policyModel.msg)
  }

  def unsafeFullPriceTableForAgreement(
     userAgreement: UserAgreementMsg,
     knownPolicy: PolicyMsg
  ): FullPriceTableMsg = {
    val policy = knownPolicy match {
      case null ⇒
        unsafeValidPolicyAt(userAgreement.getValidFromMillis)

      case policy ⇒
        policy
    }

    val role = userAgreement.getRole
    userAgreement.getFullPriceTableRef match {
      case null ⇒
        policy.getRoleMapping.get(role) match {
          case null ⇒
            throw new AquariumInternalError("Unknown role %s while computing full price table for user %s at %s",
              role,
              userAgreement.getUserID,
              TimeHelpers.toYYYYMMDDHHMMSSSSS(userAgreement.getValidFromMillis)
            )

          case fullPriceTable ⇒
            fullPriceTable
        }

      case fullPriceTable ⇒
        fullPriceTable
    }
 }

  /**
   * Computes the initial user agreement for the given role and reference time. Also,
   * records the ID from a potential related IMEvent.
   *
   * @param imEvent       The IMEvent that creates the user
   */
  def initialUserAgreement(imEvent: IMEventMsg): UserAgreementModel = {
    require(MessageHelpers.isIMEventCreate(imEvent))

    val role = imEvent.getRole
    val referenceTimeMillis = imEvent.getOccurredMillis

    // Just checking
    assert(null ne unsafeFullPriceTableModelForRoleAt(role, referenceTimeMillis))

    ModelFactory.newUserAgreementModelFromIMEvent(imEvent)
  }

  def initialUserBalance(role: String, referenceTimeMillis: Long): Real = {
    // FIXME: Where is the mapping?
    Real.Zero
  }

  def chargingBehaviorOf(resourceType: ResourceTypeMsg): ChargingBehavior = {
    // A resource type never changes charging behavior. By definition.
    val className = resourceType.getChargingBehaviorClass
    _chargingBehaviorMap.get(className) match {
      case Some(chargingBehavior) ⇒
        chargingBehavior

      case _ ⇒
        try {
          _chargingBehaviorMap synchronized {
            val chargingBehavior = newInstance[ChargingBehavior](className)
            _chargingBehaviorMap = _chargingBehaviorMap.updated(className, chargingBehavior)
            chargingBehavior
          }
        }
        catch {
          case e: Exception ⇒
            throw new AquariumInternalError("Could not load charging behavior %s".format(className), e)
        }
    }
  }

  def defaultPolicyMsg = apply(EnvKeys.defaultPolicyMsg)

  def defaultClassLoader = apply(EnvKeys.defaultClassLoader)

  def resourceEventStore = apply(EnvKeys.storeProvider).resourceEventStore

  def imEventStore = apply(EnvKeys.storeProvider).imEventStore

  def userStateStore = apply(EnvKeys.storeProvider).userStateStore

  def policyStore = this.cachingPolicyStore

  def eventsStoreFolder = apply(EnvKeys.eventsStoreFolder)

  def eventBus = apply(EnvKeys.eventBus)

  def chargingService = apply(EnvKeys.chargingService)

  def userStateTimestampThreshold = apply(EnvKeys.userStateTimestampThreshold)

  def adminCookie = apply(EnvKeys.adminCookie)

  def converters = apply(EnvKeys.converters)

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
    override def toString = "%s(%s)".format(manifest[T], name)
  }

  final val ServiceKeys: List[TypedKey[_ <: Lifecycle]] = List(
    EnvKeys.timerService,
    EnvKeys.akkaService,
    EnvKeys.eventBus,
    EnvKeys.restService,
    EnvKeys.rabbitMQService,
    EnvKeys.storeWatcherService,
    EnvKeys.rabbitMQProducer
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
     */
    final val restPort = IntKey("rest.port")

    final val restShutdownTimeoutMillis = LongKey("rest.shutdown.timeout.millis")

    /**
     * A cookie used in every administrative REST API call, so that Aquarium knows it comes from
     * an authorised client.
     */
    final val adminCookie: TypedKey[Option[String]] =
      new AquariumEnvKey[Option[String]]("admin.cookie")

    /**
     * The class that initializes the REST service
     */
    final val restService: TypedKey[Lifecycle] =
      new AquariumEnvKey[Lifecycle]("rest.service.class")

    final val akkaService: TypedKey[AkkaService] =
      new AquariumEnvKey[AkkaService]("akka.service")

    final val eventBus: TypedKey[EventBusService] =
      new AquariumEnvKey[EventBusService]("event.bus.service")

    final val timerService: TypedKey[TimerService] =
      new AquariumEnvKey[TimerService]("timer.service")

    final val rabbitMQService: TypedKey[RabbitMQService] =
      new AquariumEnvKey[RabbitMQService]("rabbitmq.service")

    final val rabbitMQProducer: TypedKey[RabbitMQProducer] =
      new AquariumEnvKey[RabbitMQProducer]("rabbitmq.client")

    final val storeWatcherService: TypedKey[StoreWatcherService] =
      new AquariumEnvKey[StoreWatcherService]("store.watcher.service")

    final val converters: TypedKey[Converters] =
      new AquariumEnvKey[Converters]("converters")

    final val chargingService: TypedKey[ChargingService] =
      new AquariumEnvKey[ChargingService]("charging.service")

    final val defaultClassLoader: TypedKey[ClassLoader] =
      new AquariumEnvKey[ClassLoader]("default.class.loader")

    final val defaultPolicyMsg: TypedKey[PolicyMsg] =
      new AquariumEnvKey[PolicyMsg]("default.policy.msg")
  }
}
