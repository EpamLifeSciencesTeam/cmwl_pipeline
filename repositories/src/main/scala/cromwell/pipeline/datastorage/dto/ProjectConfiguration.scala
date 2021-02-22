package cromwell.pipeline.datastorage.dto

import java.nio.file.Path

import ProjectFile.pathFormat
import play.api.libs.json.{ Json, OFormat }

case class ProjectFileConfiguration(path: Path, inputs: List[FileParameter])

object ProjectFileConfiguration {
  implicit val projectFileConfigurationFormat: OFormat[ProjectFileConfiguration] = Json.format
}

case class ProjectConfiguration(
  projectId: ProjectId,
  active: Boolean,
  projectFileConfigurations: List[ProjectFileConfiguration]
)

object ProjectConfiguration {
  implicit val projectConfigurationFormat: OFormat[ProjectConfiguration] = Json.format
}
