package com.juliasoft.pinger

import akka.actor.Actor
import com.juliasoft.pinger.Model.{Task, TaskResult}

object StorageActor {

  /** Results (TaskResults - PingData) */
  case class CreateResult(r: TaskResult)

  case class ListResults(taskId: Long)

  /** Results Answer */
  case class ResultAck(result: TaskResult)

  case class ResultListAck(results: List[TaskResult])

  /** Task */
  case object ListTasks

  case class GetTask(taskId: Long)

  case class CreateTask(task: Task)

  case class UpdateTask(task: Task)

  case class DeleteTask(taskId: Long)

  /** Task Answer */
  case class TaskAck(task: Option[Task])

  case class TaskListAck(tasks: List[Task])

  case class TaskDeletedAck(value: Int)
}

trait Storage {
  /** PingData (TaskResult) */
  def createResult(tr: TaskResult): TaskResult

  def listResults(taskId: Long): List[TaskResult]

  /** Task */
  def listTasks: List[Task]

  def getTask(taskId: Long): Option[Task]

  def createTask(task: Task): Task

  def updateTask(task: Task): Task

  def deleteTask(taskId: Long): Int
}

trait StorageActor extends Storage with Actor {

  import com.juliasoft.pinger.StorageActor._

  def receive: Receive = {
    case ListResults(taskId) => sender ! ResultListAck(listResults(taskId))
    case CreateResult(r) => sender ! ResultAck(createResult(r))
    //
    case ListTasks => sender ! TaskListAck(listTasks)
    case GetTask(taskId) => sender ! TaskAck(getTask(taskId))
    case CreateTask(task) => sender ! TaskAck(Some(createTask(task)))
    case UpdateTask(task) => sender ! TaskAck(Some(updateTask(task)))
    case DeleteTask(taskId) => sender ! TaskDeletedAck(deleteTask(taskId))
  }
}
