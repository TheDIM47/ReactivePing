package tests

import actors.StorageCommands.{ListResults, ResultListAck}
import akka.actor.{Actor, ActorLogging}
import model.{Pinger, TaskResult}
import model.Pinger.{ErrorInfo, PingInfo}

import scala.concurrent.Promise
import scala.concurrent.duration._

/**
  *
  */
object Tools {

  abstract class FakePinger extends Pinger {
    var info = Promise[PingInfo]()
    var error = Promise[ErrorInfo]()

    override def sendSuccess(info: PingInfo): Unit = this.info.success(info)

    override def sendFailure(error: ErrorInfo): Unit = this.error.success(error)
  }

  class InMemoryResultStorage extends Actor with ActorLogging {
    private var _db = Map.empty[Long, Seq[TaskResult]]

    override def receive: Receive = {
      case ListResults(id) =>
        log.debug(s"got: ListResults($id)")
        sender ! ResultListAck(_db.getOrElse(id, Seq.empty[TaskResult]))

      case tr: TaskResult =>
        log.debug(s"got: $tr")
        val tseq = _db.getOrElse(tr.id, Seq.empty[TaskResult])
        _db += tr.id -> (tseq :+ tr)
        log.debug(s"_db: ${_db}")

      case x =>
        log.debug(s"got unhandled: $x")
    }
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
