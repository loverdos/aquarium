package gr.grnet.aquarium.processor.actor

import com.ckkloverdos.maybe.{Just, Failed, NoVal}
import gr.grnet.aquarium.messaging.MessagingNames
import gr.grnet.aquarium.logic.events.ResourceEvent
import gr.grnet.aquarium.actor.DispatcherRole
import java.lang.ThreadLocal
import gr.grnet.aquarium.store.{ResourceEventStore}


/**
 * An event processor service for resource events
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
final class ResourceEventProcessorService extends EventProcessorService[ResourceEvent] {

  lazy val store = new ThreadLocal[ResourceEventStore]
  
  override def decode(data: Array[Byte]) = ResourceEvent.fromBytes(data)

  override def forward(event: ResourceEvent): Unit = {
    val businessLogicDispacther = _configurator.actorProvider.actorForRole(DispatcherRole)
    //businessLogicDispacther ! ProcessResourceEvent(event)
  }

  override def exists(event: ResourceEvent): Boolean =
    store.get match {
      case null => store.set(_configurator.resourceEventStore); return exists(event)
      case x => x.findResourceEventById(event.id).isJust
    }

  override def persist(event: ResourceEvent): Boolean = {
    val st = store.get match {
      case null => store.set(_configurator.resourceEventStore); return persist(event)
      case x => x
    }

    st.storeResourceEvent(event) match {
      case Just(x) => true
      case x: Failed =>
        logger.error("Could not save event: %s. Reason:".format(event, x.toString))
        false
      case NoVal => false
    }
  }

  override def queueReaderThreads: Int = 1
  override def persisterThreads: Int = numCPUs + 4
  override def numQueueActors: Int = 1 * queueReaderThreads
  override def numPersisterActors: Int = 2 * persisterThreads
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