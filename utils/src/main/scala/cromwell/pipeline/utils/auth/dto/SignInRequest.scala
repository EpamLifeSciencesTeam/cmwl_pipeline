package cromwell.pipeline.utils.auth.dto

import cromwell.pipeline.model.wrapper.{ Password, UserEmail }
import play.api.libs.json.{ Json, OFormat }

final case class SignInRequest(email: UserEmail, password: Password)

object SignInRequest {
  implicit val signInRequestFormat: OFormat[SignInRequest] = Json.format[SignInRequest]
}
