package tests

import java.net.URI

import model.HttpPinger
import org.scalatest.FunSuite
import tests.Tools._

import scala.concurrent.{Await, Future}

class HttpPingSpec extends FunSuite {
  import scala.concurrent.ExecutionContext.Implicits.global

  test("HttpPing should fail on resolve localhost") {
    val p = new FakeObject with HttpPinger
    p.ping(new URI("http://localhost:1111"))
    Await.result(Future.firstCompletedOf(Seq(p.error.future, p.info.future)), defaultDelay)
    assert(p.error.isCompleted)
    p.error.future.map(e => assert(e.msg.contains("ConnectException")))
  }

  test("HttpPing should fail on resolve localhost IP") {
    val p = new FakeObject with HttpPinger
    p.ping(new URI("http://127.0.0.1:1111"))
    Await.result(Future.firstCompletedOf(Seq(p.error.future, p.info.future)), defaultDelay)
    assert(p.error.isCompleted)
    p.error.future.map(e => assert(e.msg.contains("ConnectException")))
  }

  test("HttpPing should fail on wrong IP") {
    val p = new FakeObject with HttpPinger
    p.ping(new URI("http://0.0.0.1:1111"))
    Await.result(Future.firstCompletedOf(Seq(p.error.future, p.info.future)), defaultDelay)
    assert(p.error.isCompleted)
    p.error.future.map(e => assert(e.msg.contains("URISyntaxException")))
  }

  test("HttpPing should successfully resolve google") {
    val p = new FakeObject with HttpPinger
    p.ping(new URI("http://google.com"))
    Await.result(Future.firstCompletedOf(Seq(p.error.future, p.info.future)), defaultDelay)
    assert(p.info.isCompleted)
  }
}
