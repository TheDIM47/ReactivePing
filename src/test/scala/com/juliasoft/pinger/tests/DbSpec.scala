package com.juliasoft.pinger.tests

import java.sql.Timestamp
import java.util.Calendar

import com.juliasoft.pinger.Db
import com.juliasoft.pinger.Model.{Task, TaskResult}
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.FunSuite

class DbSpec extends FunSuite with StrictLogging {
  val driver = "org.h2.Driver"
  val url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:testdb.sql'"
  val user = "sa"
  val pass = ""

  test("case1: Should successfully open connection and read empty lists") {
    val db = Db(driver, url, user, pass)
    assert(db.listTasks == Nil)
    assert(db.listTaskResults(0) == Nil)
  }

  test("case2: Should successfully create/read/update/delete task") {
    val db = Db(driver, url, user, pass)
    assert(db.listTasks == Nil)
    // create
    val task = db.createTask(Task(0, "test-case2", "test-case2", 90, true))
    assert(task.id > 0)
    // read 1
    assert(db.listTasks != Nil)
    assert(db.listTasks.head == task)
    // read 2
    assert(db.getTask(task.id) == Some(task))
    // update
    val tmp = task.copy(id = task.id, name = "some name", url = "some url")
    db.updateTask(tmp)
    assert(db.getTask(task.id) == Some(tmp))
    // delete
    db.deleteTask(task.id)
    assert(db.getTask(task.id) == None)
  }

  test("case3: Should successfully create/list ping data") {
    val db = Db(driver, url, user, pass)
    // create task
    val task = db.createTask(Task(0, "test-case3", "test-case3", 90, true))
    assert(task.id > 0)
    // create task results
    val now = Calendar.getInstance()
    val r1 = TaskResult(task.id, new Timestamp(now.getTimeInMillis), Some(1), 1, None)
    now.add(Calendar.SECOND, 1)
    val r2 = TaskResult(task.id, new Timestamp(now.getTimeInMillis), None, 0, Some("Some exception"))
    db.createTaskResult(r1)
    db.createTaskResult(r2)
    // read task results
    val data = db.listTaskResults(task.id)
    assert(data != Nil)
    assert(data.size == 2)
    assert(data.contains(r1))
    assert(data.contains(r2))
  }

  test("case4: Should fail on duplicates") {
    val db = Db(driver, url, user, pass)
    val task = db.createTask(Task(0, "dup", "dup", 90, true))
    assert(task.id > 0)
    intercept[java.sql.SQLException] {
      db.createTask(Task(0, "dup", "dup", 90, true))
      assert(false)
    }
    val result = TaskResult(task.id, new Timestamp(Calendar.getInstance().getTimeInMillis), Some(1), 1, None)
    db.createTaskResult(result)
    intercept[java.sql.SQLException] {
      db.createTaskResult(result)
      assert(false)
    }
  }

}
