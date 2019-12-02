package cromwell.pipeline.datastorage.dao.repository

import cromwell.pipeline.database.PipelineDatabaseEngine
import cromwell.pipeline.datastorage.dao.entry.UserEntry
import cromwell.pipeline.datastorage.dto.{ User, UserId }

import scala.concurrent.Future

class UserRepository(pipelineDatabaseEngine: PipelineDatabaseEngine, userEntry: UserEntry) {

  import pipelineDatabaseEngine._
  import pipelineDatabaseEngine.profile.api._

  def getUserById(userId: UserId): Future[Option[User]] =
    database.run(userEntry.getUserByIdAction(userId).result.headOption)

  def getUserByEmail(email: String): Future[Option[User]] =
    database.run(userEntry.getUserByEmailAction(email).result.headOption)

  def addUser(user: User): Future[UserId] = database.run(userEntry.addUserAction(user))

  def deactivateUser(email: String): Future[Int] = database.run(userEntry.deactivateUserAction(email))

}
