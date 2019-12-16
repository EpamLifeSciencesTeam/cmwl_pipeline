package cromwell.pipeline.datastorage.dto.user

import cromwell.pipeline.datastorage.dto.UserId
import play.api.libs.json.{ Json, OFormat }

final case class DeactivateUserByIdRequest(userId: UserId)

object DeactivateUserByIdRequest {
  implicit lazy val DeactivateUserRequestByIdFormat: OFormat[DeactivateUserByIdRequest] =
    Json.format[DeactivateUserByIdRequest]
}
