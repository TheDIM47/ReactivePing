package com.juliasoft.pinger.tests

import java.net.URI

import com.juliasoft.pinger.NativePing
import com.juliasoft.pinger.tests.Tools.{TestPing, TestAnswer}
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.FunSuite

import scala.concurrent.duration._
import scala.concurrent.Await

class NativePingSpec extends FunSuite with StrictLogging {
  import scala.concurrent.ExecutionContext.Implicits.global

  test("case 1: NativePing should successfully resolve localhost") {
    val p = new TestPing(0, 100, "") with NativePing
    p.pingService.ping(new URI("localhost"))
    Await.result(p.answered.future, 1.second)
  }

  test("case 2: NativePing should successfully resolve localhost IP") {
    val p = new TestPing(0, 100, "") with NativePing
    p.pingService.ping(new URI("127.0.0.1"))
    Await.result(p.answered.future, 1.second)
  }

  test("case 3: NativePing should fail on wrong IP") {
    val p = new TestPing(0, 100, "Unreachable host 0.0.0.1") with NativePing
    p.pingService.ping(new URI("0.0.0.1"))
    Await.result(p.answered.future, 1.second)
  }

  test("case 4: NativePing should successfully resolve google")  {
    val p = new TestPing(0, 1000, "") with NativePing
    p.pingService.ping(new URI("http://google.com"))
    Await.result(p.answered.future, 1.second)
  }
  
}
