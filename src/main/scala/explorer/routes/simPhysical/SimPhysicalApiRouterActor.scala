package explorer.routes.simPhysical

import akka.actor.{Actor, ActorRef}
import spray.routing.HttpService

class SimPhysicalApiRouterActor(af: ActorRef) extends Actor with HttpService {

  def actorRefFactory = context

  def receive = runRoute {
    pathPrefix("api") {
       ctx => af ! ctx
    }
  }

}
