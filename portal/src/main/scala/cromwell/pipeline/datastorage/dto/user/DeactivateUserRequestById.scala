package cromwell.pipeline.datastorage.dto.user

import cromwell.pipeline.datastorage.dto.UserId
import play.api.libs.json.{ Format, Json, OFormat }
import play.api.libs.functional.syntax._

final case class DeactivateUserRequestById(userId: UserId)

object DeactivateUserRequestById {
//
//  implicit val stringFormat: Format[String] = ???

  implicit lazy val userIdFormat: Format[UserId] = implicitly[Format[String]].inmap(UserId, _.value)
  //  implicit val userIdFormat: OFormat[UserId] = Json.format[UserId]
  implicit lazy val DeactivateUserRequestByIdFormat: OFormat[DeactivateUserRequestById] =
    Json.format[DeactivateUserRequestById]
}
