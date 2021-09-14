package cromwell.pipeline.auth.impls

import cromwell.pipeline.auth.AuthUtils
import cromwell.pipeline.datastorage.dto.auth.{ AccessTokenContent, AuthResponse, RefreshTokenContent }
import pdi.jwt.JwtClaim

class AuthUtilsTestImpl(authResponses: List[AuthResponse], jwtClaims: List[JwtClaim]) extends AuthUtils {

  override def getAuthResponse(
    accessTokenContent: AccessTokenContent,
    refreshTokenContent: RefreshTokenContent,
    currentTimestamp: Long
  ): Option[AuthResponse] =
    authResponses.headOption

  override def getOptJwtClaims(refreshToken: String): Option[JwtClaim] =
    jwtClaims.headOption

}

object ProjectVersioningTestImpl {

  def apply(
    authResponses: List[AuthResponse] = Nil,
    jwtClaims: List[JwtClaim] = Nil
  ): AuthUtilsTestImpl =
    new AuthUtilsTestImpl(authResponses, jwtClaims)

}
