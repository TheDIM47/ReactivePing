package tests

import java.net.URI

import model.ReachableEcho
import org.scalatest._
import tests.Tools._

import scala.concurrent.{Await, Future}

class ReachableEchoSpecs extends FunSuite {
  import scala.concurrent.ExecutionContext.Implicits.global

  test("ReachableEcho should NOT fail on resolve localhost") {
    val p = new FakePinger with ReachableEcho
    p.ping(new URI("localhost"))
    Await.result(Future.firstCompletedOf(Seq(p.error.future, p.info.future)), defaultDelay)
    assert(p.info.isCompleted)
    p.info.future.map(e => assert(e.rtt > 0))
  }

  test("ReachableEcho should NOT fail on resolve localhost IP") {
    val p = new FakePinger with ReachableEcho
    p.ping(new URI("127.0.0.1"))
    Await.result(Future.firstCompletedOf(Seq(p.error.future, p.info.future)), defaultDelay)
    assert(p.info.isCompleted)
    p.info.future.map(e => assert(e.rtt > 0))
  }

  test("ReachableEcho should fail on wrong IP") {
    val p = new FakePinger with ReachableEcho
    p.ping(new URI("0.0.0.1"))
    Await.result(Future.firstCompletedOf(Seq(p.error.future, p.info.future)), defaultDelay)
    assert(p.error.isCompleted)
    p.error.future.map(e => assert(e.msg.contains("URISyntaxException")))
  }

  test("ReachableEcho should successfully resolve host") {
    val p = new FakePinger with ReachableEcho
    p.ping(new URI("oracle.com"))
    Await.result(Future.firstCompletedOf(Seq(p.error.future, p.info.future)), defaultDelay)
    assert(p.info.isCompleted)
  }
}
