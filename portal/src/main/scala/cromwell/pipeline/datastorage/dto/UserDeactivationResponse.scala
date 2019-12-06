package cromwell.pipeline.datastorage.dto

import play.api.libs.json.{ Json, OFormat }

final case class UserDeactivationResponse(email: String, active: Boolean)

object UserDeactivationResponse {
  implicit val UserDeactivationResponseFormat: OFormat[UserDeactivationResponse] = Json.format[UserDeactivationResponse]
}
