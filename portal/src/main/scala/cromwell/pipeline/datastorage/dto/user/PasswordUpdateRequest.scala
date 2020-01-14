package cromwell.pipeline.datastorage.dto.user

import play.api.libs.json.{ Json, OFormat }

final case class PasswordUpdateRequest(currentPassword: String, newPassword: String, repeatPassword: String)

object PasswordUpdateRequest {
  implicit val updatePasswordRequestFormat: OFormat[PasswordUpdateRequest] = Json.format[PasswordUpdateRequest]
}
