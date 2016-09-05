package apps

import actors.StorageCommands._
import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import model.Task
import scala.concurrent.duration._
import svc.PingerJsonFormatSupport

import scala.util.Success

case class PingerRestService(taskStorage: ActorRef, resultStorage: ActorRef) extends Directives with PingerJsonFormatSupport {
  import akka.pattern.ask

  implicit val defaultTimeout = Timeout(5.seconds)

  val route: Route = get {
    path("task" / "list") {
      val res = taskStorage.ask(ListTasks)
      onSuccess(res) {
        case TaskListAck(tasks) => complete(tasks)
        case _ => complete(StatusCodes.NotFound)
      }
    }
  } ~ get {
    pathPrefix("task" / LongNumber) { id =>
      val res = taskStorage.ask(GetTask(id))
      onSuccess(res) {
        case TaskAck(task) => complete(task)
        case _ => complete(StatusCodes.NotFound)
      }
    }
  } ~ post {
    path("task") {
      entity(as[Task]) { task =>
        val res = taskStorage.ask(CreateTask(task))
        onComplete(res) { done =>
          complete("Task created")
        }
      }
    }
  } ~ put {
    path("task") {
      entity(as[Task]) { t =>
        val res = taskStorage.ask(UpdateTask(t))
        onComplete(res) {
          case Success(TaskAck(task)) => complete(task)
          case _ => complete(StatusCodes.BadRequest)
        }
      }
    }
  } ~ delete {
    pathPrefix("task" / LongNumber) { id =>
      val res = taskStorage.ask(DeleteTask(id))
      onComplete(res) { done =>
        complete("Task deleted")
      }
    }
  } ~ get {
    pathPrefix("result" / LongNumber) { id =>
      val res = resultStorage.ask(ListResults(id))
      onSuccess(res) {
        case ResultListAck(results) => complete(results)
        case _ => complete(StatusCodes.NotFound)
      }
    }
  }
}

