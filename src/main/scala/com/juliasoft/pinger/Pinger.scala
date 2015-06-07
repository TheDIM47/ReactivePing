package com.juliasoft.pinger

import java.net.{InetAddress, URI}
import java.util.{Calendar, Date}

import akka.actor.{Actor, ActorRef}
import com.juliasoft.pinger.Pinger.{ErrorInfo, PingInfo}
import org.icmp4j.{IcmpPingRequest, IcmpPingResponse, IcmpPingUtil}

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.util.{Success, Try}

object Pinger {

  // val timeout = 60 * 1000

  case class Ping(address: URI)

  class PingAck(start: Date)

  case class PingInfo(start: Date, rtt: Long) extends PingAck(start)

  case class ErrorInfo(start: Date, msg: String) extends PingAck(start)

  @tailrec final def retry[T](n: Int)(fn: => T): Try[T] = {
    Try {
      fn
    } match {
      case x: Success[T] => x
      case _ if n > 1 => retry(n - 1)(fn)
      case f => f
    }
  }
}

trait Pinger {
  val start = Calendar.getInstance.getTime
  val timeout = 60 * 1000

  def ping(address: URI): Unit

  def host(address: URI) = if (address.getHost == null) address.getPath else address.getHost
}

trait Answer {
  def sendSuccess(info: PingInfo): Unit

  def sendFailure(error: ErrorInfo): Unit
}

trait PrintAnswer extends Answer {
  override def sendSuccess(info: PingInfo): Unit = println(info)

  override def sendFailure(error: ErrorInfo): Unit = println(error)
}

trait ActorAnswer extends Answer with Actor {
  def manager: ActorRef

  override def sendSuccess(info: PingInfo): Unit = manager ! info

  override def sendFailure(error: ErrorInfo): Unit = manager ! error
}

/**
 * "Ping" using Http GET request with redirect (Status 302)
 * Success if answer 200 OK
 */
trait HttpPing extends Pinger with Answer {

  import dispatch.Defaults._
  import dispatch._

  override def ping(address: URI): Unit = {
    val f = Http.configure(_ setFollowRedirect true)(url(address.toURL.toString) OK as.String)
    f.onSuccess({
      case _ => sendSuccess(PingInfo(start, Calendar.getInstance.getTimeInMillis - start.getTime))
    })
    f.onFailure({
      case t: Throwable => sendFailure(ErrorInfo(start, t.getMessage))
    })
  }
}

/**
 * Call platform native PING utility and parse output
 */
trait NativePing extends Pinger with Answer {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def ping(address: URI): Unit = {
    val f: Future[IcmpPingResponse] = Future {
      val request: IcmpPingRequest = IcmpPingUtil.createIcmpPingRequest
      request.setHost(host(address))
      IcmpPingUtil.executePingRequest(request)
    }
    f.onSuccess({
      case r: IcmpPingResponse => if (r.getSuccessFlag) sendSuccess(PingInfo(start, r.getRtt))
      else sendFailure(ErrorInfo(start, s"Unreachable host ${address}"))
    })
    f.onFailure({
      case t: Throwable => sendFailure(ErrorInfo(start, t.getMessage))
    })
  }
}

/**
 * "Ping" using ICMP Echo request to single address
 * @See https://docs.oracle.com/javase/7/docs/api/java/net/InetAddress.html#isReachable(int)
 */
trait ReachableEcho extends Pinger with Answer {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def ping(address: URI): Unit = {
    val f = Future { InetAddress.getByName(host(address)).isReachable(timeout) }
    f.onSuccess({
      case b => if (b) sendSuccess(PingInfo(start, Calendar.getInstance.getTimeInMillis - start.getTime))
      else sendFailure(ErrorInfo(start, s"Unreachable host ${address}"))
    })
    f.onFailure({
      case t: Throwable => sendFailure(ErrorInfo(start, t.getMessage))
    })
  }
}

/**
 * "Ping" using ICMP Echo request to multiple addresses
 * Success if any of addresses returns Success
 * @See https://docs.oracle.com/javase/7/docs/api/java/net/InetAddress.html#isReachable(int)
 */
trait ReachableFuture extends Pinger with Answer {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def ping(address: URI): Unit = {
    val f = Future.find { InetAddress.getAllByName(host(address)).map(a => Future(a.isReachable(timeout))) } { f => f } // { f => f == true }
    f.onSuccess({
      case Some(b) if (b) => sendSuccess(PingInfo(start, Calendar.getInstance.getTimeInMillis - start.getTime))
      case _ => sendFailure(ErrorInfo(start, s"Unreachable host ${address}"))
    })
    f.onFailure({
      case t: Throwable => sendFailure(ErrorInfo(start, t.getMessage))
    })
  }
}

