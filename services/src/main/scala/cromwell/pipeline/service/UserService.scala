package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.datastorage.dto.{ User, UserNoCredentials }
import cromwell.pipeline.model.wrapper.{ Password, UserEmail, UserId }
import cromwell.pipeline.utils.StringUtils._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Random

trait UserService {

  def getUsersByEmail(emailPattern: String): Future[Seq[User]]

  def getUserByEmail(email: UserEmail): Future[Option[User]]

  def addUser(user: User): Future[UserId]

  def deactivateUserById(userId: UserId): Future[Option[UserNoCredentials]]

  def updateUser(userId: UserId, request: UserUpdateRequest): Future[Int]

  def updatePassword(
    userId: UserId,
    request: PasswordUpdateRequest,
    salt: String = Random.nextLong().toHexString
  ): Future[Int]

}

object UserService {

  def apply(userRepository: UserRepository)(implicit executionContext: ExecutionContext): UserService =
    new UserService {

      def getUsersByEmail(emailPattern: String): Future[Seq[User]] =
        userRepository.getUsersByEmail(emailPattern)

      def getUserByEmail(email: UserEmail): Future[Option[User]] =
        userRepository.getUserByEmail(email)

      def addUser(user: User): Future[UserId] = userRepository.addUser(user)

      def deactivateUserById(userId: UserId): Future[Option[UserNoCredentials]] =
        for {
          _ <- userRepository.deactivateUserById(userId)
          user <- userRepository.getUserById(userId)
        } yield user.map(UserNoCredentials.fromUser)

      def updateUser(userId: UserId, request: UserUpdateRequest): Future[Int] =
        userRepository.getUserById(userId).flatMap {
          case Some(user) =>
            userRepository.updateUser(
              user.copy(email = request.email, firstName = request.firstName, lastName = request.lastName)
            )
          case None => Future.failed(new RuntimeException("user with this id doesn't exist"))
        }

      def updatePassword(
        userId: UserId,
        request: PasswordUpdateRequest,
        salt: String = Random.nextLong().toHexString
      ): Future[Int] = {

        def checkRequestPassword: Future[Unit] =
          if (request.newPassword == request.repeatPassword) {
            Future.unit
          } else {
            Future.failed(new RuntimeException("new password incorrectly duplicated"))
          }

        def checkUserPassword(user: User): Future[Unit] =
          if (user.passwordHash == calculatePasswordHash(request.currentPassword, user.passwordSalt)) {
            Future.unit
          } else {
            Future.failed(new RuntimeException("new password incorrectly duplicated"))
          }

        for {
          _ <- checkRequestPassword
          user <- getUserById(userId)
          _ <- checkUserPassword(user)
          res <- updatePasswordUnsafe(user, request.newPassword, salt)
        } yield res
      }

      private def getUserById(userId: UserId): Future[User] =
        userRepository.getUserById(userId).flatMap {
          case Some(user) => Future.successful(user)
          case None       => Future.failed(new RuntimeException("user with this id doesn't exist"))
        }

      private def updatePasswordUnsafe(user: User, newPassword: Password, salt: String): Future[Int] = {
        val passwordHash = calculatePasswordHash(newPassword, salt)
        userRepository.updatePassword(user.copy(passwordSalt = salt, passwordHash = passwordHash))
      }
    }

}
