enablePlugins(ScalaJSPlugin)

name := "ReactivePing"

version := "1.0"

scalaVersion := "2.11.6"

compileOrder := CompileOrder.JavaThenScala

scalacOptions in ThisBuild := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8", "-Xlint")

// (fork in Test) := true

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oS")

resolvers += "spray repo" at "http://repo.spray.io"

testFrameworks += new TestFramework("utest.runner.Framework")

scalaJSStage in Global := FastOptStage

libraryDependencies ++= {
  val akkaVersion = "2.3.11"
  val sprayVersion = "1.3.1"
  val slf4jVersion = "1.7.12"
  Seq(
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.+"
    ,"org.scala-lang.modules" %% "scala-async" % "0.9.+"

    ,"io.spray" %% "spray-can" % sprayVersion
    ,"io.spray" %% "spray-routing" % sprayVersion

    ,"com.lihaoyi" %%% "scalatags" % "0.5.2"
    ,"com.lihaoyi" %%% "utest" % "0.3.1"
    ,"org.scala-js" %%% "scalajs-dom" % "0.8.1"

    ,"com.typesafe.scala-logging" %% "scala-logging" % "3.1.+"
    ,"org.slf4j" % "slf4j-api" % slf4jVersion
    ,"org.slf4j" % "slf4j-simple" % slf4jVersion

    ,"net.java.dev.jna" % "jna-platform" % "4.1.0"

    ,"com.typesafe.akka" %% "akka-actor" % akkaVersion
    ,"com.typesafe.akka" %% "akka-persistence-experimental" % akkaVersion
    ,"com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"

    ,"com.h2database" % "h2" % "1.4.187"

    ,"org.scalatest" %% "scalatest" % "2.2.+" % "test"
  )
}

// workbenchSettings

Revolver.settings

// updateBrowsers <<= updateBrowsers.triggeredBy(fastOptJS in Compile)
