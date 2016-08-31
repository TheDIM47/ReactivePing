package model

import java.net.{InetAddress, URI}
import java.util.{Calendar, Date}

import model.Pinger.{ErrorInfo, PingInfo}
import org.icmp4j.{IcmpPingRequest, IcmpPingResponse, IcmpPingUtil}

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}

object Pinger {

//  case class PingReq(address: URI)
//
  abstract class PingAck(start: Date)

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

  def ping(address: URI): Unit

  def sendSuccess(info: PingInfo): Unit

  def sendFailure(error: ErrorInfo): Unit
}

/**
  * "Ping" using Http GET request with redirect (Status 302)
  * Success if answer 200 OK
  */
trait HttpPinger extends Pinger {
  import dispatch._

  def ping(address: URI): Unit = {
    val start = Calendar.getInstance.getTime
    val f = Http.configure(builder => builder.setFollowRedirect(true).setRequestTimeout(timeout))(url(address.toURL.toString) OK as.String)
    f.onSuccess({
      case _ => sendSuccess(PingInfo(start, (Calendar.getInstance.getTimeInMillis - start.getTime).toInt))
    })
    f.onFailure({
      case t: Throwable => sendFailure(ErrorInfo(start, t.getMessage))
    })
  }
}

/**
  * Call platform's native PING utility and parse output
  */
trait NativePinger extends Pinger {
  def ping(address: URI): Unit = {
    val start = Calendar.getInstance.getTime
    val f: Future[IcmpPingResponse] = Future {
      val host = if (address.getHost == null) address.getPath else address.getHost
      require(host != null)
      val request: IcmpPingRequest = IcmpPingUtil.createIcmpPingRequest
      request.setHost(host)
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
  *
  * @See https://docs.oracle.com/javase/8/docs/api/java/net/InetAddress.html#isReachable(int)
  */
trait ReachableEcho extends Pinger {
  def ping(address: URI): Unit = {
    val start = Calendar.getInstance.getTime
    val f = Future {
      val host = if (address.getHost == null) address.getPath else address.getHost
      require(host != null)
      InetAddress.getByName(host).isReachable(timeout)
    }
    f.onSuccess({
      case b => if (b) sendSuccess(PingInfo(start, (Calendar.getInstance.getTimeInMillis - start.getTime).toInt))
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
  *
  * @See https://docs.oracle.com/javase/7/docs/api/java/net/InetAddress.html#isReachable(int)
  */
trait ReachableFuture extends Pinger {
  def ping(address: URI): Unit = {
    val start = Calendar.getInstance.getTime
    val f = Future.find {
      val host = if (address.getHost == null) address.getPath else address.getHost
      require(host != null)
      InetAddress.getAllByName(host).map(a => Future(a.isReachable(timeout)))
    } { f => f } // { f => f == true }
    f.onSuccess({
      case Some(b) if (b) => sendSuccess(PingInfo(start, (Calendar.getInstance.getTimeInMillis - start.getTime).toInt))
      case _ => sendFailure(ErrorInfo(start, s"Unreachable host ${address}"))
    })
    f.onFailure({
      case t: Throwable => sendFailure(ErrorInfo(start, t.getMessage))
    })
  }
}

