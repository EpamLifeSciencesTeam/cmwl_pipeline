package cromwell.pipeline.datastorage.dto.management

import play.api.libs.json.{Json, OFormat}

final case class DeactivateUserRequest(email: String)

object DeactivateUserRequest {
  implicit val DeactivateRequestFormat: OFormat[DeactivateUserRequest] = Json.format[DeactivateUserRequest]
}
