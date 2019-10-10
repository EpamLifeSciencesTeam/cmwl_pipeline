package cromwell.pipeline.datastorage.dto.auth

import play.api.libs.json.{Json, OFormat}

final case class AuthResponse(accessToken: String,
                              refreshToken: String,
                              accessTokenExpiration: Long)

object AuthResponse {
  implicit val authResponseFormat: OFormat[AuthResponse] = Json.format[AuthResponse]
}
