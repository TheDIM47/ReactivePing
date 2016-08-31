package tests

import model.Pinger
import model.Pinger.{ErrorInfo, PingInfo}

import scala.concurrent.Promise
import scala.concurrent.duration._

/**
 *
 */
object Tools {
  abstract class FakeObject extends Pinger {
    var info = Promise[PingInfo]()
    var error = Promise[ErrorInfo]()
    override def sendSuccess(info: PingInfo): Unit = this.info.success(info)
    override def sendFailure(error: ErrorInfo): Unit = this.error.success(error)
  }

  val defaultDelay = 60.seconds

  val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')
  val charsLen = chars.length

  def randomString(length: Int) = randomAlpha(length)

  def randomAlpha(length: Int): String = {
    randomStringFromCharList(length, chars)
  }

  def randomStringFromCharList(length: Int, chars: Seq[Char]): String = {
    val sb = new StringBuilder
    for (i <- 1 to length) {
      val randomNum = scala.util.Random.nextInt(chars.length)
      sb.append(chars(randomNum))
    }
    sb.toString
  }

  @volatile private var _seq = 0L

  def nextSeq = {
    _seq += 1
    _seq
  }
}
