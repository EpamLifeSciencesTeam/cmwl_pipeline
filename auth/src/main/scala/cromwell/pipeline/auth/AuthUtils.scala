package cromwell.pipeline.auth

import cromwell.pipeline.datastorage.dto.auth.{ AccessTokenContent, AuthContent, AuthResponse, RefreshTokenContent }
import cromwell.pipeline.utils.AuthConfig
import pdi.jwt.{ Jwt, JwtClaim }
import play.api.libs.json.Json

class AuthUtils(authConfig: AuthConfig) {

  def getAuthResponse(
    accessTokenContent: AccessTokenContent,
    refreshTokenContent: RefreshTokenContent,
    currentTimestamp: Long
  ): Option[AuthResponse] = {

    import authConfig._

    def buildAuthResponse: AuthResponse = {
      val updatedAccessTokenLifetime = refreshTokenContent.optRestOfUserSession.map { restOfUserSession =>
        Seq(restOfUserSession, expirationTimeInSeconds.accessToken).min
      }.getOrElse(expirationTimeInSeconds.accessToken)

      val updatedRefreshTokenLifetime = refreshTokenContent.optRestOfUserSession.map { restOfUserSession =>
        Seq(restOfUserSession, expirationTimeInSeconds.refreshToken).min
      }.getOrElse(expirationTimeInSeconds.refreshToken)

      val updatedRefreshTokenContent = refreshTokenContent.optRestOfUserSession.map { restOfUserSession =>
        val updatedRestOfUserSession = restOfUserSession - updatedRefreshTokenLifetime
        refreshTokenContent.copy(optRestOfUserSession = Some(updatedRestOfUserSession))
      }.getOrElse {
        val restOfUserSession = expirationTimeInSeconds.userSession - expirationTimeInSeconds.refreshToken
        refreshTokenContent.copy(optRestOfUserSession = Some(restOfUserSession))
      }

      val newAccessToken = getAuthToken(accessTokenContent, currentTimestamp, updatedAccessTokenLifetime)
      val newRefreshToken = getAuthToken(updatedRefreshTokenContent, currentTimestamp, updatedRefreshTokenLifetime)

      AuthResponse(newAccessToken, newRefreshToken, currentTimestamp + updatedAccessTokenLifetime)
    }

    refreshTokenContent.optRestOfUserSession match {
      case optRestOfUserSession if optRestOfUserSession.exists(_ > 0) || optRestOfUserSession.isEmpty =>
        Some(buildAuthResponse)
      case _ =>
        None
    }
  }

  def getOptJwtClaims(refreshToken: String): Option[JwtClaim] =
    Jwt.decode(refreshToken, authConfig.secretKey, Seq(authConfig.hmacAlgorithm)).toOption

  private def getAuthToken(authContent: AuthContent, currentTimestamp: Long, lifetime: Long): String = {
    val claims = JwtClaim(
      content = Json.stringify(Json.toJson(authContent)),
      expiration = Some(currentTimestamp + lifetime),
      issuedAt = Some(currentTimestamp)
    )
    Jwt.encode(claims, authConfig.secretKey, authConfig.hmacAlgorithm)
  }

}
