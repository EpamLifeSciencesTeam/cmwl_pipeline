package cromwell.pipeline.datastorage.dto

import play.api.libs.json.{ Json, OFormat }

final case class UserDeactivationResponse(
  userId: UserId,
  email: String,
  firstName: String,
  lastName: String,
  active: Boolean
)

object UserDeactivationResponse {
  implicit val UserDeactivationByIdResponseFormat: OFormat[UserDeactivationResponse] =
    Json.format[UserDeactivationResponse]

  def fromUser(user: User): UserDeactivationResponse =
    UserDeactivationResponse(
      userId = user.userId,
      email = user.email,
      firstName = user.firstName,
      lastName = user.lastName,
      active = user.active
    )
}
