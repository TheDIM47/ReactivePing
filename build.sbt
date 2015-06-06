name := "ReactivePing"

version := "1.0"

scalaVersion := "2.11.6"

compileOrder := CompileOrder.JavaThenScala

scalacOptions in ThisBuild := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8", "-Xlint")

libraryDependencies ++= Seq(
   "net.databinder.dispatch" %% "dispatch-core" % "0.11.+"
  ,"org.scala-lang.modules" %% "scala-async" % "0.9.+"

  ,"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
  ,"org.slf4j" % "slf4j-api" % "1.7.12"
  ,"org.slf4j" % "slf4j-simple" % "1.7.12"

  ,"net.java.dev.jna" % "jna-platform" % "4.1.0"

  ,"com.typesafe.akka" %% "akka-actor" % "2.3.11"
  ,"com.typesafe.akka" %% "akka-testkit" % "2.3.11"
  ,"com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.11"

  ,"junit" % "junit" % "4.12" % "test"
  ,"org.scalacheck" %% "scalacheck" % "1.12.+" % "test"
  ,"org.scalatest" %% "scalatest" % "2.2.+" % "test"
  ,"org.scalamock" %% "scalamock-scalatest-support" % "3.2.+" % "test"
)
