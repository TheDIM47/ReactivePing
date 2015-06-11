package com.juliasoft.pinger.tests

import com.juliasoft.pinger.Answer
import com.juliasoft.pinger.Pinger.{ErrorInfo, PingInfo}
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.FunSpec

import scala.concurrent.Promise
import scala.util.Success

/**
 *
 */
object Tools extends FunSpec with StrictLogging {
  import scala.concurrent.ExecutionContext.Implicits.global

  case class TestAnswer(rtt1: Long, rtt2: Long, msg:String) extends Answer {
    var answered = Promise[Boolean]()
    override def sendSuccess(info: PingInfo): Unit = {
      logger.info(s"${info}")
      assert(info.rtt >= rtt1 && info.rtt < rtt2)
      answered.complete(Success(true))
    }
    override def sendFailure(error: ErrorInfo): Unit = {
      logger.error(s"${error}")
      assert(error.msg == msg)
      answered.complete(Success(true))
    }
  }

  @volatile private var _seq = 0L
  def nextSeq = {
    _seq += 1
    _seq
  }
}
