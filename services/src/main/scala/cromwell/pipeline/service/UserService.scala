package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.datastorage.dto.{ User, UserNoCredentials }
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.utils.StringUtils._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Random

trait UserService {

  def getUsersByEmail(emailPattern: String): Future[Seq[User]]

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
      ): Future[Int] =
        if (request.newPassword == request.repeatPassword) {
          userRepository.getUserById(userId).flatMap {
            case Some(user) =>
              user match {
                case user
                    if user.passwordHash == calculatePasswordHash(request.currentPassword.unwrap, user.passwordSalt) => {
                  val passwordSalt = salt
                  val passwordHash = calculatePasswordHash(request.newPassword.unwrap, passwordSalt)
                  userRepository.updatePassword(user.copy(passwordSalt = passwordSalt, passwordHash = passwordHash))
                }
                case _ => Future.failed(new RuntimeException("user password differs from entered"))
              }
            case None => Future.failed(new RuntimeException("user with this id doesn't exist"))
          }
        } else Future.failed(new RuntimeException("new password incorrectly duplicated"))

    }

}
