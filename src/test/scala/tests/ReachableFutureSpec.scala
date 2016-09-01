package tests

import java.net.URI

import model.ReachableFuture
import org.scalatest.FunSuite
import tests.Tools._

import scala.concurrent.{Await, Future}

class ReachableFutureSpec extends FunSuite {
  import scala.concurrent.ExecutionContext.Implicits.global

  test("ReachableFuture should NOT fail on resolve localhost") {
    val p = new FakePinger with ReachableFuture
    p.ping(new URI("localhost"))
    Await.result(Future.firstCompletedOf(Seq(p.error.future, p.info.future)), defaultDelay)
    assert(p.info.isCompleted)
    p.info.future.map(e => assert(e.rtt > 0))
  }

  test("ReachableFuture should NOT fail on resolve localhost IP") {
    val p = new FakePinger with ReachableFuture
    p.ping(new URI("127.0.0.1"))
    Await.result(Future.firstCompletedOf(Seq(p.error.future, p.info.future)), defaultDelay)
    assert(p.info.isCompleted)
    p.info.future.map(e => assert(e.rtt > 0))
  }

  test("ReachableFuture should fail on wrong IP") {
    val p = new FakePinger with ReachableFuture
    p.ping(new URI("0.0.0.1"))
    Await.result(Future.firstCompletedOf(Seq(p.error.future, p.info.future)), defaultDelay)
    assert(p.error.isCompleted)
    p.error.future.map(e => assert(e.msg.contains("URISyntaxException")))
  }

  test("ReachableFuture should successfully resolve host") {
    val p = new FakePinger with ReachableFuture
    p.ping(new URI("oracle.com"))
    Await.result(Future.firstCompletedOf(Seq(p.error.future, p.info.future)), defaultDelay)
    assert(p.info.isCompleted)
  }

}
