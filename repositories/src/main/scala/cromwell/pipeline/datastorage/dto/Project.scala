package cromwell.pipeline.datastorage.dto

import java.nio.file.Path

import play.api.libs.json.{ Format, Json, OFormat }
import slick.lifted.MappedTo
import play.api.libs.functional.syntax._

final case class Project(
  projectId: ProjectId,
  ownerId: UserId,
  name: String,
  repository: String,
  active: Boolean
)

object Project{
  implicit lazy val projectFormat: OFormat[Project] = Json.format[Project]
}

final case class ProjectId(value: String) extends MappedTo[String]

object ProjectId{
  implicit lazy val projectIdFormat: OFormat[ProjectId] = Json.format[ProjectId]
}

final case class ProjectAdditionRequest(name: String)

object ProjectAdditionRequest {
  implicit lazy val projectAdditionFormat: OFormat[ProjectAdditionRequest] = Json.format[ProjectAdditionRequest]
}

final case class Version(value: String) extends AnyVal

final case class ProjectFile(path: Path, content: String)
