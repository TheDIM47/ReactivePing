package apps

import java.net.URI

import actors.StorageCommands.CreateTask
import akka.actor._
import actors.{ResultStorageActor, TaskManager, TaskStorageActor}
import model.{PingNative, Task}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Console Ping google.com each 5 seconds
 */
object ConsolePinger extends App {
  private val config = ConfigFactory.load()

  private val driver = config.getString("com.juliasoft.pinger.driver")
  private val url = config.getString("com.juliasoft.pinger.url")
  private val user = config.getString("com.juliasoft.pinger.user")
  private val password = config.getString("com.juliasoft.pinger.password")

  val system = ActorSystem("ConsolePingerSystem")
  val taskStorage = system.actorOf(Props(classOf[TaskStorageActor], driver, url, user, password), "task-storage")
  val resultStorage = system.actorOf(Props(classOf[ResultStorageActor], driver, url, user, password), "result-storage")
  val manager = system.actorOf(Props(classOf[TaskManager], resultStorage), "task-manager")

  val googleTask = Task(1, PingNative, "Google", new URI("www.google.com"), 5.seconds, true)
  taskStorage ! CreateTask(googleTask)
  manager ! googleTask

  // stop this sample after 1 minute
  system.scheduler.scheduleOnce(20.seconds, new Runnable {
    override def run(): Unit = {
      manager ! PoisonPill
      taskStorage ! PoisonPill
      resultStorage ! PoisonPill
      system.terminate()
    }
  })
}
