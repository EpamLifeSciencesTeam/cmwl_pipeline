package cromwell.pipeline.datastorage.dto.formatters

import play.api.libs.json.{ Json, OFormat }

object AuthFormatters {

  final case class AuthResponse(accessToken: String, refreshToken: String, accessTokenExpiration: Long)

  object AuthResponse {
    implicit val authResponseFormat: OFormat[AuthResponse] = Json.format[AuthResponse]
  }

  final case class SignInRequest(email: String, password: String)

  object SignInRequest {
    implicit val signInRequestFormat: OFormat[SignInRequest] = Json.format[SignInRequest]
  }

  final case class SignUpRequest(email: String, password: String, firstName: String, lastName: String)

  object SignUpRequest {
    implicit val signUpRequestFormat: OFormat[SignUpRequest] = Json.format[SignUpRequest]
  }

  final case class PasswordProblemsResponse(value:String, valid: Boolean = false, errors: List[Map[String,String]])

  object PasswordProblemsResponse {
    implicit  val passwordProblemsResponseFormat: OFormat[PasswordProblemsResponse] = Json.format[PasswordProblemsResponse]
  }

}
