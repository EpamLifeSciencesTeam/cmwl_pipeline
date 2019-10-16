package cromwell.pipeline.datastorage.dto.auth

import play.api.libs.json.{Json, OFormat}

final case class SignInRequest(email: String,
                               password: String)

object SignInRequest {
  implicit val SignInRequestFormat: OFormat[SignInRequest] = Json.format[SignInRequest]
}
