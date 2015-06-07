package com.juliasoft.pinger.tests

import java.net.URI

import com.juliasoft.pinger.ReachableEcho
import com.juliasoft.pinger.tests.Tools.TestAnswer
import com.typesafe.scalalogging.StrictLogging
import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration._

class ReachableEchoSpecs extends FunSuite with StrictLogging {
  import scala.concurrent.ExecutionContext.Implicits.global

  test("case 1: ReachableEcho should successfully resolve localhost") {
    val p = new TestAnswer(0, 100, "") with ReachableEcho
    p.ping(new URI("localhost"))
    Await.result(p.answered.future, 1.second)
  }

  test("case 2: ReachableEcho should successfully resolve localhost IP") {
    val p = new TestAnswer(0, 100, "") with ReachableEcho
    p.ping(new URI("127.0.0.1"))
    Await.result(p.answered.future, 1.second)
  }

  test("case 3: ReachableEcho should fail on wrong IP") {
    val p = new TestAnswer(1, 0, "Unreachable host 0.0.0.1") with ReachableEcho
    p.ping(new URI("0.0.0.1"))
    Await.result(p.answered.future, 1.second)
  }

  test("case 4: ReachableEcho should fail with java.util.concurrent.TimeoutException")  {
    intercept[java.util.concurrent.TimeoutException] {
      val p = new TestAnswer(0, 1000, "") with ReachableEcho
      p.ping(new URI("http://google.com"))
      Await.result(p.answered.future, 1.second)
    }
  }

}
