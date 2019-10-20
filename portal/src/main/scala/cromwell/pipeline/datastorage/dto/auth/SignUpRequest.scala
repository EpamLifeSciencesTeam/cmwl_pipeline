package cromwell.pipeline.datastorage.dto.auth

import play.api.libs.json.{Json, OFormat}

final case class SignUpRequest(email: String,
                               password: String,
                               firstName: String,
                               lastName: String)

object SignUpRequest {
  implicit val SignUpRequestFormat: OFormat[SignUpRequest] = Json.format[SignUpRequest]
}
