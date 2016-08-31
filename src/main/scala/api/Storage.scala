package api

//trait StorageActor extends Storage with Actor {
//
//  import StorageActor._
//
//  def receive: Receive = {
//    case ListResults(taskId) => sender ! ResultListAck(listResults(taskId))
//    case CreateResult(r) => sender ! ResultAck(createResult(r))
//    //
//    case ListTasks => sender ! TaskListAck(listTasks)
//    case GetTask(taskId) => sender ! TaskAck(getTask(taskId))
//    case CreateTask(task) => sender ! TaskAck(Some(createTask(task)))
//    case UpdateTask(task) => sender ! TaskAck(Some(updateTask(task)))
//    case DeleteTask(taskId) => sender ! TaskDeletedAck(deleteTask(taskId))
//  }
//}
