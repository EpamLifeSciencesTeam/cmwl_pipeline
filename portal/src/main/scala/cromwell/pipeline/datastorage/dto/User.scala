package cromwell.pipeline.datastorage.dto

import cromwell.pipeline.datastorage.dto.User.UserEmail
import cromwell.pipeline.utils.validator.{
  DomainValidation,
  EmailDoesNotMeetCriteria,
  FirstNameHasSpecialCharacters,
  LastNameHasSpecialCharacters
}
import play.api.libs.json.{ Json, OFormat }
import play.api.libs.json.Format
import slick.lifted.MappedTo
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

final case class User(
  userId: UserId,
  email: UserEmail,
  passwordHash: String,
  passwordSalt: String,
  firstName: String,
  lastName: String,
  profilePicture: Option[ProfilePicture] = None,
  active: Boolean = true
) {
  require(DomainValidation.checkEmail(email), EmailDoesNotMeetCriteria.errorMessage)
  require(DomainValidation.checkFirstName(firstName), FirstNameHasSpecialCharacters.errorMessage)
  require(DomainValidation.checkLastName(lastName), LastNameHasSpecialCharacters.errorMessage)
}

object User {
  implicit lazy val userFormat: OFormat[User] = Json.format[User]
  type UserEmail = String
}

final case class UserId(value: String) extends MappedTo[String]

object UserId {
  implicit lazy val userIdFormat: Format[UserId] = implicitly[Format[String]].inmap(UserId.apply, _.value)
}

final case class ProfilePicture(value: Array[Byte]) extends MappedTo[Array[Byte]]

object ProfilePicture {
  implicit lazy val profilePictureFormat: OFormat[ProfilePicture] = OFormat(profilePictureReads, profilePictureWrites)

  lazy val profilePictureWrites = new OWrites[ProfilePicture] {
    def writes(profilePicture: ProfilePicture) = Json.obj(
      "value" -> None
    )
  }

  lazy val profilePictureReads: Reads[ProfilePicture] =
    (JsPath \ "value").read(Reads.of[Array[Byte]]).map(ProfilePicture.apply _)
}
