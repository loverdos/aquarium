package gr.grnet.aquarium.processor.actor

import com.ckkloverdos.maybe.{Just, Failed, NoVal}
import gr.grnet.aquarium.messaging.MessagingNames
import gr.grnet.aquarium.logic.events.ResourceEvent
import gr.grnet.aquarium.actor.DispatcherRole


/**
 * An event processor service for resource events
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
final class ResourceEventProcessorService extends EventProcessorService[ResourceEvent] {

  override def decode(data: Array[Byte]) = ResourceEvent.fromBytes(data)

  override def forward(event: ResourceEvent): Unit = {
    val businessLogicDispacther = _configurator.actorProvider.actorForRole(DispatcherRole)
    businessLogicDispacther ! ProcessResourceEvent(event)
  }

  override def exists(event: ResourceEvent): Boolean =
    _configurator.resourceEventStore.findResourceEventById(event.id).isJust

  override def persist(event: ResourceEvent): Boolean = {
    _configurator.resourceEventStore.storeResourceEvent(event) match {
      case Just(x) => true
      case x: Failed =>
        logger.error("Could not save event: %s. Reason:".format(event, x.toString))
        false
      case NoVal => false
    }
  }

  override def queueReaderThreads: Int = 4
  override def persisterThreads: Int = numCPUs
  override def name = "resevtproc"

  lazy val persister = new PersisterManager
  lazy val queueReader = new QueueReaderManager

  override def persisterManager   = persister
  override def queueReaderManager = queueReader

  def start() {
    logger.info("Starting resource event processor service")

    consumer("%s.#".format(MessagingNames.RES_EVENT_KEY),
      MessagingNames.RESOURCE_EVENT_QUEUE, MessagingNames.AQUARIUM_EXCHANGE,
      queueReaderManager.lb, false)
  }

  def stop() {
    queueReaderManager.stop()
    persisterManager.stop()

    logger.info("Stopping resource event processor service")
  }
}