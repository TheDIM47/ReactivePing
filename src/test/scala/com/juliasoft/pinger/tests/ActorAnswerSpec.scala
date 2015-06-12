package com.juliasoft.pinger.tests

import java.net.URI

import akka.actor.Actor.Receive
import akka.actor._
import akka.event.LoggingReceive
import akka.testkit.{TestKit, TestActorRef, TestProbe}
import com.juliasoft.pinger.Pinger._
import com.juliasoft.pinger._
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.{FunSuiteLike, BeforeAndAfterAll, FunSuite}

import scala.concurrent.duration._

class ActorAnswerSpec extends TestKit(ActorSystem("PingerSpec")) with FunSuiteLike with BeforeAndAfterAll with StrictLogging {

  override def afterAll(): Unit = {
    system.shutdown()
  }

  abstract class TestActor(val manager: ActorRef) extends Pinger with Actor {
//    this: Pinger =>
    def receive: Receive = LoggingReceive {
      case Ping(uri) => pingService.ping(uri)
    }
  }

  test("case 1: NativePing Actor should successfully resolve localhost") {
    val mgr = TestProbe()
    val pinger = TestActorRef(new TestActor(mgr.ref) with NativePing with ActorAnswer, "test-case-1")
    mgr.send(pinger, Ping(new URI("localhost")))
    mgr.expectMsgPF(1.second) {
      case PingInfo(start, rtt) => assert(rtt >= 0)
    }
  }

  test("case 2: ReachableEcho should fail on wrong IP") {
    val manager = TestProbe()
    val pinger = TestActorRef(new TestActor(manager.ref) with ReachableEcho with ActorAnswer, "test-case-2")
    manager.send(pinger, Ping(new URI("0.0.0.1")))
    manager.expectMsgPF(1.second) {
      case ErrorInfo(start, msg) => assert(msg == "Unreachable host 0.0.0.1")
    }
  }

  test("case 3: ReachableEcho should not answer during timeout") {
    val manager = TestProbe()
    val pinger = TestActorRef(new TestActor(manager.ref) with ReachableEcho with ActorAnswer {
      override val timeout = 1000
    }, "test-case-3")
    manager.send(pinger, Ping(new URI("http://www.google.com")))
    manager.expectNoMsg(1.second)
  }

  test("case 4: ReachableEcho should fail after timeout") {
    val manager = TestProbe()
    val pinger = TestActorRef(new TestActor(manager.ref) with ReachableEcho with ActorAnswer {
      override val timeout = 1000
    }, "test-case-4")
    manager.send(pinger, Ping(new URI("http://www.google.com")))
    manager.expectNoMsg(750.millis)
    manager.expectMsgPF(1000.millis) {
      case ErrorInfo(start, msg) => {
        assert(msg == "Unreachable host http://www.google.com")
      }
      case _ => {
        assert(false)
      }
    }
  }

  test("case 5: HttpPing should fail on resolve localhost IP") {
    val manager = TestProbe()
    val pinger = TestActorRef(new TestActor(manager.ref) with HttpPing with ActorAnswer, "test-case-5")
    manager.send(pinger, Ping(new URI("http://127.0.0.1:8080")))
    manager.expectMsgPF(1.second) {
      case ErrorInfo(start, msg) => {
        assert(msg == "java.net.ConnectException: Connection refused: /127.0.0.1:8080")
      }
      case _ => {
        assert(false)
      }
    }
  }

}
