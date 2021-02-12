package cromwell.pipeline.datastorage.dto

import java.nio.file.Path

import ProjectFile.pathFormat
import org.mongodb.scala.bson.collection.immutable.Document
import play.api.libs.json.{ Json, OFormat }

case class ProjectFileConfiguration(path: Path, inputs: List[FileParameter])

object ProjectFileConfiguration {
  implicit val projectFileConfigurationFormat: OFormat[ProjectFileConfiguration] = Json.format
}

case class ProjectConfiguration(projectId: ProjectId, active: Boolean, projectFileConfigurations: List[ProjectFileConfiguration])

object ProjectConfiguration {
  implicit val projectConfigurationFormat: OFormat[ProjectConfiguration] = Json.format

  def toDocument(projectConfiguration: ProjectConfiguration): Document =
    Document(Json.stringify(Json.toJson(projectConfiguration)))

  def fromDocument(document: Document): ProjectConfiguration =
    Json.parse(document.toJson()).as[ProjectConfiguration]
}
