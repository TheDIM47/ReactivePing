package com.juliasoft.pinger

import java.sql._

import akka.actor.Actor
import com.juliasoft.pinger.Model.{Task, TaskResult}
import com.typesafe.scalalogging.StrictLogging

object Db {
  def using[Closeable <: AutoCloseable, B](closeable: Closeable)(f: Closeable => B): B =
    try {
      f(closeable)
    } finally {
      closeable.close()
    }

  private[this] def applyParams(stmt: PreparedStatement, params: Any*): Unit = {
    params.zipWithIndex.foreach(p => p._1 match {
      case None => stmt.setObject(1 + p._2, null)
      case Some(x) => stmt.setObject(1 + p._2, x)
      case _ => stmt.setObject(1 + p._2, p._1)
    })
  }

  def getOption[T](index: Int, rs: ResultSet): Option[T] = {
    val v = rs.getObject(index)
    if (rs.wasNull)
      None
    else
      Some(v.asInstanceOf[T])
  }

  /**
   * Executes SELECT query with parameters
   * @param conn connection
   * @param sql query
   * @param params query parameters (if any)
   * @param f will be applied to ResultSet
   * @tparam B
   * @return result of applying "process" function to ResultSet
   */
  def query[B](conn: Connection, sql: String, params: Any*)(f: ResultSet => B): B =
    using(conn) { connection =>
      using(connection.prepareStatement(sql)) { stmt =>
        applyParams(stmt, params: _*) // params.zipWithIndex.foreach(p => stmt.setObject(1 + p._2, p._1))
        using(stmt.executeQuery()) { results =>
          f(results)
        }
      }
    }

  /**
   * Execute query that not return ResultSet (non-select queries)
   * @param conn connection
   * @param sql query
   * @param params query parameters (if any)
   * @return number of records processed
   */
  def updateQuery(conn: Connection, sql: String, params: Any*): Int =
    using(conn) { connection =>
      using(connection.prepareStatement(sql)) { stmt =>
        applyParams(stmt, params: _*)
        stmt.executeUpdate()
      }
    }

  /**
   * Execute Update (non-select) query and return auto-generated keys
   * @param conn connection
   * @param sql query
   * @param params query parameters (if any)
   * @param f will be applied to ResultSet of GeneratedKeys
   * @tparam B
   * @return result of applying "process" function to ResultSet of GeneratedKeys
   */
  def createQuery[B](conn: Connection, sql: String, params: Any*)(f: ResultSet => B): B =
    using(conn) { connection =>
      using(connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { stmt =>
        applyParams(stmt, params: _*) // params.zipWithIndex.foreach(p => stmt.setObject(1 + p._2, p._1))
        stmt.executeUpdate()
        //        if (stmt.executeUpdate() != 1)
        //          throw new SQLException(s"Unable to create record for $sql [${params}]")
        using(stmt.getGeneratedKeys) { results =>
          f(results)
        }
      }
    }

  def apply(driver: String, url: String, user: String, pass: String) = new Db(driver, url, user, pass)
}

class Db(driver: String, url: String, user: String, pass: String) extends Storage with AutoCloseable with StrictLogging {
  Class.forName(driver)

  private[this] def connection = DriverManager.getConnection(url, user, pass)

  /** PingData (TaskResult) */

  def createResult(tr: TaskResult): TaskResult = {
    val sql = "insert into pingdata(id, start, rtt, status, message)values(?, ?, ?, ?, ?)"
    if (Db.updateQuery(connection, sql, tr.id, tr.start, tr.rtt, tr.status, tr.message) != 1)
      throw new SQLException(s"Unable to insert task result $tr")
    tr
  }

  def listResults(taskId: Long): List[TaskResult] = {
    val sql = "select id, start, rtt, status, message from pingdata where id=?"
    var result: List[TaskResult] = Nil
    Db.query(connection, sql, taskId) { rs =>
      while (rs.next()) {
        result = rsToTaskResult(rs) :: result
      }
    }
    result
  }

  /** Task */

  def listTasks: List[Task] = {
    val sql = "select id, name, url, period, active from tasks"
    var result: List[Task] = Nil
    Db.query(connection, sql) { rs =>
      while (rs.next()) {
        result = rsToTask(rs) :: result
      }
    }
    result
  }

  def getTask(taskId: Long): Option[Task] = {
    val sql = "select id, name, url, period, active from tasks where id=?"
    Db.query(connection, sql, taskId) { rs =>
      if (rs.next()) {
        Some(rsToTask(rs))
      } else {
        None
      }
    }
  }

  def createTask(task: Task): Task = {
    val sql = "insert into tasks(name, url, period, active)values(?, ?, ?, ?)"
    Db.createQuery(connection, sql, task.name, task.url, task.period, task.active) { rs =>
      if (rs.next())
        task.copy(id = rs.getInt(1))
      else
        throw new SQLException(s"Unable to insert $task")
    }
  }

  def updateTask(task: Task): Task = {
    val sql = "update tasks set name=?, url=?, period=?, active=? where id=?"
    if (Db.updateQuery(connection, sql, task.name, task.url, task.period, task.active, task.id) == 1)
      task
    else
      throw new SQLException(s"Unable to update $task")
  }

  def deleteTask(taskId: Long): Int = {
    Db.updateQuery(connection, "delete from pingdata where id=?", taskId)
    val r = Db.updateQuery(connection, "delete from tasks where id=?", taskId)
    if (r != 1)
      throw new SQLException(s"Unable to delete task id=$taskId")
    r
  }

  private[this] def rsToTask(rs: ResultSet): Task = {
    val id = rs.getLong(1)
    val name = rs.getString(2)
    val url = rs.getString(3)
    val period = rs.getInt(4)
    val active = rs.getBoolean(5)
    Task(id, name, url, period, active)
  }

  private[this] def rsToTaskResult(rs: ResultSet): TaskResult = {
    val id = rs.getLong(1)
    val start = rs.getTimestamp(2)
    //    val rtt = Some(rs.getInt(3))
    val rtt = Db.getOption(3, rs)
    val status = rs.getInt(4)
    //    val message = Some(rs.getString(5))
    val message = Db.getOption(5, rs)
    TaskResult(id, start, rtt, status, message)
  }

  override def close() = {
    if (!connection.isClosed) connection.close()
  }
}
