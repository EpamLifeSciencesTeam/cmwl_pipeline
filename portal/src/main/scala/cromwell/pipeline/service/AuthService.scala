package cromwell.pipeline.service

import java.time.Instant
import java.util.UUID

import cats.data.OptionT
import cats.implicits._
import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.auth.{ AuthResponse, SignInRequest, SignUpRequest }
import cromwell.pipeline.datastorage.dto.{ User, UserId }
import cromwell.pipeline.utils.StringUtils
import cromwell.pipeline.utils.auth.{ AccessTokenContent, AuthContent, AuthUtils, RefreshTokenContent }
import play.api.libs.json.Json

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Random

class AuthService(userRepository: UserRepository, authUtils: AuthUtils)(implicit executionContext: ExecutionContext) {

  import authUtils._

  def signIn(request: SignInRequest): Future[Option[AuthResponse]] =
    OptionT(userRepository.getUserByEmail(request.email))
      .filter(user => user.passwordHash == StringUtils.calculatePasswordHash(request.password, user.passwordSalt))
      .map { user =>
        val accessTokenContent = AccessTokenContent(user.userId.value)
        val refreshTokenContent = RefreshTokenContent(user.userId.value, None)
        getAuthResponse(accessTokenContent, refreshTokenContent, Instant.now.getEpochSecond)
      }
      .value
      .map(_.flatten)

  def signUp(request: SignUpRequest): Future[Option[AuthResponse]] = {
    val passwordSalt = Random.nextLong().toHexString
    val passwordHash = StringUtils.calculatePasswordHash(request.password, passwordSalt)
    val newUser = User(
      userId = UserId(UUID.randomUUID().toString),
      email = request.email,
      passwordSalt = passwordSalt,
      passwordHash = passwordHash,
      firstName = request.firstName,
      lastName = request.lastName
    )

    userRepository.addUser(newUser).map { userId =>
      val accessTokenContent = AccessTokenContent(userId.value)
      val refreshTokenContent = RefreshTokenContent(userId.value, None)
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

}
