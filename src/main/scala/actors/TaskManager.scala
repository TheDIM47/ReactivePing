package actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Terminated}
import akka.event.LoggingReceive
import model.Pinger.{ErrorInfo, PingInfo}
import model._

import scala.concurrent.duration.Duration

class TaskManager(val resultStorage: ActorRef) extends Actor with ActorLogging {
  var actorMap = Map.empty[Long, ActorRef]

  // Start watching DB Actor
  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = context.watch(resultStorage)

  override def receive: Receive = LoggingReceive {
    // Create or update task
    case task: Task =>
      if (actorMap.contains(task.id)) {
        actorMap.get(task.id).foreach(_ ! PoisonPill)
        actorMap -= task.id
      }
      if (task.active) {
        val child: ActorRef = createChild(task)
        log.debug("Created child: {} for task: {}", child, task)
        context.watch(child)
        actorMap += task.id -> child
        log.debug("Child: {} for task: {} started", child, task)
      }
    // case t: Task

    // Db/Child terminated
    case Terminated(a) =>
      context.unwatch(a)
      log.debug("Actor {} terminated", a)
    // case Terminated
  }

  import TaskManager._

  def createChild(task: Task): ActorRef = task.method match {
    case PingNative => context.actorOf(Props(classOf[NativePing], task, resultStorage), "ping-native-" + genSeq())
    case PingHttp => context.actorOf(Props(classOf[HttpPing], task, resultStorage), "ping-http-" + genSeq())
    case PingEcho => context.actorOf(Props(classOf[EchoPinger], task, resultStorage), "ping-echo-" + genSeq())
    case PingFuture => context.actorOf(Props(classOf[FuturePinger], task, resultStorage), "ping-future-" + genSeq())
  }

  def genSeq() = {
    _seq = _seq + 1
    _seq
  }

  @volatile private var _seq = 0L
}

object TaskManager {
  case object PingEvent

  import scala.concurrent.ExecutionContext.Implicits.global

  abstract class PingActor(task: Task, resultStorage: ActorRef) extends Actor with Pinger with ActorLogging {
    val ticker = context.system.scheduler.schedule(Duration.Zero, task.period, self, PingEvent)

    @scala.throws[Exception](classOf[Exception])
    override def postStop(): Unit = ticker.cancel()

    def sendSuccess(info: PingInfo): Unit = {
      val result = TaskResult(task.id, info.start, Some(info.rtt), 0, None)
      resultStorage ! result
    }

    def sendFailure(error: ErrorInfo): Unit = {
      val result = TaskResult(task.id, error.start, None, 1, Some(error.msg))
      resultStorage ! result
    }

    override def receive: Receive = LoggingReceive {
      case PingEvent => ping(task.uri)
    }
  }

  class NativePing(task: Task, resultStorage: ActorRef) extends PingActor(task, resultStorage) with NativePinger

  class HttpPing(task: Task, resultStorage: ActorRef) extends PingActor(task, resultStorage) with HttpPinger

  class EchoPinger(task: Task, resultStorage: ActorRef) extends PingActor(task, resultStorage) with ReachableEcho

  class FuturePinger(task: Task, resultStorage: ActorRef) extends PingActor(task, resultStorage) with ReachableFuture
}
