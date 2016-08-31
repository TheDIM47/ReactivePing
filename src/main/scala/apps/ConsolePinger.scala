package apps

import java.net.URI

import akka.actor._
import actors.{ResultStorageActor, TaskManager, TaskStorageActor}
import model.{PingNative, Task}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

//case class DbApiActor(driver: String, url: String, user: String, pass: String) extends DbApi(driver, url, user, pass) with StorageActor

/**
 * Console Pinger application
 */
object ConsolePinger extends App {
  private val config = ConfigFactory.load()

  private val driver = config.getString("com.juliasoft.pinger.driver")
  private val url = config.getString("com.juliasoft.pinger.url")
  private val user = config.getString("com.juliasoft.pinger.user")
  private val password = config.getString("com.juliasoft.pinger.password")

  val googleTask = Task(1, PingNative, "Google", new URI("www.google.com"), 10.seconds, true)
  val system = ActorSystem("ConsolePingerSystem")
//  val taskStorage = system.actorOf(Props(classOf[TaskStorageActor], driver, url, user, password), "task-storage")
  val resultStorage = system.actorOf(Props(classOf[ResultStorageActor], driver, url, user, password), "result-storage")
  val manager = system.actorOf(Props(classOf[TaskManager], resultStorage), "task-manager")

  // stop this sample after 1 minute
  system.scheduler.scheduleOnce(60.seconds, new Runnable {
    override def run(): Unit = {
//      system.stop(taskStorage)
      system.stop(resultStorage)
      system.terminate()
    }
  })

  manager ! googleTask
}
