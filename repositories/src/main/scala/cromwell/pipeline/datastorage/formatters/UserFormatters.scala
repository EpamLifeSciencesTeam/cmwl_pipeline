package cromwell.pipeline.datastorage.formatters

import cats.implicits.catsStdShowForString
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.{ Name, Password, UserEmail, UserId }
import play.api.libs.json.{ Format, Json, OFormat }

object UserFormatters {
  implicit lazy val userFormat: OFormat[User] = Json.format[User]
  implicit lazy val profilePictureFormat: OFormat[ProfilePicture] = Json.format[ProfilePicture]
  implicit val UserNoCredentialsFormat: OFormat[UserNoCredentials] =
    Json.format[UserNoCredentials]
  implicit lazy val updatePasswordRequestFormat: OFormat[PasswordUpdateRequest] = Json.format[PasswordUpdateRequest]
  implicit lazy val updateRequestFormat: OFormat[UserUpdateRequest] = Json.format[UserUpdateRequest]

  implicit lazy val nameFormat: Format[Name] = Name.wrapperFormat
  implicit lazy val passwordFormat: Format[Password] = Password.wrapperFormat
  implicit lazy val userEmailFormat: Format[UserEmail] = UserEmail.wrapperFormat
  implicit lazy val userIdFormat: Format[UserId] = UserId.wrapperFormat
}
