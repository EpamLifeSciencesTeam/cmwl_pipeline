package cromwell.pipeline.datastorage.dto

import cromwell.pipeline.datastorage.dto.User.UserEmail
import cromwell.pipeline.datastorage.dto.formatters.UserFormatters.ProfilePicture
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{Format, Json, OFormat}
import slick.lifted.MappedTo

final case class User(
  userId: UserId,
  email: UserEmail,
  passwordHash: String,
  passwordSalt: String,
  firstName: String,
  lastName: String,
  profilePicture: Option[ProfilePicture] = None,
  active: Boolean = true
)

object User {
  implicit lazy val userFormat: OFormat[User] = Json.format[User]
  type UserEmail = String
}

final case class UserId(value: String) extends MappedTo[String]

object UserId {
  implicit lazy val userIdFormat: Format[UserId] = implicitly[Format[String]].inmap(UserId.apply, _.value)
}

