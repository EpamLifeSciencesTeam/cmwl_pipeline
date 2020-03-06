package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.datastorage.dto.{ User, UserId, UserNoCredentials }
import cromwell.pipeline.utils.StringUtils

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Random

class UserService(userRepository: UserRepository)(implicit executionContext: ExecutionContext) {

  def getUsersByEmail(emailPattern: String): Future[Seq[User]] =
    userRepository.getUsersByEmail(emailPattern)

  def deactivateUserById(userId: UserId): Future[Option[UserNoCredentials]] =
    for {
      _ <- userRepository.deactivateUserById(userId)
      user <- userRepository.getUserById(userId)
    } yield user.map(UserNoCredentials.fromUser)

  def updateUser(userId: String, request: UserUpdateRequest): Future[Int] =
    userRepository.getUserById(UserId(userId)).flatMap { userOpt =>
      userOpt.map { user =>
        userRepository.updateUser(
          user.copy(email = request.email, firstName = request.firstName, lastName = request.lastName)
        )
      } match {
        case Some(value) => value
        case None        => Future.failed(new RuntimeException("user with this id doesn't exist"))
      }
    }

  def updatePassword(
    userId: String,
    request: PasswordUpdateRequest,
    salt: String = Random.nextLong().toHexString
  ): Future[Int] =
    if (request.newPassword == request.repeatPassword) {
      userRepository.getUserById(UserId(userId)).flatMap {
        case Some(user)
            if user.passwordHash == StringUtils.calculatePasswordHash(request.currentPassword, user.passwordSalt) =>
          val passwordSalt = salt
          val passwordHash = StringUtils.calculatePasswordHash(request.newPassword, passwordSalt)
          userRepository.updatePassword(user.copy(passwordSalt = passwordSalt, passwordHash = passwordHash))
        case Some(_) => Future.failed(new RuntimeException("user password differs from entered"))
        case None    => Future.failed(new RuntimeException("user with this id doesn't exist"))
      }
    } else Future.failed(new RuntimeException("new password incorrectly duplicated"))
}
