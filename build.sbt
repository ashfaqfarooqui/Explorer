name := """explorer"""

organization  := "explorer"

version       := "0.1"

scalaVersion  := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature")

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io/"
)

libraryDependencies ++= {
  val akkaV = "2.2.3"
  val sprayV = "1.2.0"
  Seq(
    "io.spray"            %   "spray-servlet"     % sprayV,
    "io.spray"            %   "spray-routing"     % sprayV,
    "io.spray"            %   "spray-testkit"     % sprayV,
    "io.spray"            %   "spray-client"      % sprayV,
    "io.spray"            %   "spray-util"        % sprayV,
    "io.spray"            %   "spray-caching"     % sprayV,
    "io.spray"            %   "spray-can"         % sprayV,
    "io.spray"            %   "spray-client"      % sprayV,
    "io.spray"            %%  "spray-json"        % "1.3.1",
    "com.typesafe.akka"   %%  "akka-slf4j"        % "2.1.4",
    "ch.qos.logback"      %   "logback-classic"   % "1.0.13",
    "com.typesafe.akka"   %%  "akka-actor"        % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"      % akkaV,
    "org.specs2"          %%  "specs2"            % "2.2.3" % "test"
  )
}

Revolver.settings: Seq[sbt.Def.Setting[_]]
