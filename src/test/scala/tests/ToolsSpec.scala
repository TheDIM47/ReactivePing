package tests

import java.util.Date

import actors.StorageCommands.{ListResults, ResultListAck}
import akka.actor._
import akka.testkit.{ImplicitSender, TestKit}
import model.TaskResult
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import tests.Tools.InMemoryResultStorage


class ToolsSpec extends TestKit(ActorSystem("ToolsSpec"))
  with ImplicitSender with FunSuiteLike with BeforeAndAfterAll with Matchers {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  test("Fake TaskResult storage must accumulate and list results") {
    val id = 0L
    val storage = system.actorOf(Props[InMemoryResultStorage], "fake-storage")
    storage ! ListResults(id)
    expectMsg( ResultListAck(Seq.empty) )

    val t1 = TaskResult(id, new Date, Some(1), 1, Some("1"))
    storage ! t1
    storage ! ListResults(id)
    val single = expectMsgClass( classOf[ResultListAck] )
    single.results.size should be (1)
    single.results.head should be (t1)

    val t2 = TaskResult(id, new Date, Some(2), 2, Some("2"))
    storage ! t2
    storage ! ListResults(id)
    val double = expectMsgClass( classOf[ResultListAck] )
    double.results.size should be (2)
    double.results should contain allOf (t1, t2)

    storage ! ListResults(id - 1)
    expectMsg( ResultListAck(Seq.empty) )
  }

}
