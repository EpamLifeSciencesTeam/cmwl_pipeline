package cromwell.pipeline.datastorage.dto.user

import cromwell.pipeline.datastorage.dto.UserId
import play.api.libs.json.{ Json, OFormat }

final case class DeactivateUserRequestById(userId: UserId)

object DeactivateUserRequestById {
  implicit lazy val DeactivateUserRequestByIdFormat: OFormat[DeactivateUserRequestById] =
    Json.format[DeactivateUserRequestById]
}
