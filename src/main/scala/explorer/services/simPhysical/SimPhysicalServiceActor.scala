package explorer.services.simPhysical

import akka.actor.{Actor, Props}
import base.TextFilePrefix
import explorer.services.{PostTransition_TSServiceMessage, ClientToPostARequestOfTypeStringActor}
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol

import scala.util.Random

case class RandomComplete(timeToComplete: Int = (new Random()).nextInt(6000)) extends Actor {
  val log = LoggerFactory.getLogger(classOf[RandomComplete])
  def receive = {
    case StartOperation_SimPhysicalServiceMessage(op) => {
      log.debug(s"Time to complete operation $op: $timeToComplete")
      Thread.sleep(timeToComplete)
      sender ! CompletedOperation_SimPhysicalServiceMessage(op)
      context stop self
    }
  }
}

case class SimPhysicalServiceActor() extends Actor {
  val log = LoggerFactory.getLogger(classOf[SimPhysicalServiceActor])

  private var opsExecuting : Set[String] = Set()

  def receive = {
    case StartOperation_SimPhysicalServiceMessage(op) => {
      opsExecuting += op
      context.actorOf(Props(new RandomComplete())) ! StartOperation_SimPhysicalServiceMessage(op)
    }
    case CompletedOperation_SimPhysicalServiceMessage(op) => {
      //post on supervisor server
      log.debug(s"Physical system completed operation: $op")
      val transition = s"${TextFilePrefix.UNCONTROLLABLE_PREFIX}$op"
      context.actorOf(Props(new ClientToPostARequestOfTypeStringActor("0.0.0.0",8080,"/api/ts/transition")(transition))) !  PostTransition_TSServiceMessage
      opsExecuting -= op
    }
    case GetOpsExe_SimPhysicalServiceMessage => sender ! SetOfOpsProtocol(opsExecuting)
  }

}

trait SimPhysicalServiceMessage
case class StartOperation_SimPhysicalServiceMessage(op : String) extends SimPhysicalServiceMessage
case class CompletedOperation_SimPhysicalServiceMessage(op : String) extends SimPhysicalServiceMessage
case object GetOpsExe_SimPhysicalServiceMessage extends SimPhysicalServiceMessage

case class SetOfOpsProtocol(operations : Set[String] = Set() ) {
  override def toString() = operations.mkString("\n")
}

object SetOfOpsProtocolJsonProtocol extends DefaultJsonProtocol {
  implicit val tsFormat = jsonFormat1(SetOfOpsProtocol)
}