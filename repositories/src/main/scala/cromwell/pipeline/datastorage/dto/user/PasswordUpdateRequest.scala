package cromwell.pipeline.datastorage.dto.user

import cromwell.pipeline.model.wrapper.Password
import play.api.libs.json.{ Json, OFormat }

final case class PasswordUpdateRequest(currentPassword: Password, newPassword: Password, repeatPassword: Password)

object PasswordUpdateRequest {
  implicit val updatePasswordRequestFormat: OFormat[PasswordUpdateRequest] = Json.format[PasswordUpdateRequest]
}
