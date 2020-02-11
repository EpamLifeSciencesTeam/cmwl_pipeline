package cromwell.pipeline.datastorage.dto.project

import play.api.libs.json.{ Json, OFormat }

final case class ProjectAdditionRequest(name: String)

object ProjectAdditionRequest {
  implicit lazy val projectAdditionFormat: OFormat[ProjectAdditionRequest] = Json.format[ProjectAdditionRequest]
}
