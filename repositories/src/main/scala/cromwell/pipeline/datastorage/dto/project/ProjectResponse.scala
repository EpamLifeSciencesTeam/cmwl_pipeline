package cromwell.pipeline.datastorage.dto.project

import cromwell.pipeline.datastorage.dto.ProjectId
import play.api.libs.json.{Json, OFormat}

final case class ProjectResponse(projectId: ProjectId, name: String, active: Boolean)

object ProjectResponse {
  implicit lazy val findProjectResponse: OFormat[ProjectResponse] = Json.format[ProjectResponse]
}
