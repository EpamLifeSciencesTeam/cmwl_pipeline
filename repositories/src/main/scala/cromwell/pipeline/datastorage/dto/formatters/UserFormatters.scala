package cromwell.pipeline.datastorage.dto.formatters

import cromwell.pipeline.datastorage.dto.{ PasswordUpdateRequest, ProfilePicture, UserNoCredentials, UserUpdateRequest }
import play.api.libs.json.{ Json, OFormat }

object UserFormatters {
  implicit val profilePictureFormat: OFormat[ProfilePicture] = Json.format[ProfilePicture]
  implicit val updatePasswordRequestFormat: OFormat[PasswordUpdateRequest] = Json.format[PasswordUpdateRequest]
  implicit val updateRequestFormat: OFormat[UserUpdateRequest] = Json.format[UserUpdateRequest]
  implicit val UserNoCredentialsFormat: OFormat[UserNoCredentials] = Json.format[UserNoCredentials]
}
