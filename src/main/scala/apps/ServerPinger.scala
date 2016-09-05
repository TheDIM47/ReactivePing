package apps

import actors.StorageCommands._
import actors.{ResultStorageActor, TaskManager, TaskStorageActor}
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.io.StdIn

/**
  * Pinger as Service
  */
object ServerPinger extends App {
  private val config = ConfigFactory.load()

  private val driver = config.getString("com.juliasoft.pinger.driver")
  private val url = config.getString("com.juliasoft.pinger.url")
  private val user = config.getString("com.juliasoft.pinger.user")
  private val password = config.getString("com.juliasoft.pinger.password")

  implicit val system = ActorSystem("ServerPingerSystem")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val defaultTimeout = Timeout(5.seconds)

  val taskStorage = system.actorOf(Props(classOf[TaskStorageActor], driver, url, user, password), "task-storage")
  val resultStorage = system.actorOf(Props(classOf[ResultStorageActor], driver, url, user, password), "result-storage")
  val manager = system.actorOf(Props(classOf[TaskManager], resultStorage), "task-manager")

  // Load list of active tasks and start pinging
  taskStorage.ask(ListTasks).onSuccess({
    case TaskListAck(tasks) => tasks.filter(_.active).foreach(task => manager ! task)
  })

  val host = "localhost"
  val port = 8080
  val bindingFuture = Http().bindAndHandle(PingerRestService(taskStorage, resultStorage).route, host, port)
  println(s"Server online at http://$host:$port/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
}
