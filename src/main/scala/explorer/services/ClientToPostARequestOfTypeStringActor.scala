package explorer.services

import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor.{Actor, ActorSystem}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.http._
import spray.can.Http
import HttpMethods._

import scala.util.{Failure, Success}

trait ClientToPostARequestOfTypeStringTrait {
  private implicit val timeout: Timeout = 3.seconds

  val host: String
  val port: Int
  val uri: String

  def postString(str: String)(implicit system: ActorSystem): Future[HttpResponse] = {
    import system.dispatcher // execution context for future transformation below
    for {
      Http.HostConnectorInfo(hostConnector, _) <- IO(Http) ? Http.HostConnectorSetup(host = host, port = port, sslEncryption = false)
      response <- hostConnector.ask(HttpRequest(method = POST, uri = uri, entity = str)).mapTo[HttpResponse]
      _ <- IO(Http) ? Http.CloseAll
    } yield {
      response
    }
  }
}

case class ClientToPostARequestOfTypeStringActor(host: String = "0.0.0.0", port: Int, uri: String)(transition: String) extends Actor with ClientToPostARequestOfTypeStringTrait {
  val log = LoggerFactory.getLogger(classOf[ClientToPostARequestOfTypeStringActor])

  implicit val system = ActorSystem("simple-client")
  import system.dispatcher  // execution context for future transformations below

  def receive = {
    case PostTransition_TSServiceMessage => {
      postString(transition) onComplete {
        case Success(res) => log.debug(s"Client received: $res")
        case Failure(error) => log.error(s"Client error: $error")
      }
      context stop self
    }
  }
}


