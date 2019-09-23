package cromwell.pipeline.datastorage.dao.repository

import cromwell.pipeline.database.PipelineDatabaseEngine
import cromwell.pipeline.datastorage.dao.entry.UserEntry
import cromwell.pipeline.datastorage.dto.{User, UserId}

import scala.concurrent.Future

class UserRepository(pipelineDatabaseEngine: PipelineDatabaseEngine, userEntry: UserEntry) {

  import pipelineDatabaseEngine.profile.api._

  private val database = pipelineDatabaseEngine.database

  def getUserById(userId: UserId): Future[Option[User]] = database.run(userEntry.getUserByIdAction(userId).result.headOption)

  def addUser(user: User): Future[UserId] = database.run(userEntry.addUserAction(user))

}
