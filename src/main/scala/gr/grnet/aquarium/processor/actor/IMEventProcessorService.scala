package gr.grnet.aquarium.processor.actor

import gr.grnet.aquarium.messaging.MessagingNames
import gr.grnet.aquarium.logic.events.AquariumEvent

/**
 * An event processor service for IM events
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
final class IMEventProcessorService extends EventProcessorService {

  override def decode(data: Array[Byte]) = null

  override def forward(resourceEvent: AquariumEvent) {}

  override def exists(event: AquariumEvent) = false

  override def persist(event: AquariumEvent) = false

  override def queueReaderThreads: Int = 1
  override def persisterThreads: Int = 2
  override def name = "imevtproc"

  override def persisterManager = new PersisterManager
  override def queueReaderManager = new QueueReaderManager

  def start() {
    logger.info("Starting IM event processor service")

    consumer("%s.#".format(MessagingNames.IM_EVENT_KEY),
      MessagingNames.IM_EVENT_QUEUE, MessagingNames.IM_EXCHANGE,
      queueReaderManager.lb, false)
  }

  def stop() {
    queueReaderManager.stop()
    persisterManager.stop()

    logger.info("Stopping IM event processor service")
  }
}