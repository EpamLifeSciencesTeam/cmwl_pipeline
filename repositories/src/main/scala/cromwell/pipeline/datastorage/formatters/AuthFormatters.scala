package cromwell.pipeline.datastorage.formatters

import cromwell.pipeline.datastorage.dto.{ AuthResponse, SignInRequest, SignUpRequest }
import cromwell.pipeline.datastorage.utils.auth.{ AccessTokenContent, AuthContent, RefreshTokenContent }
import play.api.libs.json.{ Json, OFormat }
import cromwell.pipeline.datastorage.formatters.UserFormatters._

object AuthFormatters {
  implicit val authContentFormat: OFormat[AuthContent] = Json.format[AuthContent]
  implicit val accessTokenContentFormat: OFormat[AccessTokenContent] = Json.format[AccessTokenContent]
  implicit val refreshTokenContentFormat: OFormat[RefreshTokenContent] = Json.format[RefreshTokenContent]
  implicit val authResponseFormat: OFormat[AuthResponse] = Json.format[AuthResponse]
  implicit val signInRequestFormat: OFormat[SignInRequest] = Json.format[SignInRequest]
  implicit val signUpRequestFormat: OFormat[SignUpRequest] = Json.format[SignUpRequest]
}
