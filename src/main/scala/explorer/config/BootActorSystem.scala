package explorer.config

import akka.actor.{Props, ActorSystem}
import akka.io.IO
import explorer.routes.simPhysical.{SimPhysicalApiRouterActor, SimPhysicalRouterActor}
import explorer.routes.{ApiRouterActor, TSRouterActor}
import explorer.services.TSServiceActor
import explorer.services.simPhysical.SimPhysicalServiceActor
import spray.can.Http

/**
 * Initializes one to two stand alone spray-can http servers:
 * One for the controller
 * And one optional for the simulated physical system.
 */
object BootActorSystem extends App {

  if (CheckOnStart.canStart) {
    implicit val system = ActorSystem("explorer")

    val tsServiceActor = system.actorOf(Props[TSServiceActor], "transitionSystem-service")
    val tsRouterActor = system.actorOf(Props(new TSRouterActor(tsServiceActor)), "transitionSystem-router")
    val apiRouterActor = system.actorOf(Props(new ApiRouterActor(tsRouterActor)), "api-router")

    IO(Http) ! Http.Bind(apiRouterActor, interface = "0.0.0.0", port = 8080)

    if (SimulatePhysicalSystem.check()) {
      val simPhysicalServiceActor = system.actorOf(Props[SimPhysicalServiceActor], "simPhysical-service")
      val simPhysicalRouterActor = system.actorOf(Props(new SimPhysicalRouterActor(simPhysicalServiceActor)), "simPhysical-router")
      val simPhysicalApiRouterActor = system.actorOf(Props(new SimPhysicalApiRouterActor(simPhysicalRouterActor)), "simPhysicalApi-router")

      IO(Http) ! Http.Bind(simPhysicalApiRouterActor, interface = "0.0.0.0", port = 8000)
    }
  }
}