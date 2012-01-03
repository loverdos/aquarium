package gr.grnet.aquarium.messaging

/**
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
object MessagingNames {

  def AQUARIUM_EXCHANGE = "aquarium"
  def IM_EXCHANGE = "im"

  def RES_EVENT_KEY = "resevent"
  def IM_EVENT_KEY = "imevent"

  def RESOURCE_EVENT_QUEUE = "resource-events"
  def IM_EVENT_QUEUE = "im-events"
  def TEST_QUEUE = "akka-amqp-test"
}