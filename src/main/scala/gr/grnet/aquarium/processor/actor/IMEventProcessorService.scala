package gr.grnet.aquarium.processor.actor

import gr.grnet.aquarium.messaging.MessagingNames
import gr.grnet.aquarium.logic.events.AquariumEvent

/**
 * An event processor service for IM events
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
class IMEventProcessorService extends EventProcessorService {

  protected def decode(data: Array[Byte]) = null

  protected def forward(resourceEvent: AquariumEvent) {}

  protected def exists(event: AquariumEvent) = false

  protected def persist(event: AquariumEvent) = false

  override def queueReaderThreads: Int = 1
  override def persisterThreads: Int = 2
  override def name = "imevtproc"

  def start() {
    logger.info("Starting IM event processor service")

    QueueReaderManager.start()
    PersisterManager.start()

    consumer("%s.#".format(MessagingNames.IM_EVENT_KEY),
      MessagingNames.IM_EVENT_QUEUE, MessagingNames.IM_EXCHANGE,
      QueueReaderManager.lb, false)
  }

  def stop() {
    QueueReaderManager.stop()
    PersisterManager.stop()

    logger.info("Stopping IM event processor service")
  }
}