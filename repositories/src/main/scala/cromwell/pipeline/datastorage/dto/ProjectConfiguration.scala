package cromwell.pipeline.datastorage.dto

import java.nio.file.Path

import ProjectFile.pathFormat
import cromwell.pipeline.datastorage.dto.project.configuration.ProjectConfigurationEntity
import org.mongodb.scala.bson.collection.immutable.Document
import play.api.libs.json.{ Json, OFormat }

case class ProjectFileConfiguration(path: Path, inputs: List[FileParameter])

object ProjectFileConfiguration {
  implicit val projectFileConfigurationFormat: OFormat[ProjectFileConfiguration] = Json.format
}

final case class ProjectConfiguration(
  projectId: ProjectId,
  projectFileConfigurations: List[ProjectFileConfiguration],
  isActive: Boolean = true
)

object ProjectConfiguration {
  implicit val projectConfigurationFormat: OFormat[ProjectConfiguration] = Json.format

  def apply(configuration: ProjectConfigurationEntity): ProjectConfiguration =
    ProjectConfiguration(configuration.projectId, configuration.projectFileConfigurations)

  def toDocument(projectConfiguration: ProjectConfiguration): Document =
    Document(Json.stringify(Json.toJson(projectConfiguration)))

  def fromDocument(document: Document): ProjectConfiguration =
    Json.parse(document.toJson()).as[ProjectConfiguration]
}
