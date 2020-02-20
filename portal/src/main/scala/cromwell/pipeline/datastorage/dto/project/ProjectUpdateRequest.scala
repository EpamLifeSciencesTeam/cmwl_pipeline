package cromwell.pipeline.datastorage.dto.project

import cromwell.pipeline.datastorage.dto.ProjectId
import play.api.libs.json.{ Json, OFormat }

final case class ProjectUpdateRequest(projectId: ProjectId, name: String, repository: String)

object ProjectUpdateRequest {
  implicit val updateRequestFormat: OFormat[ProjectUpdateRequest] = Json.format[ProjectUpdateRequest]
}
