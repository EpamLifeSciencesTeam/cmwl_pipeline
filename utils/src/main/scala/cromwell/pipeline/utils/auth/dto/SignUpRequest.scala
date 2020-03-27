package cromwell.pipeline.utils.auth.dto

import cromwell.pipeline.model.wrapper.{ Name, Password, UserEmail }
import play.api.libs.json.{ Json, OFormat }

final case class SignUpRequest(email: UserEmail, password: Password, firstName: Name, lastName: Name)

object SignUpRequest {
  implicit val signUpRequestFormat: OFormat[SignUpRequest] = Json.format[SignUpRequest]
}
