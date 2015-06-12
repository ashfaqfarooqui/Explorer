package explorer.routes.simPhysical

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import explorer.services.simPhysical._
import org.slf4j.LoggerFactory
import spray.httpx.SprayJsonSupport
import spray.routing.HttpService

class SimPhysicalRouterActor(tsCompOpsService: ActorRef) extends Actor with HttpService with SprayJsonSupport {
  val log = LoggerFactory.getLogger(classOf[SimPhysicalRouterActor])

  def actorRefFactory = context

  implicit val timeout = Timeout(3000)

  import context.dispatcher
  import SetOfOpsProtocolJsonProtocol._

  def receive = runRoute {
    pathPrefix("opsIO") {
      get {
        complete {
          (tsCompOpsService ? GetOpsExe_SimPhysicalServiceMessage).mapTo[SetOfOpsProtocol]
        }
      } ~ post {
        entity(as[String]) { op =>
          complete {
            log.debug(s"Sim of physical system started operation: $op")
            tsCompOpsService ! StartOperation_SimPhysicalServiceMessage(op)
            "ok"
          }
        }
      }
    }
  }

}
