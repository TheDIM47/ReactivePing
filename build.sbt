name := "ReactivePing"

version := "1.0"

scalaVersion := "2.11.8"

compileOrder := CompileOrder.JavaThenScala

scalacOptions in ThisBuild := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8", "-Xlint")

(fork in Test) := true

libraryDependencies ++= {
  val akkaVersion = "2.4.9"
  val dispatchVersion = "0.11.3"
  val slf4jVersion = "1.7.21"
  val h2Version = "1.4.192"
  val jnaVersion = "4.2.2"
  Seq(
     "com.typesafe.akka" %% "akka-actor" % akkaVersion

    ,"net.java.dev.jna" % "jna-platform" % jnaVersion

    ,"net.databinder.dispatch" %% "dispatch-core" % dispatchVersion

    ,"org.slf4j" % "slf4j-api" % slf4jVersion
    ,"com.h2database" % "h2" % h2Version

    ,"com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
    ,"org.scalatest" %% "scalatest" % "3.0.0" % Test
  )
}

