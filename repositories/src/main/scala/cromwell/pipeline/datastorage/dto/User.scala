package cromwell.pipeline.datastorage.dto

import cromwell.pipeline.datastorage.dto.User.UserEmail
import cromwell.pipeline.datastorage.dto.formatters.UserFormatters._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{ Format, Json, OFormat }
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

final case class ProfilePicture(value: Array[Byte]) extends MappedTo[Array[Byte]]

final case class PasswordUpdateRequest(currentPassword: String, newPassword: String, repeatPassword: String)

final case class UserUpdateRequest(email: String, firstName: String, lastName: String)

final case class UserNoCredentials(
  userId: UserId,
  email: String,
  firstName: String,
  lastName: String,
  active: Boolean
)

object UserNoCredentials {
  def fromUser(user: User): UserNoCredentials =
    UserNoCredentials(
      userId = user.userId,
      email = user.email,
      firstName = user.firstName,
      lastName = user.lastName,
      active = user.active
    )
}
