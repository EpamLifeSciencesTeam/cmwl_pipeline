package cromwell.pipeline.datastorage.dto.management

import cromwell.pipeline.datastorage.dto.UserId
import play.api.libs.json.{Json, OFormat}

final case class DeactivateUserRequestById(userId: UserId)

object DeactivateUserRequestById {

  implicit val userIdFormat: OFormat[UserId] = Json.format[UserId]
  implicit val DeactivateUserRequestByIdFormat: OFormat[DeactivateUserRequestById] = Json.format[DeactivateUserRequestById]
}
