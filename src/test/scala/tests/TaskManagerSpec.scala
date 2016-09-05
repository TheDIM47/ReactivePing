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
  * TaskManager specifications
  */
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

}
