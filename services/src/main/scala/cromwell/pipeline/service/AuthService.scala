package cromwell.pipeline.service

import cats.Show

import java.time.Instant
import cats.data.{ NonEmptyChain, Validated }
import cats.implicits._
import cromwell.pipeline.auth.AuthUtils
import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.User
import cromwell.pipeline.datastorage.dto.auth.{
  AccessTokenContent,
  AuthContent,
  AuthResponse,
  RefreshTokenContent,
  SignInRequest,
  SignUpRequest
}
import cromwell.pipeline.model.wrapper.{ Name, Password, UserEmail, UserId }
import cromwell.pipeline.service.AuthService.{
  existUserMessage,
  inactiveUserMessage,
  incorrectPasswordMessage,
  invalidEmailMessage,
  invalidNameMessage,
  invalidPasswordMessage,
  userNotFoundMessage
}
import cromwell.pipeline.service.AuthorizationException.{
  AuthorizationFailureException,
  DuplicateUserException,
  InactiveUserException,
  IncorrectPasswordException,
  RegistrationFailureException,
  UserNotFoundException
}
import cromwell.pipeline.utils.StringUtils
import play.api.libs.json.Json

import scala.concurrent.{ ExecutionContext, Future }
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.Random

class AuthService(userRepository: UserRepository, authUtils: AuthUtils)(implicit executionContext: ExecutionContext) {

  import authUtils._
  val log: Logger = LoggerFactory.getLogger(getClass)

  def signIn(request: SignInRequest): Future[Option[AuthResponse]] = {

    def failMsg(cause: String): String = s"Authorization failure with email ${request.email} : $cause"

    val data: Either[NonEmptyChain[String], Future[Option[AuthResponse]]] = for {
      email <- logged(
        UserEmail.from(request.email),
        failMsg(invalidEmailMessage)
      ).toEither
      password <- logged(
        Password.from(request.password),
        failMsg(invalidPasswordMessage)
      ).toEither
    } yield takeUserFromRequest(email, password).map(responseFromUser)

    data match {
      case Right(value) => value
      case Left(error)  => Future.failed(AuthorizationFailureException(error.show))
    }
  }

  def signUp(request: SignUpRequest): Future[Option[AuthResponse]] = {

    def failMsg(cause: String): String = s"Registration failure with email ${request.email} : $cause"

    val data: Either[NonEmptyChain[String], Future[Option[AuthResponse]]] = for {
      email <- logged(
        UserEmail.from(request.email),
        failMsg(invalidEmailMessage)
      ).toEither
      firstname <- logged(
        Name.from(request.firstName),
        failMsg(s"$invalidNameMessage [${request.firstName}]")
      ).toEither
      lastname <- logged(
        Name.from(request.lastName),
        failMsg(s"$invalidNameMessage [${request.lastName}]")
      ).toEither
      password <- logged(
        Password.from(request.password),
        failMsg(invalidPasswordMessage)
      ).toEither
    } yield userRepository.getUserByEmail(email).flatMap {
      case Some(_) =>
        log.warn(failMsg(existUserMessage))
        Future.failed(DuplicateUserException(s"$email $existUserMessage"))
      case None =>
        val passwordSalt = Random.nextLong().toHexString
        val passwordHash = StringUtils.calculatePasswordHash(password.value, passwordSalt)
        val newUser = User(
          userId = UserId.random,
          email = email,
          passwordSalt = passwordSalt,
          passwordHash = passwordHash,
          firstName = firstname,
          lastName = lastname
        )
        userRepository.addUser(newUser).map { userId =>
          val accessTokenContent = AccessTokenContent(userId)
          val refreshTokenContent = RefreshTokenContent(userId, None)
          getAuthResponse(accessTokenContent, refreshTokenContent, Instant.now.getEpochSecond)
        }
    }

    data match {
      case Right(value) => value
      case Left(error)  => Future.failed(RegistrationFailureException(error.show))
    }
  }

  // Info: Do not move the logic of creating new access token content to another place,
  //       otherwise authentication testing will become a challenging task.
  //       In the future we will add another data into access token content with repositories help.
  def refreshTokens(refreshToken: String): Option[AuthResponse] = {
    val currentTimestamp = Instant.now.getEpochSecond
    getOptJwtClaims(refreshToken)
      .filter(_.expiration.exists(_ > currentTimestamp))
      .map(claims => Json.parse(claims.content).as[AuthContent])
      .collect {
        case refreshTokenContent: RefreshTokenContent =>
          val accessTokenContent = AccessTokenContent(refreshTokenContent.userId)
          getAuthResponse(accessTokenContent, refreshTokenContent, currentTimestamp)
      }
      .flatten
  }

  def takeUserFromRequest(email: UserEmail, password: Password): Future[User] = {

    def failMsg(cause: String): String = s"Authorization failure with email $email : $cause"

    userRepository.getUserByEmail(email).flatMap {
      case Some(user) =>
        passwordCorrect(password, user).orElse(userIsActive(user)) match {
          case Some(value) =>
            log.warn(failMsg(value.getMessage))
            Future.failed(value)
          case None => Future.successful(user)
        }
      case None =>
        log.warn(failMsg(userNotFoundMessage))
        Future.failed(UserNotFoundException(userNotFoundMessage))
    }
  }

  def responseFromUser(user: User): Option[AuthResponse] = {
    val accessTokenContent = AccessTokenContent(user.userId)
    val refreshTokenContent = RefreshTokenContent(user.userId, None)
    getAuthResponse(accessTokenContent, refreshTokenContent, Instant.now.getEpochSecond)
  }

  def passwordCorrect(password: Password, user: User): Option[Throwable] =
    if (user.passwordHash == StringUtils.calculatePasswordHash(password.value, user.passwordSalt)) {
      None
    } else {
      Some(IncorrectPasswordException(incorrectPasswordMessage))
    }

  def userIsActive(user: User): Option[Throwable] =
    if (user.active) {
      None
    } else {
      Some(InactiveUserException(inactiveUserMessage))
    }

  def logged[E: Show, A](validated: Validated[E, A], error: => String): Validated[E, A] =
    validated.leftMap { e =>
      log.warn(s"$error. Reason: [${e.show}]")
      e
    }
}

object AuthService {
  val incorrectPasswordMessage = "incorrect password"
  val inactiveUserMessage = "user is not active"
  val invalidPasswordMessage = "invalid password"
  val invalidEmailMessage = "invalid email"
  val invalidNameMessage = "invalid name"
  val userNotFoundMessage = "user has not been found"
  val existUserMessage = "already exists"
}

sealed abstract class AuthorizationException extends Exception { val message: String }

object AuthorizationException {
  case class IncorrectPasswordException(message: String) extends AuthorizationException()
  case class DuplicateUserException(message: String) extends AuthorizationException()
  case class InactiveUserException(message: String) extends AuthorizationException()
  case class UserNotFoundException(message: String) extends AuthorizationException()
  case class AuthorizationFailureException(message: String) extends AuthorizationException()
  case class RegistrationFailureException(message: String) extends AuthorizationException()
}
