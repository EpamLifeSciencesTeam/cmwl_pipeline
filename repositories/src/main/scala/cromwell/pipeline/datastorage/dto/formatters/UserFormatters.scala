package cromwell.pipeline.datastorage.dto.formatters

import cromwell.pipeline.datastorage.dto.{User, UserId}
import play.api.libs.json.{Json, OFormat}
import slick.lifted.MappedTo

object UserFormatters {

  final case class ProfilePicture(value: Array[Byte]) extends MappedTo[Array[Byte]]

  object ProfilePicture {
    implicit lazy val profilePictureFormat: OFormat[ProfilePicture] = Json.format[ProfilePicture]
  }

  final case class PasswordUpdateRequest(currentPassword: String, newPassword: String, repeatPassword: String)

  object PasswordUpdateRequest {
    implicit val updatePasswordRequestFormat: OFormat[PasswordUpdateRequest] = Json.format[PasswordUpdateRequest]
  }

  final case class UserUpdateRequest(email: String, firstName: String, lastName: String)

  object UserUpdateRequest {
    implicit val updateRequestFormat: OFormat[UserUpdateRequest] = Json.format[UserUpdateRequest]
  }

  final case class UserNoCredentials(userId: UserId, email: String, firstName: String, lastName: String, active: Boolean)

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

}
