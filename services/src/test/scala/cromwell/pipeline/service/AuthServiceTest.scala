package cromwell.pipeline.service

import java.time.Instant

import cromwell.pipeline.auth.AuthUtils
import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.auth.{ AccessTokenContent, AuthContent, AuthResponse, RefreshTokenContent }
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.utils.{ AuthConfig, ExpirationTimeInSeconds }
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Matchers, WordSpec }
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{ Jwt, JwtAlgorithm, JwtClaim }
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext

class AuthServiceTest extends WordSpec with Matchers with MockFactory {

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private val authConfig = AuthConfig(
    secretKey = "secretKey",
    hmacAlgorithm = JwtAlgorithm.fromString(algo = "HS256").asInstanceOf[JwtHmacAlgorithm],
    expirationTimeInSeconds = ExpirationTimeInSeconds(accessToken = 300, refreshToken = 900, userSession = 3600)
  )

  import authConfig._

  private val userRepository: UserRepository = stub[UserRepository]
  private val authUtils: AuthUtils = stub[AuthUtils]
  private val authService: AuthService = new AuthService(userRepository, authUtils)
  private val userId = UserId.random

  "AuthServiceTest" when {

    "refreshTokens" should {

      "return Auth response for active refresh token" taggedAs Service in new RefreshTokenContext(
        expirationTimeInSeconds.refreshToken
      ) {
        authService.refreshTokens(refreshToken) shouldBe Some(authResponse)
      }

      "return None for outdated refresh token" taggedAs Service in new RefreshTokenContext(lifetime = 0) {
        authService.refreshTokens(refreshToken) shouldBe None
      }

      "return None for another type of token" taggedAs Service in {
        val currentTimestamp = Instant.now.getEpochSecond
        val accessTokenContent: AuthContent = AccessTokenContent(userId = userId)
        val accessTokenClaims = JwtClaim(
          content = Json.stringify(Json.toJson(accessTokenContent)),
          expiration = Some(currentTimestamp + expirationTimeInSeconds.accessToken),
          issuedAt = Some(currentTimestamp)
        )
        val accessToken = Jwt.encode(accessTokenClaims, secretKey, hmacAlgorithm)

        (authUtils.getOptJwtClaims _ when accessToken).returns(Some(accessTokenClaims))

        authService.refreshTokens(accessToken) shouldBe None
      }

      "return None for wrong token" taggedAs Service in {
        val wrongToken = "wrongToken"

        (authUtils.getOptJwtClaims _ when wrongToken).returns(None)

        authService.refreshTokens(wrongToken) shouldBe None
      }
    }
  }

  class RefreshTokenContext(lifetime: Long) {
    val currentTimestamp: Long = Instant.now.getEpochSecond
    val refreshTokenContent: AuthContent = RefreshTokenContent(
      userId = userId,
      optRestOfUserSession = Some(expirationTimeInSeconds.userSession - lifetime)
    )
    val refreshTokenClaims: JwtClaim = JwtClaim(
      content = Json.stringify(Json.toJson(refreshTokenContent)),
      expiration = Some(currentTimestamp + lifetime),
      issuedAt = Some(currentTimestamp)
    )
    val refreshToken: String = Jwt.encode(refreshTokenClaims, secretKey, hmacAlgorithm)
    val authResponse: AuthResponse = AuthResponse("accessToken", "refreshToken", expirationTimeInSeconds.accessToken)

    (authUtils.getOptJwtClaims _ when refreshToken).returns(Some(refreshTokenClaims))
    (authUtils.getAuthResponse _ when (*, *, *)).returns(Some(authResponse))
  }

}
