package com.juliasoft.pinger

import java.net.{InetAddress, URI}
import java.util.{Calendar, Date}

import akka.actor.ActorRef
import com.juliasoft.pinger.Pinger._
import org.icmp4j.{IcmpPingRequest, IcmpPingResponse, IcmpPingUtil}

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}

object Pinger {

  case class Ping(address: URI)

  class PingAck(start: Date)

  case class PingInfo(start: Date, rtt: Int) extends PingAck(start)

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
  val timeout = 60 * 1000

  def pingService: PingService

  def answerService: AnswerService

  trait PingService {
    def host(address: URI) = if (address.getHost == null) address.getPath else address.getHost

    def ping(address: URI): Unit
  }

  trait AnswerService {
    def sendSuccess(info: PingInfo): Unit

    def sendFailure(error: ErrorInfo): Unit
  }

}

trait PrintAnswer extends Pinger {
  // this: PingService =>
  def answerService: AnswerService = new PrintAnswerService

  class PrintAnswerService extends AnswerService {
    def sendSuccess(info: PingInfo): Unit = println(info)

    def sendFailure(error: ErrorInfo): Unit = println(error)
  }

}

trait ActorAnswer extends Pinger {
  // this: PingService =>
  def manager: ActorRef

  def answerService: AnswerService = new ActorAnswerService(manager)

  class ActorAnswerService(val manager: ActorRef) extends AnswerService {
    def sendSuccess(info: PingInfo): Unit = manager ! info

    def sendFailure(error: ErrorInfo): Unit = manager ! error
  }

}

/**
 * "Ping" using Http GET request with redirect (Status 302)
 * Success if answer 200 OK
 */
trait HttpPing extends Pinger {
//  this: AnswerService =>
  def pingService: PingService = new HttpPingService

  class HttpPingService extends PingService {

    import dispatch._

    def ping(address: URI): Unit = {
      val start = Calendar.getInstance.getTime
      val f = Http.configure(builder => builder.setFollowRedirect(true).setRequestTimeout(timeout))(url(address.toURL.toString) OK as.String)
      f.onSuccess({
        case _ => answerService.sendSuccess(PingInfo(start, (Calendar.getInstance.getTimeInMillis - start.getTime).toInt))
      })
      f.onFailure({
        case t: Throwable => answerService.sendFailure(ErrorInfo(start, t.getMessage))
      })
    }
  }

}

/**
 * Call platform's native PING utility and parse output
 */
trait NativePing extends Pinger {
//  this: AnswerService =>
  def pingService: PingService = new NativePingService

  class NativePingService extends PingService {
    def ping(address: URI): Unit = {
      val start = Calendar.getInstance.getTime
      val f: Future[IcmpPingResponse] = Future {
        val request: IcmpPingRequest = IcmpPingUtil.createIcmpPingRequest
        request.setHost(host(address))
        IcmpPingUtil.executePingRequest(request)
      }
      f.onSuccess({
        case r: IcmpPingResponse => if (r.getSuccessFlag) answerService.sendSuccess(PingInfo(start, r.getRtt))
        else answerService.sendFailure(ErrorInfo(start, s"Unreachable host ${address}"))
      })
      f.onFailure({
        case t: Throwable => answerService.sendFailure(ErrorInfo(start, t.getMessage))
      })
    }
  }

}

/**
 * "Ping" using ICMP Echo request to single address
 * @See https://docs.oracle.com/javase/7/docs/api/java/net/InetAddress.html#isReachable(int)
 */
trait ReachableEcho extends Pinger {
//  this: AnswerService =>
  def pingService: PingService = new ReachableEchoService

  class ReachableEchoService extends PingService {
    def ping(address: URI): Unit = {
      val start = Calendar.getInstance.getTime
      val f = Future {
        InetAddress.getByName(host(address)).isReachable(timeout)
      }
      f.onSuccess({
        case b => if (b) answerService.sendSuccess(PingInfo(start, (Calendar.getInstance.getTimeInMillis - start.getTime).toInt))
        else answerService.sendFailure(ErrorInfo(start, s"Unreachable host ${address}"))
      })
      f.onFailure({
        case t: Throwable => answerService.sendFailure(ErrorInfo(start, t.getMessage))
      })
    }
  }

}

/**
 * "Ping" using ICMP Echo request to multiple addresses
 * Success if any of addresses returns Success
 * @See https://docs.oracle.com/javase/7/docs/api/java/net/InetAddress.html#isReachable(int)
 */
trait ReachableFuture extends Pinger {
//  this: AnswerService =>
  def pingService: PingService = new ReachableFutureService

  class ReachableFutureService extends PingService {
    def ping(address: URI): Unit = {
      val start = Calendar.getInstance.getTime
      val f = Future.find {
        InetAddress.getAllByName(host(address)).map(a => Future(a.isReachable(timeout)))
      } { f => f } // { f => f == true }
      f.onSuccess({
        case Some(b) if (b) => answerService.sendSuccess(PingInfo(start, (Calendar.getInstance.getTimeInMillis - start.getTime).toInt))
        case _ => answerService.sendFailure(ErrorInfo(start, s"Unreachable host ${address}"))
      })
      f.onFailure({
        case t: Throwable => answerService.sendFailure(ErrorInfo(start, t.getMessage))
      })
    }
  }

}

