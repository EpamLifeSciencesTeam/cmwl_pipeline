package cromwell.pipeline.datastorage.dto.formatters

import cromwell.pipeline.datastorage.dto.auth.{ AuthResponse, PasswordProblemsResponse, SignInRequest, SignUpRequest }
import play.api.libs.json.{ Json, OFormat }

object AuthFormatters {
  implicit val authResponseFormat: OFormat[AuthResponse] = Json.format[AuthResponse]
  implicit val signInRequestFormat: OFormat[SignInRequest] = Json.format[SignInRequest]
  implicit val signUpRequestFormat: OFormat[SignUpRequest] = Json.format[SignUpRequest]
  implicit val passwordProblemsResponseFormat: OFormat[PasswordProblemsResponse] = Json.format[PasswordProblemsResponse]
}
