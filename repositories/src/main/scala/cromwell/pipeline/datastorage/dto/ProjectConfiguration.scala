package cromwell.pipeline.datastorage.dto

import java.nio.file.Path

import org.mongodb.scala.bson.collection.immutable.Document
import play.api.libs.json.{ Json }
import cromwell.pipeline.datastorage.formatters.ProjectFormatters._

case class ProjectFileConfiguration(path: Path, inputs: List[FileParameter])

case class ProjectConfiguration(projectId: ProjectId, projectFileConfigurations: List[ProjectFileConfiguration])

object ProjectConfiguration {

  def toDocument(projectConfiguration: ProjectConfiguration): Document =
    Document(Json.stringify(Json.toJson(projectConfiguration)))

  def fromDocument(document: Document): ProjectConfiguration =
    Json.parse(document.toJson()).as[ProjectConfiguration]
}
