package cromwell.pipeline.datastorage.dto.user

import cromwell.pipeline.datastorage.dto.{ Name, UserEmail }
import play.api.libs.json.{ Json, OFormat }

final case class UserUpdateRequest(email: UserEmail, firstName: Name, lastName: Name)

object UserUpdateRequest {
  implicit val updateRequestFormat: OFormat[UserUpdateRequest] = Json.format[UserUpdateRequest]
}
