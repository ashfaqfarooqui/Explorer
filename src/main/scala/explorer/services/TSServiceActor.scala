package explorer.services

import java.util.UUID

import explorer.services.simPhysical.SetOfOpsProtocol

import akka.actor.{Props, Actor}
import base.{FlowerPopulater, TextFilePrefix, ReadFromWmodFileModuleFactory}
import explorer.config.{SimulatePhysicalSystem, ProjectConfigTrait}
import exploring.StateTransExploring
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol

case class TSServiceActor() extends Actor with StateTransExploring with FlowerPopulater with ProjectConfigTrait {
  val log = LoggerFactory.getLogger(classOf[TSServiceActor])

  def receive = {
    case GetActive_TSServiceMessage => sender ! activeModelOnServer
    case UpdateStateBasedOnTransition_TSMessage(t) => sender ! updateBasedOnTransition(t)
    case UpdateStateBasedOnState_TSMessage(s) => sender ! updateBasedOnState(s)
    case CheckStateBasedOnState_TSMessage(s) => sender ! checkState(s).toString
    case GetExeOps_TSServiceMessage => sender ! executingOps()
  }

  val mModule = ReadFromWmodFileModuleFactory(projectConfigMap("tsWmodFile")).get

  var transitionCountMap = Map(): Map[String, Int]
  var activeTSStateOnServer = initState()
  var activeModelOnServer = updateModelOnServer()
  val supervisorAsBDD = SupervisorAsBDD()

  lazy val opMap = {
    getComment.split("\n").flatMap(str => parseAll(s"${TextFilePrefix.OPERATION_PREFIX}".r ~> s"(.(?!:))+".r ~ (s"${TextFilePrefix.TRANSITION_PREFIX}".r ~> s".*".r), str.replaceAll(" ", "")) match {
      case Success(~(name, data), _) => Some(name -> data.split(",").toSeq)
      case _ => None
    }).toMap
  }.map { case (k, v) => k -> v.toSeq }

  def activeTs() = getTsEnabledFromSpecificState(activeTSStateOnServer)

  def checkState(state : String) = supervisorAsBDD.containsState(parseStateJsonToStateMap(state))

  def updateBasedOnState(state: String): String = {
    val stateMap = parseStateJsonToStateMap(state)
    if (activeModelOnServer.variables.size == stateMap.size) {
      activeTSStateOnServer = stateMap.foldLeft(activeTSStateOnServer) { case (acc, (k, v)) => acc + (k -> v) }
      activeModelOnServer = updateModelOnServer()
      "ok"
    } else "nok"
  }

  case class LocalState(name: String, value: String)

  object LocalStateJsonProtocol extends DefaultJsonProtocol {
    implicit val lsFormat = jsonFormat2(LocalState)
  }

  def parseStateJsonToStateMap(state: String): Map[String, Int] = {
    import spray.json._
    import LocalStateJsonProtocol._

    val varDomains = activeModelOnServer.variables.map { case v => v.name -> v.domain }.toMap
    val stateAsMap = (for {
      ls <- state.parseJson.convertTo[List[LocalState]]
      domain <- varDomains.get(ls.name)
      index = domain.indexOf(ls.value)
      optIndex <- if (index >= 0) Some(index) else None
    } yield {
        ls.name -> optIndex
      }).toMap

    stateAsMap
  }

  def updateBasedOnTransition(transition: String): String = {
    val action = activeTs().get(transition)
    //"transition" was among active transitions :)
    if (action.isDefined) {
      //Count "transition" frequency
      transitionCountMap = transitionCountMap + (transition -> (transitionCountMap.getOrElse(transition, 0) + 1))
      //Update state and model on server
      activeTSStateOnServer = action.get(activeTSStateOnServer)
      activeModelOnServer = updateModelOnServer()

      if (SimulatePhysicalSystem.check()) {
        //Post operation to start on server simulating physical system
        if (!transition.startsWith(TextFilePrefix.UNCONTROLLABLE_PREFIX)) {
          context.actorOf(Props(new ClientToPostARequestOfTypeStringActor("0.0.0.0", 8000, "/api/opsIO")(transition))) ! PostTransition_TSServiceMessage
        }
      }

      return "ok"
    }
    //"transition" was not among active transitions :(
    "nok"
  }

  def executingOps() = SetOfOpsProtocol(activeModelOnServer.operations.filter(opMap => opMap("executing").equals(true.toString)).map(opMap => opMap("name")).toSet)

  def updateModelOnServer(): ModelOnServer = {
    val transitions = getTs.keySet.map {
      t => Map("name" -> t,
        "enabled" -> activeTs().keySet.contains(t).toString
      )
    }.toList
    val variables = getVariables.map {
      v => val fullName = v.getName
        val activeValue = unmaskState(activeTSStateOnServer).getOrElse(fullName, activeTSStateOnServer(fullName).toString)
        VariableOnServer(
          name = fullName,
          nameToShow = if (fullName.startsWith("v")) fullName.substring(1) else fullName,
          activeValue = activeValue,
          domain = getTextForVariableValuesFromModuleComments().getOrElse(fullName, Seq(v.getType.toString)).toArray,
          initValue = unmaskState(initState()).getOrElse(fullName, initState()(fullName).toString),
          marking = evalMarking(v)(Map(fullName -> activeTSStateOnServer(fullName))).toString,
          setToValue = activeValue
        )
    }.toList
    val operations = opMap.map { case (op, ts) =>
        Map("name" -> op,
          "startable" -> activeTs().keySet.contains(ts.head).toString,
          "executing" -> (transitionCountMap.getOrElse(ts.head,0)>transitionCountMap.getOrElse(ts.last,0)).toString,
          "nbrOfExecutions" -> transitionCountMap.getOrElse(ts.last, 0).toString,
          "startTrans" -> ts.head
        )
    }.toList
    ModelOnServer(transitions = transitions, variables = variables, operations = operations)
  }
}

trait TSServiceMessage

case object GetActive_TSServiceMessage extends TSServiceMessage

case class UpdateStateBasedOnTransition_TSMessage(transition: String) extends TSServiceMessage

case class UpdateStateBasedOnState_TSMessage(state: String) extends TSServiceMessage

case class CheckStateBasedOnState_TSMessage(state: String) extends TSServiceMessage

case object PostTransition_TSServiceMessage extends TSServiceMessage

case object GetExeOps_TSServiceMessage extends TSServiceMessage

case class IsStateReachable_TSMessage(state: Map[String, Int]) extends TSServiceMessage

case class VariableOnServer(name: String, nameToShow: String, activeValue: String, domain: Array[String], initValue: String, marking: String, setToValue: String)

case class ModelOnServer(id: String = UUID.randomUUID().toString,
                         transitions: List[Map[String, String]] = List(Map()),
                         variables: List[VariableOnServer],
                         operations: List[Map[String, String]] = List(Map())) {
  override def toString = transitions.mkString("\n")
}

object ModelOnServerJsonProtocol extends DefaultJsonProtocol {
  implicit val vosFormat = jsonFormat7(VariableOnServer)
  implicit val tsFormat = jsonFormat4(ModelOnServer)
}

