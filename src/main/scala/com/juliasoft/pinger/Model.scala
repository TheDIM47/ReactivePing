package com.juliasoft.pinger

import java.sql.Timestamp
import java.util.Date

object Model {

  /**
   * Task to execute
   * @param id task id
   * @param method method type: 'N' - Native, 'H' - Http, 'F' - Future, 'E' - Echo
   * @param name task name
   * @param url url to check
   * @param period period in seconds
   * @param active true if tsk active
   */
  case class Task(id: Long,
                  method: String,
                  name: String,
                  url: String,
                  period: Int,
                  active: Boolean)

  /**
   * Task result
   * @param id task id @See Task.id
   * @param start start datetime
   * @param rtt Some(roundtrip) or None if failed
   * @param status status (ok/failed)
   * @param message error message
   */
  case class TaskResult(id: Long,
                        start: Timestamp,
                        rtt: Option[Int],
                        status: Byte,
                        message: Option[String])

}
