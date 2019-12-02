package cromwell.pipeline.datastorage.dao.repository

import cromwell.pipeline.database.PipelineDatabaseEngine
import cromwell.pipeline.datastorage.dao.entry.UserEntry
import cromwell.pipeline.datastorage.dto.User.UserEmail
import cromwell.pipeline.datastorage.dto.{ User, UserId }

import scala.concurrent.Future

class UserRepository(pipelineDatabaseEngine: PipelineDatabaseEngine, userEntry: UserEntry) {

  import pipelineDatabaseEngine._
  import pipelineDatabaseEngine.profile.api._

  def getUserById(userId: UserId): Future[Option[User]] =
    database.run(userEntry.getUserByIdAction(userId).result.headOption)

  def getUserByEmail(email: UserEmail): Future[Option[User]] =
    database.run(userEntry.getUserByEmailAction(email).result.headOption)

  def addUser(user: User): Future[UserId] = database.run(userEntry.addUserAction(user))

  def deactivateByEmail(email: UserEmail): Future[Int] = database.run(userEntry.deactivateUserByEmail(email))

  def deactivateById(userId: UserId): Future[Int] = database.run(userEntry.deactivateUserById(userId))

}
