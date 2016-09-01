package actors

import actors.StorageCommands._
import akka.actor.{Actor, ActorLogging}
import api.{ResultStorageImpl, TaskStorageImpl}
//import model.Pinger.PingAck
import model.{Task, TaskResult}

object StorageCommands {

  /** Results (TaskResults - PingData) */
//  case class CreateResult(r: TaskResult)

  case class ListResults(taskId: Long)

  /** Results Answer */
  case class ResultAck(result: TaskResult)

  case class ResultListAck(results: Seq[TaskResult])

  /** Task */
  case object ListTasks

  case class GetTask(taskId: Long)

  case class CreateTask(task: Task)

  case class UpdateTask(task: Task)

  case class DeleteTask(taskId: Long)

  /** Task Answer */
  case class TaskAck(task: Option[Task])

  case class TaskListAck(tasks: Seq[Task])

  case class TaskDeletedAck(value: Int)
}

class ResultStorageActor(driver: String, url: String, user: String, pass: String)
  extends ResultStorageImpl(driver, url, user, pass) with Actor with ActorLogging {

  def receive: Receive = {
    case ListResults(taskId) =>
      log.debug(s"got messageListResults($taskId)")
      sender ! ResultListAck(listResults(taskId))
    case r : TaskResult =>
      log.debug(s"got message $r")
      createResult(r)
    case r =>
      log.debug(s"got unhandled message $r")
  }
}

/** Task */
class TaskStorageActor(driver: String, url: String, user: String, pass: String)
  extends TaskStorageImpl(driver, url, user, pass) with Actor with ActorLogging {

  def receive: Receive = {
    case ListTasks =>
      sender ! TaskListAck(listTasks)
    case GetTask(taskId) =>
      sender ! TaskAck(getTask(taskId))
    case CreateTask(task) =>
      sender ! TaskAck(Some(createTask(task)))
    case UpdateTask(task) =>
      sender ! TaskAck(Some(updateTask(task)))
    case DeleteTask(taskId) =>
      sender ! TaskDeletedAck(deleteTask(taskId))
  }
}
