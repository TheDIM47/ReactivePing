logLevel := Level.Warn

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.3")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.2")

// addSbtPlugin("com.lihaoyi" % "workbench" % "0.2.3")
