package gr.grnet.aquarium.processor.actor

import gr.grnet.aquarium.logic.events.UserEvent
import com.ckkloverdos.maybe.{NoVal, Failed, Just}
import gr.grnet.aquarium.actor.DispatcherRole
import gr.grnet.aquarium.Configurator.Keys

/**
 * An event processor service for user events coming from the IM system
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class UserEventProcessorService extends EventProcessorService[UserEvent] {

  override def decode(data: Array[Byte]) = UserEvent.fromBytes(data)

  override def forward(event: UserEvent) =
    _configurator.actorProvider.actorForRole(DispatcherRole) ! ProcessUserEvent(event)

  override def exists(event: UserEvent) =
    _configurator.userEventStore.findUserEventById(event.id).isJust

  override def persist(event: UserEvent) = {
    _configurator.userEventStore.storeUserEvent(event) match {
      case Just(x) => true
      case x: Failed =>
        logger.error("Could not save user event: %s".format(event))
        false
      case NoVal => false
    }
  }

  override def queueReaderThreads: Int = 1
  override def persisterThreads: Int = numCPUs
  protected def numQueueActors = 2 * queueReaderThreads
  protected def numPersisterActors = 2 * persisterThreads
  override def name = "usrevtproc"

  lazy val persister = new PersisterManager
  lazy val queueReader = new QueueReaderManager

  override def persisterManager   = persister
  override def queueReaderManager = queueReader

  def start() {
    logger.info("Starting user event processor service")
    declareQueues(Keys.amqp_userevents_queues)
  }

  def stop() {
    queueReaderManager.stop()
    persisterManager.stop()

    logger.info("Stopping user event processor service")
  }
}