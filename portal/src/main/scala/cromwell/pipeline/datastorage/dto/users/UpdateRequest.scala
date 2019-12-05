package cromwell.pipeline.datastorage.dto.user

import play.api.libs.json.{Json, OFormat}

final case class UpdateRequest(email: String, password: String, firstName: String, lastName: String)

object UpdateRequest {
  implicit val UpdateRequestFormat: OFormat[UpdateRequest] = Json.format[UpdateRequest]
}
