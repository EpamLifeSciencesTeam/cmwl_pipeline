package cromwell.pipeline.datastorage.dto.project.configuration

import cromwell.pipeline.datastorage.dto.{ ProjectFileConfiguration, ProjectId }
import play.api.libs.json.{ Json, OFormat }

case class ProjectConfigurationEntity(projectId: ProjectId, projectFileConfigurations: List[ProjectFileConfiguration])

object ProjectConfigurationEntity {
  implicit val projectConfigurationRequestFormat: OFormat[ProjectConfigurationEntity] =
    Json.format[ProjectConfigurationEntity]
}
