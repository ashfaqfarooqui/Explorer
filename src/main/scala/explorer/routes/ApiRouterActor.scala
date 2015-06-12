package explorer.routes

import akka.actor.{Actor, ActorRef}
import spray.routing.HttpService

/**
 * Routes the incoming request.  If the route begins with "api" the request is passed
 * along to the matching spray routing actor (if there's a match)
 *
 * Other routes are assumed to be static resources and are served from the resource
 * directory on the classpath.  getFromDirectory takes the remainder of the path
 * so a route like "index.html" is completed with the classpath resource "dist/index.html"
 * or returns a 404 if it's not found.
 *
 * To run the front end app in dev mode change "dist" to "app"
 */
class ApiRouterActor(transitionSystemRouter: ActorRef) extends Actor with HttpService {

  def actorRefFactory = context

  def receive = runRoute {
    pathPrefix("api") {
       ctx => transitionSystemRouter ! ctx
    } ~
      getFromDirectory("./app/")
  }

}
