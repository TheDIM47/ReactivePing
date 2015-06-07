package com.juliasoft.pinger.tests

import java.net.URI

import akka.actor.{PoisonPill, ActorLogging, ActorRef, ActorSystem}
import akka.event.LoggingReceive
import akka.testkit.{TestActorRef, TestProbe}
import com.juliasoft.pinger.Pinger.{ErrorInfo, Ping, PingInfo}
import com.juliasoft.pinger._
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.FunSuite

import scala.concurrent.duration._

class ActorAnswerSpec extends FunSuite with StrictLogging {

  abstract case class TestActor(manager: ActorRef) extends ActorAnswer with Pinger with ActorLogging {
    override def receive: Receive = LoggingReceive {
      case Ping(uri) => ping(uri)
    }
  }

  test("case 1: NativePing Actor should successfully resolve localhost") {
    implicit val system = ActorSystem("PingerSpec")
    val manager = TestProbe()
    val pinger = TestActorRef(new TestActor(manager.ref) with NativePing)
    manager.watch(pinger)
    manager.send(pinger, Ping(new URI("localhost")))
    manager.expectMsgPF(1.second) {
      case PingInfo(start, rtt) => assert(rtt >= 0)
    }
    manager.send(pinger, PoisonPill)
    manager.expectTerminated(pinger)
    system.shutdown()
  }

  test("case 2: ReachableEcho should fail on wrong IP") {
    implicit val system = ActorSystem("PingerSpec")
    val manager = TestProbe()
    val pinger = TestActorRef(new TestActor(manager.ref) with ReachableEcho)
    manager.watch(pinger)
    manager.send(pinger, Ping(new URI("0.0.0.1")))
    manager.expectMsgPF(1.second) {
      case ErrorInfo(start, msg) => assert(msg == "Unreachable host 0.0.0.1")
    }
    manager.send(pinger, PoisonPill)
    manager.expectTerminated(pinger)
    system.shutdown()
  }

  test("case 3: ReachableEcho should not answer during timeout") {
    implicit val system = ActorSystem("PingerSpec")
    val manager = TestProbe()
    val pinger = TestActorRef(new TestActor(manager.ref) with ReachableEcho)
    manager.send(pinger, Ping(new URI("http://www.google.com")))
    manager.expectNoMsg(1.second)
    system.shutdown()
  }

  test("case 4: ReachableEcho should fail after timeout") {
    implicit val system = ActorSystem("PingerSpec")
    val manager = TestProbe()
    val pinger = TestActorRef(new TestActor(manager.ref) with ReachableEcho {
      override val timeout = 800
    })
    manager.watch(pinger)
    manager.send(pinger, Ping(new URI("http://www.google.com")))
    manager.expectNoMsg(800.millis)
    manager.expectMsgPF(400.millis) {
      case ErrorInfo(start, msg) => assert(msg == "Unreachable host http://www.google.com")
    }
    manager.send(pinger, PoisonPill)
    manager.expectTerminated(pinger)
    system.shutdown()
  }

  test("case 5: HttpPing should fail on resolve localhost IP") {
    implicit val system = ActorSystem("PingerSpec")
    val manager = TestProbe()
    val pinger = TestActorRef(new TestActor(manager.ref) with HttpPing)
    manager.watch(pinger)
    manager.send(pinger, Ping(new URI("http://127.0.0.1:8080")))
    manager.expectMsgPF(1.second) {
      case ErrorInfo(start, msg) => assert(msg == "java.net.ConnectException: Connection refused: /127.0.0.1:8080")
    }
    manager.send(pinger, PoisonPill)
    manager.expectTerminated(pinger)
    system.shutdown()
  }

}
