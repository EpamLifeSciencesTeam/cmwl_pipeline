package cromwell.pipeline.datastorage.dto

import cromwell.pipeline.model.wrapper.{ Name, UserEmail, UserId }
import play.api.libs.json.{ Json, OFormat }

final case class UserNoCredentials(
  userId: UserId,
  email: UserEmail,
  firstName: Name,
  lastName: Name,
  active: Boolean
)

object UserNoCredentials {
  implicit val UserNoCredentialsFormat: OFormat[UserNoCredentials] =
    Json.format[UserNoCredentials]

  def fromUser(user: User): UserNoCredentials =
    UserNoCredentials(
      userId = user.userId,
      email = user.email,
      firstName = user.firstName,
      lastName = user.lastName,
      active = user.active
    )
}
