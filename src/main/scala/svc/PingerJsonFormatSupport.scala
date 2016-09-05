package svc

import java.util.Date

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import model.{PingMethod, Task, TaskResult}
import spray.json.{DefaultJsonProtocol, JsBoolean, JsNumber, JsObject, JsString, JsValue, RootJsonFormat, _}

import scala.concurrent.duration
import scala.concurrent.duration.Duration

trait PingerJsonFormatSupport extends DefaultJsonProtocol with SprayJsonSupport {

  // TaskResult(id: Long, start: Date, rtt: Option[Int], status: Byte, message: Option[String])

  implicit object TaskResultJsonFormat extends RootJsonFormat[TaskResult] {
    def write(t: TaskResult) = JsObject(
      "id" -> JsNumber(t.id),
      "start" -> JsNumber(t.start.getTime),
      "rtt" -> {
        t.rtt match {
          case Some(v) => JsNumber(v)
          case None => JsNull
        }
      },
      "status" -> JsNumber(t.status),
      "message" -> {
        t.message match {
          case Some(v) => JsString(v)
          case None => JsNull
        }
      }
    )

    def read(value: JsValue) = value.asJsObject.getFields("id", "start", "rtt", "status", "message") match {
      case Seq(JsNumber(id), JsNumber(start), JsNumber(rtt), JsNumber(status), JsString(message)) =>
        new TaskResult(
          id.toLong,
          new Date(start.toLong),
          Some(rtt.toInt),
          status.toByte,
          Some(message)
        )
      case _ => deserializationError("Task result expected")
    }
  }

  implicit object TaskJsonFormat extends RootJsonFormat[Task] {
    def write(t: Task) = JsObject(
      "id" -> JsNumber(t.id),
      "method" -> JsString(t.method.toString),
      "name" -> JsString(t.name),
      "uri" -> JsString(t.uri.toString),
      "period" -> JsNumber(t.period.toSeconds),
      "active" -> JsBoolean(t.active)
    )

    def read(value: JsValue) = value.asJsObject.getFields("id", "method", "name", "uri", "period", "active") match {
      case Seq(JsNumber(id), JsString(method), JsString(name), JsString(uri), JsNumber(period), JsBoolean(active)) =>
        new Task(id.toLong,
          PingMethod.apply(method), name,
          new java.net.URI(uri),
          Duration(period.toLong, duration.SECONDS),
          active
        )
      case _ => deserializationError("Task expected")
    }
  }

}
