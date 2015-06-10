package com.juliasoft.pinger

import akka.actor._
import com.juliasoft.pinger.Model.Task
import com.juliasoft.pinger.StorageActor.CreateTask
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

case class DbActor(driver: String, url: String, user: String, pass: String) extends Db(driver, url, user, pass) with StorageActor

/**
 * Console Pinger application
 */
object ConsolePinger extends App {
  private val config = ConfigFactory.load()

  private val driver = config.getString("com.juliasoft.pinger.driver")
  private val url = config.getString("com.juliasoft.pinger.url")
  private val user = config.getString("com.juliasoft.pinger.user")
  private val password = config.getString("com.juliasoft.pinger.password")

  val googleTask = Task(1, "N", "Google", "www.google.com", 10, true)
  val system = ActorSystem("ConsolePinger")
  val storage = system.actorOf(Props(classOf[DbActor], driver, url, user, password))
  val manager = system.actorOf(Props(classOf[Manager], storage))

  // stop this sample after 1 minute
  system.scheduler.scheduleOnce(35.seconds, new Runnable {
    override def run(): Unit = {
      system stop storage
      system.shutdown()
    }
  })

  manager ! CreateTask(googleTask)
}
