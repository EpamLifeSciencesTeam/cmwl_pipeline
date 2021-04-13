package cromwell.pipeline.service

import cats.Show
import cats.data.{ NonEmptyChain, Validated }
import cats.implicits._
import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.datastorage.dto.{ User, UserNoCredentials }
import cromwell.pipeline.model.wrapper.{ Name, Password, UserEmail, UserId }
import cromwell.pipeline.service.UserService.{
  incorrectCurrentPassword,
  incorrectDuplicatedMessage,
  invalidCurrentPasswordMessage,
  invalidEmailMessage,
  invalidNameMessage,
  invalidNewPasswordMessage,
  invalidRepeatPasswordMessage,
  userNotFoundMessage
}
import cromwell.pipeline.utils.StringUtils._
import org.slf4j.{ Logger, LoggerFactory }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Random

class UserService(userRepository: UserRepository)(implicit executionContext: ExecutionContext) {

  val log: Logger = LoggerFactory.getLogger(getClass)

  def getUsersByEmail(emailPattern: String): Future[Seq[User]] =
    userRepository.getUsersByEmail(emailPattern)

  def deactivateUserById(userId: UserId): Future[Option[UserNoCredentials]] =
    for {
      _ <- userRepository.deactivateUserById(userId)
      user <- userRepository.getUserById(userId)
    } yield user.map(UserNoCredentials.fromUser)

  def updateUser(userId: UserId, request: UserUpdateRequest): Future[Int] = {

    def failMsg(email: UserEmail, cause: String): String = s"User info updating failure with email $email : $cause"

    userRepository.getUserById(userId).flatMap {
      case Some(user) =>
        val data: Either[NonEmptyChain[String], Future[Int]] = for {
          email <- logged(
            UserEmail.from(request.email),
            failMsg(user.email, invalidEmailMessage)
          ).toEither
          firstname <- logged(
            Name.from(request.firstName),
            failMsg(email, s"$invalidNameMessage [${request.firstName}]")
          ).toEither
          lastname <- logged(
            Name.from(request.lastName),
            failMsg(email, s"$invalidNameMessage [${request.lastName}]")
          ).toEither
        } yield userRepository.updateUser(
          user.copy(email = email, firstName = firstname, lastName = lastname)
        )

        data match {
          case Right(value) => value
          case Left(error)  => Future.failed(new RuntimeException(error.show))
        }

      case None =>
        log.warn(s"$userNotFoundMessage $userId")
        Future.failed(new RuntimeException(userNotFoundMessage))
    }
  }

  def updatePassword(
    userId: UserId,
    request: PasswordUpdateRequest,
    salt: String = Random.nextLong().toHexString
  ): Future[Int] = {

    def failMsg(email: UserEmail, cause: String): String = s"Password updating failure with email $email : $cause"

    userRepository.getUserById(userId).flatMap {
      case Some(user) =>
        val data: Either[NonEmptyChain[String], Future[Int]] = for {
          currentPassword <- logged(
            Password.from(request.currentPassword),
            failMsg(user.email, invalidCurrentPasswordMessage)
          ).toEither
          newPassword <- logged(
            Password.from(request.newPassword),
            failMsg(user.email, invalidNewPasswordMessage)
          ).toEither
          repeatPassword <- logged(
            Password.from(request.repeatPassword),
            failMsg(user.email, invalidRepeatPasswordMessage)
          ).toEither
        } yield
          if (newPassword != repeatPassword) {
            log.warn(failMsg(user.email, incorrectDuplicatedMessage))
            Future.failed(new RuntimeException(incorrectDuplicatedMessage))
          } else {
            user match {
              case user if user.passwordHash == calculatePasswordHash(currentPassword.value, user.passwordSalt) =>
                val passwordSalt = salt
                val passwordHash = calculatePasswordHash(newPassword.value, passwordSalt)
                userRepository.updatePassword(user.copy(passwordSalt = passwordSalt, passwordHash = passwordHash))
              case _ =>
                log.warn(failMsg(user.email, incorrectCurrentPassword))
                Future.failed(new RuntimeException(incorrectCurrentPassword))
            }
          }

        data match {
          case Right(value) => value
          case Left(error)  => Future.failed(new RuntimeException(error.show))
        }

      case None =>
        log.warn(s"$userNotFoundMessage $userId")
        Future.failed(new RuntimeException(userNotFoundMessage))
    }
  }

  def logged[E: Show, A](validated: Validated[E, A], error: => String): Validated[E, A] =
    validated.leftMap { e =>
      log.warn(s"$error. Reason: [${e.show}]")
      e
    }
}

object UserService {
  val invalidCurrentPasswordMessage = "invalid current password"
  val invalidNewPasswordMessage = "invalid new password"
  val invalidRepeatPasswordMessage = "invalid repeat password"
  val invalidEmailMessage = "invalid email"
  val invalidNameMessage = "invalid name"
  val userNotFoundMessage = "user has not been found"
  val incorrectDuplicatedMessage = "new password incorrectly duplicated"
  val incorrectCurrentPassword = "user password differs from entered"
}
