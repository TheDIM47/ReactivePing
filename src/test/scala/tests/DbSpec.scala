package tests

import java.net.URI
import java.sql.Timestamp
import java.util.Calendar

import api.{ResultStorageImpl, TaskStorageImpl}
import model.{PingNative, Task, TaskResult}
import org.scalatest.FunSuite

import scala.concurrent.duration._

class DbSpec extends FunSuite /*with StrictLogging*/ {
  val driver = "org.h2.Driver"
  val url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:testdb.sql'"
  val user = "sa"
  val pass = ""

  test("Should successfully open connection and read empty result list") {
    val resultDB = new ResultStorageImpl(driver, url, user, pass)
    assert(resultDB.listResults(0).isEmpty)
  }

  test("Should successfully open connection and read empty task list") {
    val taskDB = new TaskStorageImpl(driver, url, user, pass)
    assert(taskDB.listTasks.isEmpty)
  }

  test("Should successfully create/read/update/delete task") {
    val taskDB = new TaskStorageImpl(driver, url, user, pass)
    assert(taskDB.listTasks.isEmpty)
    // create
    val task = taskDB.createTask(Task(0, PingNative, "test-case2", randomURI, 90.seconds, true))
    assert(task.id > 0)
    // read 1
    val list1 = taskDB.listTasks
    assert(!list1.isEmpty)
    assert(list1.head == task)
    // read 2
    assert(taskDB.getTask(task.id) == Some(task))
    // update
    val tmp = task.copy(id = task.id, name = "some name", uri = randomURI)
    taskDB.updateTask(tmp)
    assert(taskDB.getTask(task.id) == Some(tmp))
    // delete
    taskDB.deleteTask(task.id)
    assert(taskDB.getTask(task.id) == None)
  }

  test("Should successfully create/list ping data") {
    val taskDB = new TaskStorageImpl(driver, url, user, pass)
    // create task
    val task = taskDB.createTask(Task(0, PingNative, "test-case2", randomURI, 90.seconds, true))
    assert(task.id > 0)
    // create task results
    val now = Calendar.getInstance()
    val r1 = TaskResult(task.id, new Timestamp(now.getTimeInMillis), Some(1), 1, None)
    now.add(Calendar.SECOND, 1)
    val r2 = TaskResult(task.id, new Timestamp(now.getTimeInMillis), None, 0, Some("Some exception"))

    val resultDB = new ResultStorageImpl(driver, url, user, pass)
    resultDB.createResult(r1)
    resultDB.createResult(r2)
    // read task results
    val data = resultDB.listResults(task.id)
    assert(!data.isEmpty)
    assert(data.size == 2)
    assert(data.contains(r1))
    assert(data.contains(r2))
  }

  test("Should fail on duplicated task") {
    val taskDB = new TaskStorageImpl(driver, url, user, pass)
    val task = taskDB.createTask(Task(0, PingNative, "duplicated-task", randomURI, 90.seconds, true))
    assert(task.id > 0)
    intercept [java.sql.SQLException] {
      taskDB.createTask(Task(0, PingNative, "duplicated-task", randomURI, 90.seconds, true))
    }
  }

  test("Should fail on duplicated result") {
    val taskDB = new TaskStorageImpl(driver, url, user, pass)
    val task = taskDB.createTask(Task(0, PingNative, "duplicated-result", randomURI, 90.seconds, true))

    val resultDB = new ResultStorageImpl(driver, url, user, pass)
    val result = TaskResult(task.id, new Timestamp(Calendar.getInstance().getTimeInMillis), Some(1), 1, None)
    resultDB.createResult(result)
    intercept [java.sql.SQLException] {
      resultDB.createResult(result)
    }
  }

  test("Should fail on foreign key") {
    val resultDB = new ResultStorageImpl(driver, url, user, pass)
    val result = TaskResult(-1, new Timestamp(Calendar.getInstance().getTimeInMillis), Some(1), 1, None)
    intercept [java.sql.SQLException] {
      resultDB.createResult(result)
    }
  }

  test("Delete task also delete Task results") {
    // insert tasks
    val taskDB = new TaskStorageImpl(driver, url, user, pass)
    val task1 = taskDB.createTask(Task(0, PingNative, "test-delete-1", new URI("localhost:8080"), 90.seconds, true))
    assert(task1.id > 0)
    val task2 = taskDB.createTask(Task(0, PingNative, "test-delete-2", new URI("localhost:7777"), 90.seconds, true))
    assert(task2.id > 0)

    // insert results
    val resultDB = new ResultStorageImpl(driver, url, user, pass)
    val r1 = TaskResult(task1.id, new Timestamp(Calendar.getInstance().getTimeInMillis), Some(1), 1, None)
    resultDB.createResult(r1)
    val r2 = TaskResult(task2.id, new Timestamp(1 + Calendar.getInstance().getTimeInMillis), Some(1), 1, None)
    resultDB.createResult(r2)

    // check results
    val data1 = resultDB.listResults(task1.id)
    assert(data1.size == 1)
    assert(data1.contains(r1))

    val data2 = resultDB.listResults(task2.id)
    assert(data2.size == 1)
    assert(data2.contains(r2))

    // delete task
    val r = taskDB.deleteTask(task1.id)
    assert(r == 1)

    // check data1 results (deleted)
    val data1a = resultDB.listResults(task1.id)
    assert(data1a.isEmpty)

    // check data2 results (exists)
    val data2a = resultDB.listResults(task2.id)
    assert(!data2a.isEmpty)
  }

  def randomURI = new URI(Tools.randomString(10))
}
