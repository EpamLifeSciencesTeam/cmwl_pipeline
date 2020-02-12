package cromwell.pipeline.datastorage.dto.project

import cromwell.pipeline.datastorage.dto.UserId
import play.api.libs.json.{ Json, OFormat }

final case class ProjectAdditionRequest(name: String)

object ProjectAdditionRequest {
  implicit lazy val projectAdditionFormat: OFormat[ProjectAdditionRequest] = Json.format[ProjectAdditionRequest]
}
