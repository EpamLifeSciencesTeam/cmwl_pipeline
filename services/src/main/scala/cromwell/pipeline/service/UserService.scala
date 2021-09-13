package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.datastorage.dto.{ User, UserWithCredentials }
import cromwell.pipeline.model.wrapper.{ Password, UserEmail, UserId }
import cromwell.pipeline.service.UserService.Exceptions._
import cromwell.pipeline.service.exceptions.ServiceException
import cromwell.pipeline.utils.StringUtils._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Random, Success }

trait UserService {

  def getUsersByEmail(emailPattern: String): Future[Seq[User]]

  def getUserWithCredentialsByEmail(email: UserEmail): Future[Option[UserWithCredentials]]

  def addUser(user: UserWithCredentials): Future[UserId]

  def deactivateUserById(userId: UserId): Future[User]

  def updateUser(userId: UserId, request: UserUpdateRequest): Future[Int]

  def updatePassword(
    userId: UserId,
    request: PasswordUpdateRequest,
    salt: String = Random.nextLong().toHexString
  ): Future[Int]

}

object UserService {

  object Exceptions {
    sealed abstract class UserServiceException(message: String) extends ServiceException(message)

    final case class AccessDenied(message: String = "Access denied") extends UserServiceException(message)
    final case class NotFound(message: String = "User with this id doesn't exist") extends UserServiceException(message)
    final case class WrongPassword(message: String = "Password incorrect") extends UserServiceException(message)
    final case class InternalError(message: String = "Internal error") extends UserServiceException(message)
  }

  def apply(userRepository: UserRepository)(implicit executionContext: ExecutionContext): UserService =
    new UserService {

      def getUsersByEmail(emailPattern: String): Future[Seq[User]] =
        userRepository
          .getUsersByEmail(emailPattern)
          .recoverWith {
            case _ => internalError("find user")
          }
          .map(seq => seq.map(User.fromUserWithCredentials))

      def getUserWithCredentialsByEmail(email: UserEmail): Future[Option[UserWithCredentials]] =
        userRepository.getUserByEmail(email).recoverWith {
          case _ => internalError("find user")
        }

      def addUser(user: UserWithCredentials): Future[UserId] =
        userRepository.addUser(user).recoverWith {
          case _ => internalError("add user")
        }

      def deactivateUserById(userId: UserId): Future[User] =
        for {
          _ <- userRepository.deactivateUserById(userId).recoverWith {
            case _ => internalError("update user")
          }
          user <- getUserById(userId)
        } yield User.fromUserWithCredentials(user)

      def updateUser(userId: UserId, request: UserUpdateRequest): Future[Int] =
        getUserById(userId).flatMap { user =>
          userRepository
            .updateUser(
              user.copy(email = request.email, firstName = request.firstName, lastName = request.lastName)
            )
            .recoverWith {
              case _ => internalError("update user")
            }
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
            Future.failed(WrongPassword("New password incorrectly duplicated"))
          }

        def checkUserPassword(user: UserWithCredentials): Future[Unit] =
          if (user.passwordHash == calculatePasswordHash(request.currentPassword, user.passwordSalt)) {
            Future.unit
          } else {
            Future.failed(WrongPassword("Users password differs from entered"))
          }

        for {
          _ <- checkRequestPassword
          user <- getUserById(userId)
          _ <- checkUserPassword(user)
          res <- updatePasswordUnsafe(userId, request.newPassword, salt)
        } yield res
      }

      private def getUserById(userId: UserId): Future[UserWithCredentials] =
        userRepository.getUserById(userId).transform {
          case Success(Some(user)) => Success(user)
          case Success(None)       => Failure(NotFound())
          case Failure(_)          => Failure(InternalError(internalErrorMsg("find user")))
        }

      private def updatePasswordUnsafe(userId: UserId, newPassword: Password, salt: String): Future[Int] = {
        val passwordHash = calculatePasswordHash(newPassword, salt)
        userRepository.updatePassword(userId = userId, passwordHash = passwordHash, passwordSalt = salt).recoverWith {
          case _ => internalError("update user")
        }
      }

      private def internalError(action: String): Future[Nothing] =
        Future.failed(InternalError(internalErrorMsg(action)))

      private def internalErrorMsg(action: String): String = s"Failed to $action due to unexpected internal error"
    }
}
