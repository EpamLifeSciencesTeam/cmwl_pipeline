package cromwell.pipeline.datastorage.dto

import cromwell.pipeline.model.wrapper.ProjectId
import play.api.libs.json.{ Json, OFormat }

final case class ProjectResponse(projectId: ProjectId, name: String, active: Boolean)

object ProjectResponse {
  implicit lazy val findProjectResponse: OFormat[ProjectResponse] = Json.format[ProjectResponse]
}
