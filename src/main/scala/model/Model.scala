package model

import java.util.Date

import scala.concurrent.duration.FiniteDuration

sealed trait PingMethod

case object PingNative extends PingMethod

case object PingHttp extends PingMethod

case object PingFuture extends PingMethod

case object PingEcho extends PingMethod

object PingMethod {
  def apply(s: String): PingMethod = s match {
    case "PingNative" => PingNative
    case "PingHttp" => PingHttp
    case "PingFuture" => PingFuture
    case "PingEcho" => PingEcho
  }
}

case class Task(id: Long,
                method: PingMethod,
                name: String,
                uri: java.net.URI,
                period: FiniteDuration,
                active: Boolean)

case class TaskResult(id: Long,
                      start: Date,
                      rtt: Option[Int],
                      status: Byte,
                      message: Option[String])

