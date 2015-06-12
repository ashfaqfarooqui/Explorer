package explorer.routes

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import explorer.services._
import org.slf4j.LoggerFactory
import spray.httpx.SprayJsonSupport
import spray.routing.HttpService

import scala.concurrent.Future

class TSRouterActor(tsService: ActorRef) extends Actor with HttpService with SprayJsonSupport {
  val log = LoggerFactory.getLogger(classOf[TSRouterActor])

  def actorRefFactory = context

  implicit val timeout = Timeout(8000)

  import context.dispatcher
  import explorer.services.ModelOnServerJsonProtocol._

  def receive = runRoute {
    pathPrefix("ts") {
      pathEndOrSingleSlash {
        get {
          complete {
            //          log.debug("Web page checks for a new active protocol")
            (tsService ? GetActive_TSServiceMessage).mapTo[ModelOnServer]
          }
        }
      } ~ pathPrefix("transition") {
        pathEndOrSingleSlash {
          post {
            entity(as[String]) { t =>
              complete {
                log.debug(s"Server got transition $t")
                (tsService ? UpdateStateBasedOnTransition_TSMessage(t)).mapTo[String]
              }
            }
          }
        }
      } ~ pathPrefix("state") {
        pathEndOrSingleSlash {
          post {
            entity(as[String]) { s =>
              complete {
//                log.debug(s"Server got state $s")
                (tsService ? UpdateStateBasedOnState_TSMessage(s)).mapTo[String]
              }
            }
          }
        }
      } ~ pathPrefix("stateToCheck") {
        pathEndOrSingleSlash {
          post {
            entity(as[String]) { s =>
//              log.debug(s"Server got state $s")
              complete {
                (tsService ? CheckStateBasedOnState_TSMessage(s)).mapTo[String]
              }
            }
          }
        }
      }
    }
  }

}
