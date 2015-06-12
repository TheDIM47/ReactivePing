package com.juliasoft.pinger.tests

import com.juliasoft.pinger.Pinger
import com.juliasoft.pinger.Pinger.{ErrorInfo, PingInfo}
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.FunSpec

import scala.concurrent.Promise
import scala.util.Success

/**
 *
 */
object Tools extends FunSpec with StrictLogging {

  trait TestAnswer extends Pinger {
    val rtt1: Long
    val rtt2: Long
    val msg: String

    val answered = Promise[Boolean]()

    def answerService: AnswerService = new TestAnswerService

    class TestAnswerService extends AnswerService {
      def sendSuccess(info: PingInfo): Unit = {
        logger.info(s"${info}")
        assert(info.rtt >= rtt1 && info.rtt < rtt2)
        answered.complete(Success(true))
      }

      def sendFailure(error: ErrorInfo): Unit = {
        logger.error(s"${error}")
        assert(error.msg == msg)
        answered.complete(Success(true))
      }
    }

  }

  abstract case class TestPing(rtt1: Long, rtt2: Long, msg: String) extends TestAnswer

  @volatile private var _seq = 0L

  def nextSeq = {
    _seq += 1
    _seq
  }
}
