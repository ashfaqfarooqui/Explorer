package explorer.config

import spray.json.{DefaultJsonProtocol, JsonParser}
import scala.io.Source

trait ProjectConfigTrait extends DefaultJsonProtocol {
  lazy val projectConfigMap = JsonParser(Source.fromURL(getClass.getResource("/projectConfig.json")).getLines().mkString("\n")).convertTo[Map[String, String]]
}

case object CheckOnStart extends ProjectConfigTrait {
  private val checkResult = {
    for {
      filePath <- projectConfigMap.get("tsWmodFile")
    } yield new java.io.File(filePath).exists
  }

  def canStart = checkResult match {
    case Some(true) => true
    case _ => {
      println("Check your config files. I will not start..."); false
    }
  }
}

case object SimulatePhysicalSystem extends ProjectConfigTrait {
  def check() = {
    for {
      res <- projectConfigMap.get("simulatePhysicalSystem")
    } yield res
  } match {
    case Some("true") => true
    case Some("yes") => true
    case _ => false
  }
}