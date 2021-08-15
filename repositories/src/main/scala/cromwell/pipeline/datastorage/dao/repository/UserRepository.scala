package cromwell.pipeline.datastorage.dao.repository

import cromwell.pipeline.database.PipelineDatabaseEngine
import cromwell.pipeline.datastorage.dao.entry.UserEntry
import cromwell.pipeline.datastorage.dto.UserWithCredentials
import cromwell.pipeline.model.wrapper.{ UserEmail, UserId }

import scala.concurrent.Future

trait UserRepository {

  def getUserById(userId: UserId): Future[Option[UserWithCredentials]]

  def getUserByEmail(email: UserEmail): Future[Option[UserWithCredentials]]

  def getUsersByEmail(emailPattern: String): Future[Seq[UserWithCredentials]]

  def addUser(user: UserWithCredentials): Future[UserId]

  def deactivateUserByEmail(email: UserEmail): Future[Int]

  def deactivateUserById(userId: UserId): Future[Int]

  def updateUser(updatedUser: UserWithCredentials): Future[Int]

  def updatePassword(userId: UserId, passwordHash: String, passwordSalt: String): Future[Int]

}

object UserRepository {
  def apply(pipelineDatabaseEngine: PipelineDatabaseEngine, userEntry: UserEntry): UserRepository =
    new UserRepository {

      import pipelineDatabaseEngine._
      import pipelineDatabaseEngine.profile.api._

      def getUserById(userId: UserId): Future[Option[UserWithCredentials]] =
        database.run(userEntry.getUserByIdAction(userId).result.headOption)

      def getUserByEmail(email: UserEmail): Future[Option[UserWithCredentials]] =
        database.run(userEntry.getUserByEmailAction(email).result.headOption)

      def getUsersByEmail(emailPattern: String): Future[Seq[UserWithCredentials]] =
        database.run(userEntry.getUsersByEmailAction(emailPattern))

      def addUser(user: UserWithCredentials): Future[UserId] = database.run(userEntry.addUserAction(user))

      def deactivateUserByEmail(email: UserEmail): Future[Int] = database.run(userEntry.deactivateUserByEmail(email))

      def deactivateUserById(userId: UserId): Future[Int] = database.run(userEntry.deactivateUserById(userId))

      def updateUser(updatedUser: UserWithCredentials): Future[Int] = database.run(userEntry.updateUser(updatedUser))

      def updatePassword(userId: UserId, passwordHash: String, passwordSalt: String): Future[Int] =
        database.run(userEntry.updatePassword(userId, passwordHash, passwordSalt))

    }

}
