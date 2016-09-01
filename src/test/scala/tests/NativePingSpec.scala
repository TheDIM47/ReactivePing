package tests

import java.net.URI

import model.NativePinger
import org.scalatest.FunSuite
import tests.Tools._

import scala.concurrent.{Await, Future}

class NativePingSpec extends FunSuite {
  import scala.concurrent.ExecutionContext.Implicits.global

  test("NativePing should NOT fail on resolve localhost") {
    val p = new FakePinger with NativePinger
    p.ping(new URI("localhost"))
    Await.result(Future.firstCompletedOf(Seq(p.error.future, p.info.future)), defaultDelay)
    assert(p.info.isCompleted)
    p.info.future.map(e => assert(e.rtt > 0))
  }

  test("NativePing should NOT fail on resolve localhost IP") {
    val p = new FakePinger with NativePinger
    p.ping(new URI("127.0.0.1"))
    Await.result(Future.firstCompletedOf(Seq(p.error.future, p.info.future)), defaultDelay)
    assert(p.info.isCompleted)
    p.info.future.map(e => assert(e.rtt > 0))
  }

  test("NativePing should fail on wrong IP") {
    val p = new FakePinger with NativePinger
    p.ping(new URI("0.0.0.1"))
    Await.result(Future.firstCompletedOf(Seq(p.error.future, p.info.future)), defaultDelay)
    assert(p.error.isCompleted)
    p.error.future.map(e => assert(e.msg.contains("URISyntaxException")))
  }

  test("NativePing should successfully resolve google") {
    val p = new FakePinger with NativePinger
    p.ping(new URI("google.com"))
    Await.result(Future.firstCompletedOf(Seq(p.error.future, p.info.future)), defaultDelay)
    assert(p.info.isCompleted)
  }
}
