package cromwell.pipeline.datastorage.dto

import play.api.libs.json.Format
import slick.lifted.MappedTo
import play.api.libs.functional.syntax._

final case class User(
  userId: UserId,
  email: String,
  passwordHash: String,
  passwordSalt: String,
  firstName: String,
  lastName: String,
  profilePicture: Option[ProfilePicture] = None,
  active: Boolean = true
)

final case class UserId(value: String) extends MappedTo[String]

object UserId {
  implicit lazy val userIdFormat: Format[UserId] = implicitly[Format[String]].inmap(UserId.apply, _.value)
}

final case class ProfilePicture(value: Array[Byte]) extends MappedTo[Array[Byte]]
