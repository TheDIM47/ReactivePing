package tests

import java.net.URI

import actors.TaskManager
import actors.TaskManager.NativePing
import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import model.Pinger.{PingAck, PingInfo}
import model._
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

import scala.concurrent.duration._

/**
  *
  */
//case object FakeTestObject
//
//class TestTaskManager(child: ActorRef, resultStorage: ActorRef) extends TaskManager(resultStorage) {
//  override def createChild(task: Task): ActorRef = {
//    child ! FakeTestObject
//    child
//  }
//}

class TaskManagerSpec extends TestKit(ActorSystem("TaskManagerSpec"))
  with ImplicitSender with FunSuiteLike with BeforeAndAfterAll with Matchers {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  test("NativePing should send TaskResult messages to storage") {
    val task = Task(10L, PingNative, "test-task", new URI("localhost"), 300.milliseconds, true)
    val storage = TestProbe()

    val pinger = system.actorOf(Props(classOf[NativePing], task, storage.ref), "native-pinger-test")
    val result = storage.expectMsgClass(3.seconds, classOf[TaskResult])
    result.id should be (task.id)
    pinger ! PoisonPill
  }

  test("TaskManager should create Child Actor") {
    val task = Task(10L, PingNative, "test-task", new URI("localhost"), 300.milliseconds, true)
    val storage = TestProbe()

    val manager = system.actorOf(Props(classOf[TaskManager], storage.ref), "task-manager")
    manager ! task
    storage.expectMsgClass(classOf[TaskResult])
    // attempt to send same task
    manager ! task
    storage.expectMsgClass(classOf[TaskResult])
  }

  //  class TestActor(task: Task, storage: ActorRef, answer: PingAck) extends PingActor(task, storage) {
  //    override def ping(address: URI): Unit = answer match {
  //      case info: PingInfo => sendSuccess(info)
  //      case err: ErrorInfo => sendFailure(err)
  //    }
  //  }
  //
  //  test("case 1: NativePing Actor should successfully resolve localhost") {
  //    val mgr = TestProbe()
  //    val pinger = TestActorRef(new TestActor(mgr.ref) with NativePinger with ActorAnswer, "test-case-1")
  //    mgr.send(pinger, Ping(new URI("localhost")))
  //    mgr.expectMsgPF(1.second) {
  //      case PingInfo(start, rtt) => assert(rtt >= 0)
  //    }
  //  }
  //
  //  test("case 2: ReachableEcho should fail on wrong IP") {
  //    val manager = TestProbe()
  //    val pinger = TestActorRef(new TestActor(manager.ref) with ReachableEcho with ActorAnswer, "test-case-2")
  //    manager.send(pinger, Ping(new URI("0.0.0.1")))
  //    manager.expectMsgPF(1.second) {
  //      case ErrorInfo(start, msg) => assert(msg == "Unreachable host 0.0.0.1")
  //    }
  //  }
  //
  //  test("case 3: ReachableEcho should not answer during timeout") {
  //    val manager = TestProbe()
  //    val pinger = TestActorRef(new TestActor(manager.ref) with ReachableEcho with ActorAnswer {
  //      override val timeout = 1000
  //    }, "test-case-3")
  //    manager.send(pinger, Ping(new URI("http://www.google.com")))
  //    manager.expectNoMsg(1.second)
  //  }
  //
  //  test("case 4: ReachableEcho should fail after timeout") {
  //    val manager = TestProbe()
  //    val pinger = TestActorRef(new TestActor(manager.ref) with ReachableEcho with ActorAnswer {
  //      override val timeout = 1000
  //    }, "test-case-4")
  //    manager.send(pinger, Ping(new URI("http://www.google.com")))
  //    manager.expectNoMsg(750.millis)
  //    manager.expectMsgPF(1000.millis) {
  //      case ErrorInfo(start, msg) => {
  //        assert(msg == "Unreachable host http://www.google.com")
  //      }
  //      case _ => {
  //        assert(false)
  //      }
  //    }
  //  }
  //
  //  test("case 5: HttpPing should fail on resolve localhost IP") {
  //    val manager = TestProbe()
  //    val pinger = TestActorRef(new TestActor(manager.ref) with HttpPinger with ActorAnswer, "test-case-5")
  //    manager.send(pinger, Ping(new URI("http://127.0.0.1:8080")))
  //    manager.expectMsgPF(1.second) {
  //      case ErrorInfo(start, msg) => {
  //        assert(msg == "java.net.ConnectException: Connection refused: /127.0.0.1:8080")
  //      }
  //      case _ => {
  //        assert(false)
  //      }
  //    }
  //  }

}
