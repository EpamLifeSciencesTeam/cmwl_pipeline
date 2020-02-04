package cromwell.pipeline.datastorage.dto.user

import cromwell.pipeline.datastorage.dto.{ FirstName, LastName, UserEmail }
import play.api.libs.json.{ Json, OFormat }

final case class UserUpdateRequest(email: UserEmail, firstName: FirstName, lastName: LastName)

object UserUpdateRequest {
  implicit val updateRequestFormat: OFormat[UserUpdateRequest] = Json.format[UserUpdateRequest]
}
