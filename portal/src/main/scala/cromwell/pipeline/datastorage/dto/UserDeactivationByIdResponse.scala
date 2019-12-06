package cromwell.pipeline.datastorage.dto

import play.api.libs.functional.syntax._
import play.api.libs.json.{ Format, Json, OFormat }

final case class UserDeactivationByIdResponse(userId: UserId, active: Boolean)

object UserDeactivationByIdResponse {
//  implicit val userIdFormat: OFormat[UserId] = Json.format[UserId]
  implicit lazy val userIdFormat: Format[UserId] = implicitly[Format[String]].inmap(UserId, _.value)
  implicit val UserDeactivationByIdResponseFormat: OFormat[UserDeactivationByIdResponse] =
    Json.format[UserDeactivationByIdResponse]
}
