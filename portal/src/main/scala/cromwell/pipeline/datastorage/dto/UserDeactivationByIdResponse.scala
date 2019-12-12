package cromwell.pipeline.datastorage.dto

import play.api.libs.json.{ Json, OFormat }

final case class UserDeactivationByIdResponse(userId: UserId, active: Boolean)

object UserDeactivationByIdResponse {
  implicit val UserDeactivationByIdResponseFormat: OFormat[UserDeactivationByIdResponse] =
    Json.format[UserDeactivationByIdResponse]
}
