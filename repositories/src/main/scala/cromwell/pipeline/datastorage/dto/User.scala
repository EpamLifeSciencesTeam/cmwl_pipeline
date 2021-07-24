package cromwell.pipeline.datastorage.dto

import cromwell.pipeline.model.wrapper.{ Name, UserEmail, UserId }
import play.api.libs.json.{ Json, OFormat }

final case class User(
  userId: UserId,
  email: UserEmail,
  firstName: Name,
  lastName: Name,
  active: Boolean
)

object User {
  implicit val userFormat: OFormat[User] =
    Json.format[User]

  def fromUserWithCredentials(user: UserWithCredentials): User =
    User(
      userId = user.userId,
      email = user.email,
      firstName = user.firstName,
      lastName = user.lastName,
      active = user.active
    )
}
