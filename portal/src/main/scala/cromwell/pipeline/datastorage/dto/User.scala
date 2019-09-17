package cromwell.pipeline.datastorage.dto

import play.api.libs.json.{ Json, OFormat }
import slick.lifted.MappedTo

final case class User(
  userId: UserId,
  email: String = "",
  passwordHash: String = "",
  passwordSalt: String = "",
  firstName: String = "",
  lastName: String = "",
  profilePicture: Option[ProfilePicture] = None
)

final case class UserId(value: String) extends MappedTo[String]
object UserId {
  implicit val userIdFormat: OFormat[UserId] = Json.format[UserId]
}

final case class ProfilePicture(value: Array[Byte]) extends MappedTo[Array[Byte]]
object ProfilePicture {
  implicit val profilePictureFormat: OFormat[ProfilePicture] = Json.format[ProfilePicture]
}
