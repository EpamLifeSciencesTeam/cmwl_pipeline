package cromwell.pipeline.service

import cats.data.OptionT
import cats.implicits._
import cromwell.pipeline.auth.AuthUtils
import cromwell.pipeline.datastorage.dto.UserWithCredentials
import cromwell.pipeline.datastorage.dto.auth._
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.AuthorizationException.{
  DuplicateUserException,
  InactiveUserException,
  IncorrectPasswordException
}
import cromwell.pipeline.utils.StringUtils
import play.api.libs.json.Json

import java.time.Instant
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Random

trait AuthService {

  def signIn(request: SignInRequest): Future[Option[AuthResponse]]

  def signUp(request: SignUpRequest): Future[Option[AuthResponse]]

  def refreshTokens(refreshToken: String): Option[AuthResponse]

  def responseFromUser(user: UserWithCredentials): Option[AuthResponse]

  def passwordCorrect(request: SignInRequest, user: UserWithCredentials): Option[Throwable]

  def userIsActive(user: UserWithCredentials): Option[Throwable]

}

object AuthService {

  val authorizationFailure = "invalid email or password"
  val inactiveUserMessage = "User is not active"

  def apply(userService: UserService, authUtils: AuthUtils)(
    implicit executionContext: ExecutionContext
  ): AuthService =
    new AuthService {

      import authUtils._

      def signIn(request: SignInRequest): Future[Option[AuthResponse]] =
        takeUserFromRequest(request).subflatMap(responseFromUser).value

      def signUp(request: SignUpRequest): Future[Option[AuthResponse]] =
        userService.getUserWithCredentialsByEmail(request.email).flatMap {
          case Some(_) => Future.failed(DuplicateUserException(s"${request.email} already exists"))
          case None =>
            val passwordSalt = Random.nextLong().toHexString
            val passwordHash = StringUtils.calculatePasswordHash(request.password, passwordSalt)
            val newUser = UserWithCredentials(
              userId = UserId.random,
              email = request.email,
              passwordSalt = passwordSalt,
              passwordHash = passwordHash,
              firstName = request.firstName,
              lastName = request.lastName
            )
            userService.addUser(newUser).map { userId =>
              val accessTokenContent = AccessTokenContent(userId)
              val refreshTokenContent = RefreshTokenContent(userId, None)
              getAuthResponse(accessTokenContent, refreshTokenContent, Instant.now.getEpochSecond)
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

      def responseFromUser(user: UserWithCredentials): Option[AuthResponse] = {
        val accessTokenContent = AccessTokenContent(user.userId)
        val refreshTokenContent = RefreshTokenContent(user.userId, None)
        getAuthResponse(accessTokenContent, refreshTokenContent, Instant.now.getEpochSecond)
      }

      def passwordCorrect(request: SignInRequest, user: UserWithCredentials): Option[Throwable] =
        if (user.passwordHash == StringUtils.calculatePasswordHash(request.password, user.passwordSalt)) None
        else Some(IncorrectPasswordException(authorizationFailure))

      def userIsActive(user: UserWithCredentials): Option[Throwable] =
        if (user.active) None else Some(InactiveUserException(inactiveUserMessage))

      private def takeUserFromRequest(request: SignInRequest): OptionT[Future, UserWithCredentials] =
        OptionT(userService.getUserWithCredentialsByEmail(request.email)).semiflatMap[UserWithCredentials] { user =>
          val checkFilters = passwordCorrect(request, user).orElse(userIsActive(user))
          checkFilters match {
            case Some(value) => Future.failed(value)
            case None        => Future.successful(user)
          }
        }

    }

}

sealed abstract class AuthorizationException extends Exception { val message: String }

object AuthorizationException {
  case class IncorrectPasswordException(message: String) extends AuthorizationException()
  case class DuplicateUserException(message: String) extends AuthorizationException()
  case class InactiveUserException(message: String) extends AuthorizationException()

}
