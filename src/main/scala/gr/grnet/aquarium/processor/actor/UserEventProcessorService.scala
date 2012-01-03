package gr.grnet.aquarium.processor.actor

import gr.grnet.aquarium.messaging.MessagingNames
import gr.grnet.aquarium.logic.events.{UserEvent, AquariumEvent}
import com.ckkloverdos.maybe.{NoVal, Failed, Just}


/**
 * An event processor service for user events coming from the IM system
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class UserEventProcessorService extends EventProcessorService {

  override def decode(data: Array[Byte]) = UserEvent.fromBytes(data)

  override def forward(resourceEvent: AquariumEvent) {}

  override def exists(event: AquariumEvent) =
    _configurator.userEventStore.findUserEventById(event.id).isJust

  override def persist(evt: AquariumEvent) = {
    val event = evt.asInstanceOf[UserEvent]
    _configurator.userEventStore.storeUserEvent(event) match {
      case Just(x) => true
      case x: Failed =>
        logger.error("Could not save user event: %s".format(event))
        false
      case NoVal => false
    }
  }

  override def queueReaderThreads: Int = 4
  override def persisterThreads: Int = numCPUs
  override def name = "usrevtproc"

  lazy val persister = new PersisterManager
  lazy val queueReader = new QueueReaderManager

  override def persisterManager   = persister
  override def queueReaderManager = queueReader

  def start() {
    logger.info("Starting user event processor service")

    consumer("%s.#".format(MessagingNames.IM_EVENT_KEY),
      MessagingNames.IM_EVENT_QUEUE, MessagingNames.IM_EXCHANGE,
      queueReaderManager.lb, false)
  }

  def stop() {
    queueReaderManager.stop()
    persisterManager.stop()

    logger.info("Stopping user event processor service")
  }
}