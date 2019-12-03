package cromwell.pipeline.datastorage.dto.management


import play.api.libs.json.{Json, OFormat}

final case class DeactivateUserRequestByEmail(email: String)

object DeactivateUserRequestByEmail {

  implicit val DeactivateUserRequestByEmailFormat: OFormat[DeactivateUserRequestByEmail] = Json.format[DeactivateUserRequestByEmail]

}


