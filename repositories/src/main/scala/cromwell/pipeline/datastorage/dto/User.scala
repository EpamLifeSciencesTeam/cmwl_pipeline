package cromwell.pipeline.datastorage.dto

import cromwell.pipeline.model.wrapper.{ Name, Password, UserEmail, UserId }
import slick.lifted.MappedTo

final case class User(
  userId: UserId,
  email: UserEmail,
  passwordHash: String,
  passwordSalt: String,
  firstName: Name,
  lastName: Name,
  profilePicture: Option[ProfilePicture] = None,
  active: Boolean = true
)

final case class ProfilePicture(value: Array[Byte]) extends MappedTo[Array[Byte]]

final case class UserUpdateRequest(email: UserEmail, firstName: Name, lastName: Name)

final case class PasswordUpdateRequest(currentPassword: Password, newPassword: Password, repeatPassword: Password)

final case class UserNoCredentials(
  userId: UserId,
  email: UserEmail,
  firstName: Name,
  lastName: Name,
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
