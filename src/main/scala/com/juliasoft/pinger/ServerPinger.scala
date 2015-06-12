package com.juliasoft.pinger

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import spray.can.Http

import scala.concurrent.duration._

/**
 * Spray Server App
 */
object ServerPinger extends App {
  implicit val timeout = Timeout(60.seconds)

  implicit val system = ActorSystem("pinger-on-spray-can")

  private val config = ConfigFactory.load()

  private val driver = config.getString("com.juliasoft.pinger.driver")
  private val url = config.getString("com.juliasoft.pinger.url")
  private val user = config.getString("com.juliasoft.pinger.user")
  private val password = config.getString("com.juliasoft.pinger.password")

  val storage = system.actorOf(Props(classOf[DbActor], driver, url, user, password))
  val manager = system.actorOf(Props(classOf[Manager], storage))

  val service = system.actorOf(Props(classOf[PingerServiceActor], manager), "pinger-service")

  IO(Http) ? Http.Bind(service, interface = "localhost", port = 8080)
}

