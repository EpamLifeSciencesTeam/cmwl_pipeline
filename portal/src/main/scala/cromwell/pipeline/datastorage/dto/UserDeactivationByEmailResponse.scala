package cromwell.pipeline.datastorage.dto

import play.api.libs.json.{ Json, OFormat }

final case class UserDeactivationByEmailResponse(email: String, active: Boolean)

object UserDeactivationByEmailResponse {
  implicit val UserDeactivationResponseFormat: OFormat[UserDeactivationByEmailResponse] =
    Json.format[UserDeactivationByEmailResponse]
}
