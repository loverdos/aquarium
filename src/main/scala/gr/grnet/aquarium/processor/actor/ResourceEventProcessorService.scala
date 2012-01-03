package gr.grnet.aquarium.processor.actor

import com.ckkloverdos.maybe.{Just, Failed, NoVal}
import gr.grnet.aquarium.actor.DispatcherRole
import gr.grnet.aquarium.messaging.MessagingNames
import gr.grnet.aquarium.logic.events.{AquariumEvent, ResourceEvent}


/**
 * An event processor service for resource events
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
final class ResourceEventProcessorService extends EventProcessorService {

  override def decode(data: Array[Byte]) : AquariumEvent = ResourceEvent.fromBytes(data)

  override def forward(evt: AquariumEvent): Unit = {
    val resourceEvent = evt.asInstanceOf[ResourceEvent]
    val businessLogicDispacther = _configurator.actorProvider.actorForRole(DispatcherRole)
    businessLogicDispacther ! ProcessResourceEvent(resourceEvent)
  }

  override def exists(event: AquariumEvent): Boolean =
    _configurator.resourceEventStore.findResourceEventById(event.id).isJust

  override def persist(evt: AquariumEvent): Boolean = {
    val event = evt.asInstanceOf[ResourceEvent]
    _configurator.resourceEventStore.storeResourceEvent(event) match {
      case Just(x) => true
      case x: Failed =>
        logger.error("Could not save event: %s".format(event))
        false
      case NoVal => false
    }
  }

  override def queueReaderThreads: Int = 1
  override def persisterThreads: Int = 2
  override def name = "resevtproc"

  def start() {
    logger.info("Starting resource event processor service")

    QueueReaderManager.start()
    PersisterManager.start()

    consumer("%s.#".format(MessagingNames.RES_EVENT_KEY),
      MessagingNames.RESOURCE_EVENT_QUEUE, MessagingNames.AQUARIUM_EXCHANGE,
      QueueReaderManager.lb, false)
  }

  def stop() {
    QueueReaderManager.stop()
    PersisterManager.stop()

    logger.info("Stopping resource event processor service")
  }
}