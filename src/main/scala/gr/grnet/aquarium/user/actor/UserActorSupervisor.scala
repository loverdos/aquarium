package gr.grnet.aquarium.user.actor

import akka.actor.Supervisor
import akka.config.Supervision.SupervisorConfig
import akka.config.Supervision.OneForOneStrategy

/**
 * Supervisor for user actors
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
object UserActorSupervisor {

  lazy val supervisor = Supervisor(SupervisorConfig(
    OneForOneStrategy(
      List(classOf[Exception]), //What exceptions will be handled
      50, // maximum number of restart retries
      5000 // within time in millis
    ), Nil
  ))
}
