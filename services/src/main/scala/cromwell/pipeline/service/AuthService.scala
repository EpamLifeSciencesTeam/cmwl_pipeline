package cromwell.pipeline.service

import java.time.Instant

import cats.data.OptionT
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
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.service.AuthService.{ authorizationFailure, inactiveUserMessage }
import cromwell.pipeline.service.AuthorizationException.{ InactiveUserException, IncorrectPasswordException }
import cromwell.pipeline.utils.StringUtils
import play.api.libs.json.Json

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Random

class AuthService(userRepository: UserRepository, authUtils: AuthUtils)(implicit executionContext: ExecutionContext) {

  import authUtils._

  def signIn(request: SignInRequest): Future[Option[AuthResponse]] =
    takeUserFromRequest(request).subflatMap(responseFromUser).value

  def takeUserFromRequest(request: SignInRequest): OptionT[Future, User] =
    OptionT(userRepository.getUserByEmail(request.email)).semiflatMap[User] { user =>
      val checkFilters = passwordCorrect(request, user).orElse(userIsActive(user))
      checkFilters match {
        case Some(value) => Future.failed(value)
        case None        => Future.successful(user)
      }
    }

  def responseFromUser(user: User): Option[AuthResponse] = {
    val accessTokenContent = AccessTokenContent(user.userId)
    val refreshTokenContent = RefreshTokenContent(user.userId, None)
    getAuthResponse(accessTokenContent, refreshTokenContent, Instant.now.getEpochSecond)
  }

  def signUp(request: SignUpRequest): Future[Option[AuthResponse]] = {
    val passwordSalt = Random.nextLong().toHexString
    val passwordHash = StringUtils.calculatePasswordHash(request.password.value, passwordSalt)
    val newUser = User(
      userId = UserId.random,
      email = request.email,
      passwordSalt = passwordSalt,
      passwordHash = passwordHash,
      firstName = request.firstName,
      lastName = request.lastName
    )

    userRepository.addUser(newUser).map { userId =>
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

  def passwordCorrect(request: SignInRequest, user: User): Option[Throwable] =
    if (user.passwordHash == StringUtils.calculatePasswordHash(request.password.value, user.passwordSalt)) None
    else Some(IncorrectPasswordException(authorizationFailure))

  def userIsActive(user: User): Option[Throwable] =
    if (user.active) None else Some(InactiveUserException(inactiveUserMessage))
}

object AuthService {
  val authorizationFailure = "invalid email or password"
  val inactiveUserMessage = "User is not active"
}

sealed abstract class AuthorizationException extends Exception { val message: String }

object AuthorizationException {
  case class IncorrectPasswordException(message: String) extends AuthorizationException()
  case class InactiveUserException(message: String) extends AuthorizationException()

}
