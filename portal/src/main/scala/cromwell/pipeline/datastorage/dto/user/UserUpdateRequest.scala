package cromwell.pipeline.datastorage.dto.user

import play.api.libs.json.{ Json, OFormat }

final case class UserUpdateRequest(email: String, firstName: String, lastName: String)

object UserUpdateRequest {
  implicit val UpdateRequestFormat: OFormat[UserUpdateRequest] = Json.format[UserUpdateRequest]
}
