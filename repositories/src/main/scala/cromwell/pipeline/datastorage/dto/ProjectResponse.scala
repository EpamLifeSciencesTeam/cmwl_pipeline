package cromwell.pipeline.datastorage.dto

import play.api.libs.json.{Json, OFormat}

final case class ProjectResponse(
  projectId: ProjectId,
  name: String,
  active: Boolean
)

object ProjectResponse {
  implicit val findProjectResponse: OFormat[ProjectResponse] = Json.format[ProjectResponse]
}
