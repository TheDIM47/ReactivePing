package com.juliasoft.pinger

import java.net.URI
import java.sql.Timestamp

import akka.actor._
import akka.event.LoggingReceive
import com.juliasoft.pinger.Model.{Task, TaskResult}
import com.juliasoft.pinger.Pinger.{ErrorInfo, Ping, PingInfo}
import com.juliasoft.pinger.StorageActor._

/**
 * "Receptionist"
 */
object Manager {

  case class StartTask(t: Task)

  case class StopTask(t: Task)

  abstract case class Receiver(manager: ActorRef) extends ActorAnswer with Pinger with ActorLogging {
    override def receive: Receive = LoggingReceive {
      case Ping(uri) => ping(uri)
    }
  }

  class NativePinger(m: ActorRef) extends Receiver(m) with NativePing

  class HttpPinger(m: ActorRef) extends Receiver(m) with HttpPing

  class EchoPinger(m: ActorRef) extends Receiver(m) with ReachableEcho

  class FuturePinger(m: ActorRef) extends Receiver(m) with ReachableFuture

}

case class Manager(storage: ActorRef) extends Actor with ActorLogging {
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  import Manager._

  // Task.ID -> ActorRef map
  var taskMap = Map.empty[ActorRef, Task]
  var schedulers = Map.empty[ActorRef, Cancellable]

  // Ask storage for tasks
  storage ! ListTasks

  override def receive: Receive = LoggingReceive {
    case TaskListAck(tasks) => tasks.filter(_.active).foreach(t => self ! StartTask(t))

    case info: PingInfo =>
      taskMap.get(sender).foreach(task => {
        self ! TaskResult(task.id, new Timestamp(info.start.getTime), Some(info.rtt), 1, None)
//        self ! StopTask(task)
      })

    case err: ErrorInfo =>
      taskMap.get(sender).foreach(task => {
        self ! TaskResult(task.id, new Timestamp(err.start.getTime), None, 0, Some(err.msg))
//        self ! StopTask(task)
      })

    /** Task(Ping) result */
    case tr: TaskResult => storage ! CreateResult(tr)

    case r: ResultAck => () // task result stored confirmation

    /** Create, Update, Delete Task */
    case CreateTask(task) => storage ! CreateTask(task)

    case UpdateTask(task) =>
      storage ! UpdateTask(task)
      stopTask(task)

    case DeleteTask(taskId) =>
      storage ! DeleteTask(taskId)

    /** C(R)UD Task Confirmations */
    case TaskAck(task) => task match {
      case Some(t) => if (t.active) self ! StartTask(t)
      case _ => ()
    }

    case TaskDeletedAck(taskId) => stopTask(taskId)

    /** Task operations */
    case StartTask(t) => startTask(t)

    case StopTask(t) => stopTask(t)
  }

  def startTask(task: Task): Unit = {
    val actor = task.method match {
      case "N" => context.system.actorOf(Props(classOf[NativePinger], self))
      case "H" => context.system.actorOf(Props(classOf[HttpPinger], self))
      case "E" => context.system.actorOf(Props(classOf[EchoPinger], self))
      case "F" => context.system.actorOf(Props(classOf[FuturePinger], self))
    }
    taskMap += actor -> task
    schedulers += actor -> context.system.scheduler.schedule( 0.seconds, task.period.seconds, actor, Ping(new URI(task.url)) )
//    actor ! Ping(new URI(task.url))
  }

  def stopTask(task: Task): Unit = stopTask(task.id)

  def stopTask(taskId: Long): Unit = {
    val actors = taskMap.filter(_._2.id == taskId)
    taskMap = taskMap.filterNot(a => actors.contains(a._1))
    actors.foreach(a => {
      val actor = a._1
      schedulers.get(actor).foreach(_.cancel())
      schedulers -= actor
      actor ! PoisonPill
    })
  }

}

