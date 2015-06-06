package com.juliasoft.pinger.tests

import java.net.URI

import com.juliasoft.pinger.ReachableFuture
import com.juliasoft.pinger.tests.Tools.TestAnswer
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration._

class ReachableFutureSpec extends FunSuite with StrictLogging {
  import scala.concurrent.ExecutionContext.Implicits.global

  test("case 1: ReachableFuture should successfully resolve localhost") {
    val p = new TestAnswer(0, 100, "") with ReachableFuture
    p.ping(new URI("localhost"))
    Await.result(p.answered.future, 1.second)
  }

  test("case 2: ReachableFuture should successfully resolve localhost IP") {
    val p = new TestAnswer(0, 100, "") with ReachableFuture
    p.ping(new URI("127.0.0.1"))
    Await.result(p.answered.future, 1.second)
  }

  test("case 3: ReachableFuture should fail on wrong IP") {
    val p = new TestAnswer(0, 100, "Unreachable host 0.0.0.1") with ReachableFuture
    p.ping(new URI("0.0.0.1"))
    Await.result(p.answered.future, 1.second)
  }

  test("case 4: ReachableFuture should fail on resolving google")  {
    intercept[java.util.concurrent.TimeoutException] {
      val p = new TestAnswer(0, 1000, "Unreachable host http://google.com") with ReachableFuture
      p.ping(new URI("http://google.com"))
      Await.result(p.answered.future, 1.second)
    }
  }

}
