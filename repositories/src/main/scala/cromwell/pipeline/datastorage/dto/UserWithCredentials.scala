package cromwell.pipeline.datastorage.dto

import cromwell.pipeline.model.wrapper.{ Name, UserEmail, UserId }
import play.api.libs.json.{ Json, OFormat }
import slick.lifted.MappedTo

final case class UserWithCredentials(
  userId: UserId,
  email: UserEmail,
  passwordHash: String,
  passwordSalt: String,
  firstName: Name,
  lastName: Name,
  profilePicture: Option[ProfilePicture] = None,
  active: Boolean = true
)

object UserWithCredentials {
  implicit lazy val userFormat: OFormat[UserWithCredentials] = Json.format[UserWithCredentials]
}

final case class ProfilePicture(value: Array[Byte]) extends MappedTo[Array[Byte]]

object ProfilePicture {
  implicit lazy val profilePictureFormat: OFormat[ProfilePicture] = Json.format[ProfilePicture]
}
