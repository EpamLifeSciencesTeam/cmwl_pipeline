package cromwell.pipeline.datastorage.dto.project

import cromwell.pipeline.datastorage.dto.ProjectId
import play.api.libs.json.{ Json, OFormat }

final case class ProjectDeleteRequest(projectId: ProjectId)

object ProjectDeleteRequest {
  implicit lazy val projectDeleteFormat: OFormat[ProjectDeleteRequest] = Json.format[ProjectDeleteRequest]
}
