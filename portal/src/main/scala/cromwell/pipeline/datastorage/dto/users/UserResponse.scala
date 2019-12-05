package cromwell.pipeline.datastorage.dto.user

import cromwell.pipeline.datastorage.dto.{User}
import play.api.libs.json.{Json, OFormat}

final case class UserResponse(user: User)

object UserResponse {
  implicit val userFormat: OFormat[User] = Json.format[User]
  implicit val userResponseFormat: OFormat[UserResponse] = Json.format[UserResponse]
}