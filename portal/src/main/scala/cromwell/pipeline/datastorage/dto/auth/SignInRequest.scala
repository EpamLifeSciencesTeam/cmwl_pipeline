package cromwell.pipeline.datastorage.dto.auth

import cromwell.pipeline.datastorage.dto.UserEmail
import play.api.libs.json.{ Json, OFormat }

final case class SignInRequest(email: UserEmail, password: Password)

object SignInRequest {
  implicit val signInRequestFormat: OFormat[SignInRequest] = Json.format[SignInRequest]
}
