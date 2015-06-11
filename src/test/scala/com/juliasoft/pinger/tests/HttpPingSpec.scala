package com.juliasoft.pinger.tests

import java.net.URI

import com.juliasoft.pinger.HttpPing
import com.juliasoft.pinger.tests.Tools.TestAnswer
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration._

class HttpPingSpec extends FunSuite with StrictLogging {
  import scala.concurrent.ExecutionContext.Implicits.global

  test("case 1: HttpPing should fail on resolve localhost") {
    val p = new TestAnswer(0, 100, "java.net.ConnectException: Connection refused: localhost/127.0.0.1:8080") with HttpPing
    p.ping(new URI("http://localhost:8080"))
    Await.result(p.answered.future, 1.second)
  }

  test("case 2: HttpPing should fail on resolve localhost IP") {
    val p = new TestAnswer(0, 100, "java.net.ConnectException: Connection refused: /127.0.0.1:8080") with HttpPing
    p.ping(new URI("http://127.0.0.1:8080"))
    Await.result(p.answered.future, 1.second)
  }

  test("case 3: HttpPing should fail on wrong IP") {
    val p = new TestAnswer(0, 100, "java.net.ConnectException: Invalid argument") with HttpPing
    p.ping(new URI("http://0.0.0.1:8080"))
    Await.result(p.answered.future, 1.second)
  }

  test("case 4: HttpPing should successfully resolve google")  {
    val p = new TestAnswer(0, 1200, "") with HttpPing
    p.ping(new URI("http://google.com"))
    Await.result(p.answered.future, 2.second)
  }
}
