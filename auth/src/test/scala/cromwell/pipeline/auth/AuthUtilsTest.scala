package cromwell.pipeline.auth

import java.time.Instant

import cromwell.pipeline.datastorage.dto.auth.{ AccessTokenContent, AuthResponse, RefreshTokenContent }
import cromwell.pipeline.model.wrapper.UserId
import cromwell.pipeline.utils.{ AuthConfig, ExpirationTimeInSeconds }
import org.scalatest.{ Assertion, Matchers, WordSpec }
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{ Jwt, JwtAlgorithm }
import play.api.libs.json.Json

class AuthUtilsTest extends WordSpec with Matchers {

  private val authConfig = AuthConfig(
    secretKey = "secretKey",
    hmacAlgorithm = JwtAlgorithm.fromString(algo = "HS256").asInstanceOf[JwtHmacAlgorithm],
    expirationTimeInSeconds = ExpirationTimeInSeconds(accessToken = 300, refreshToken = 900, userSession = 3600)
  )
  private val authUtils = new AuthUtils(authConfig)
  private val userId = UserId.random

  "AuthUtils" when {

    "getAuthResponse" should {

      "return Auth response for new user session" in {

        val accessTokenContent = AccessTokenContent(userId)
        val refreshTokenContent = RefreshTokenContent(userId, None)
        val initTimestamp = Instant.now.getEpochSecond

        val optAuthResponse = authUtils.getAuthResponse(accessTokenContent, refreshTokenContent, initTimestamp)

        optAuthResponse match {
          case Some(authResponse) =>
            checkAuthResponse(
              authResponse = authResponse,
              accessTokenExpiration = initTimestamp + authConfig.expirationTimeInSeconds.accessToken,
              refreshTokenExpiration = initTimestamp + authConfig.expirationTimeInSeconds.refreshToken,
              restOfUserSession = authConfig.expirationTimeInSeconds.userSession - authConfig.expirationTimeInSeconds.refreshToken
            )
          case _ =>
            fail
        }

      }

      "return updated Auth response for a user session which is ending later than both of tokens" in {

        val updatedRestOfUserSession = authConfig.expirationTimeInSeconds.userSession - authConfig.expirationTimeInSeconds.refreshToken
        val accessTokenContent = AccessTokenContent(userId)
        val refreshTokenContent = RefreshTokenContent(userId, Some(updatedRestOfUserSession))
        val initTimestamp = Instant.now.getEpochSecond

        val optAuthResponse = authUtils.getAuthResponse(accessTokenContent, refreshTokenContent, initTimestamp)

        optAuthResponse match {
          case Some(authResponse) =>
            checkAuthResponse(
              authResponse = authResponse,
              accessTokenExpiration = initTimestamp + authConfig.expirationTimeInSeconds.accessToken,
              refreshTokenExpiration = initTimestamp + authConfig.expirationTimeInSeconds.refreshToken,
              restOfUserSession = updatedRestOfUserSession - authConfig.expirationTimeInSeconds.refreshToken
            )
          case _ =>
            fail
        }
      }

      "return updated Auth response for a user session which is ending later than an access token but earlier than refresh token" in {

        val tokensLifetimeDiff = authConfig.expirationTimeInSeconds.refreshToken - authConfig.expirationTimeInSeconds.accessToken
        val updatedRestOfUserSession = authConfig.expirationTimeInSeconds.accessToken + tokensLifetimeDiff / 2
        val accessTokenContent = AccessTokenContent(userId)
        val refreshTokenContent = RefreshTokenContent(userId, Some(updatedRestOfUserSession))
        val initTimestamp = Instant.now.getEpochSecond

        val optAuthResponse = authUtils.getAuthResponse(accessTokenContent, refreshTokenContent, initTimestamp)

        optAuthResponse match {
          case Some(authResponse) =>
            checkAuthResponse(
              authResponse = authResponse,
              accessTokenExpiration = initTimestamp + authConfig.expirationTimeInSeconds.accessToken,
              refreshTokenExpiration = initTimestamp + updatedRestOfUserSession,
              restOfUserSession = 0
            )
          case _ =>
            fail
        }
      }

      "return updated Auth response for a user session which is ending earlier than both of tokens" in {

        val updatedRestOfUserSession = authConfig.expirationTimeInSeconds.accessToken / 2
        val accessTokenContent = AccessTokenContent(userId)
        val refreshTokenContent = RefreshTokenContent(userId, Some(updatedRestOfUserSession))
        val initTimestamp = Instant.now.getEpochSecond

        val optAuthResponse = authUtils.getAuthResponse(accessTokenContent, refreshTokenContent, initTimestamp)

        optAuthResponse match {
          case Some(authResponse) =>
            checkAuthResponse(
              authResponse = authResponse,
              accessTokenExpiration = initTimestamp + updatedRestOfUserSession,
              refreshTokenExpiration = initTimestamp + updatedRestOfUserSession,
              restOfUserSession = 0
            )
          case _ =>
            fail
        }
      }

      "return None for expires user session" in {

        val updatedRestOfUserSession = 0
        val accessTokenContent = AccessTokenContent(userId)
        val refreshTokenContent = RefreshTokenContent(userId, Some(updatedRestOfUserSession))
        val initTimestamp = Instant.now.getEpochSecond

        val optAuthResponse = authUtils.getAuthResponse(accessTokenContent, refreshTokenContent, initTimestamp)

        optAuthResponse shouldBe None
      }

    }

  }

  def checkAuthResponse(
    authResponse: AuthResponse,
    accessTokenExpiration: Long,
    refreshTokenExpiration: Long,
    restOfUserSession: Long
  ): Assertion = {
    val result = for {
      accessTokenClaims <- Jwt.decode(authResponse.accessToken, authConfig.secretKey, Seq(authConfig.hmacAlgorithm))
      refreshTokenClaims <- Jwt.decode(authResponse.refreshToken, authConfig.secretKey, Seq(authConfig.hmacAlgorithm))
    } yield {
      val accessTokenContent = Json.parse(accessTokenClaims.content).as[AccessTokenContent]
      val refreshTokenContent = Json.parse(refreshTokenClaims.content).as[RefreshTokenContent]

      val isAccessTokenExpirationEqual = accessTokenClaims.expiration.contains(accessTokenExpiration)
      val isAccessTokenExpirationCorrect = accessTokenClaims.expiration.contains(accessTokenExpiration)
      val isRefreshTokenExpirationCorrect = refreshTokenClaims.expiration.contains(refreshTokenExpiration)
      val isRestOfUserSessionCorrect = refreshTokenContent.optRestOfUserSession.contains(restOfUserSession)
      val isUserIdEqual = Seq(accessTokenContent.userId, refreshTokenContent.userId).forall(_ == userId)

      isAccessTokenExpirationEqual && isAccessTokenExpirationCorrect && isRefreshTokenExpirationCorrect &&
      isRestOfUserSessionCorrect && isUserIdEqual
    }

    result.toOption.getOrElse(false) shouldBe true
  }

}
